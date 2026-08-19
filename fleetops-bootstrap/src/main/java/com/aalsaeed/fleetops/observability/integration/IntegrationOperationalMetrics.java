package com.aalsaeed.fleetops.observability.integration;

import com.aalsaeed.fleetops.integration.application.port.in.GetIntegrationOperationsUseCase;
import com.aalsaeed.fleetops.integration.application.port.in.IntegrationOperationsSnapshot;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.ToDoubleFunction;

/**
 * Publishes bounded-cardinality integration pipeline gauges from the existing
 * operational reconciliation use case.
 *
 * <p>The snapshot is cached for a short interval so one Prometheus scrape does
 * not execute the same PostgreSQL/RabbitMQ reconciliation query once per gauge.</p>
 */
@Component
public final class IntegrationOperationalMetrics implements MeterBinder {

    private static final Logger log = LoggerFactory.getLogger(IntegrationOperationalMetrics.class);
    private static final String OUTBOX_MESSAGES = "fleetops.integration.outbox.messages";
    private static final String INBOX_MESSAGES = "fleetops.integration.inbox.messages";
    private static final String DLQ_MESSAGES = "fleetops.integration.dlq.messages";
    private static final String DLQ_AVAILABLE = "fleetops.integration.dlq.available";
    private static final String SNAPSHOT_AVAILABLE = "fleetops.integration.snapshot.available";

    private final SnapshotCache snapshotCache;

    public IntegrationOperationalMetrics(
            GetIntegrationOperationsUseCase operationsUseCase,
            @Value("${fleetops.observability.integration.snapshot-ttl-ms:5000}") long snapshotTtlMs) {
        this(operationsUseCase, positiveDuration(snapshotTtlMs), System::nanoTime);
    }

    IntegrationOperationalMetrics(
            GetIntegrationOperationsUseCase operationsUseCase,
            Duration snapshotTtl,
            LongSupplier ticker) {
        this.snapshotCache = new SnapshotCache(operationsUseCase, snapshotTtl, ticker);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Objects.requireNonNull(registry, "Meter registry cannot be null");

        registerOutboxGauge(registry, "pending", snapshot -> snapshot.outbox().pending());
        registerOutboxGauge(registry, "publishing", snapshot -> snapshot.outbox().publishing());
        registerOutboxGauge(registry, "published", snapshot -> snapshot.outbox().published());
        registerOutboxGauge(registry, "failed", snapshot -> snapshot.outbox().failed());
        registerOutboxGauge(registry, "stale_publishing", snapshot -> snapshot.outbox().stalePublishing());

        registerInboxGauge(registry, "pending", snapshot -> snapshot.inbox().pending());
        registerInboxGauge(registry, "processing", snapshot -> snapshot.inbox().processing());
        registerInboxGauge(registry, "processed", snapshot -> snapshot.inbox().processed());
        registerInboxGauge(registry, "failed", snapshot -> snapshot.inbox().failed());
        registerInboxGauge(registry, "stale_processing", snapshot -> snapshot.inbox().staleProcessing());

        Gauge.builder(DLQ_MESSAGES, snapshotCache, cache -> cache.snapshot().deadLetterQueue().messageCount())
                .description("ERP shipment dead-letter queue depth")
                .register(registry);

        Gauge.builder(DLQ_AVAILABLE, snapshotCache,
                        cache -> cache.snapshot().deadLetterQueue().available() ? 1.0 : 0.0)
                .description("Whether ERP shipment dead-letter queue depth is currently available")
                .register(registry);

        Gauge.builder(SNAPSHOT_AVAILABLE, snapshotCache, SnapshotCache::available)
                .description("Whether the integration operational snapshot refresh succeeded")
                .register(registry);
    }

    private void registerOutboxGauge(
            MeterRegistry registry,
            String state,
            ToDoubleFunction<IntegrationOperationsSnapshot> valueFunction) {
        Gauge.builder(OUTBOX_MESSAGES, snapshotCache, cache -> valueFunction.applyAsDouble(cache.snapshot()))
                .description("Transactional outbox message count by bounded operational state")
                .tag("state", state)
                .register(registry);
    }

    private void registerInboxGauge(
            MeterRegistry registry,
            String state,
            ToDoubleFunction<IntegrationOperationsSnapshot> valueFunction) {
        Gauge.builder(INBOX_MESSAGES, snapshotCache, cache -> valueFunction.applyAsDouble(cache.snapshot()))
                .description("Integration inbox message count by bounded operational state")
                .tag("state", state)
                .register(registry);
    }

    private static Duration positiveDuration(long millis) {
        if (millis < 1) {
            throw new IllegalArgumentException("Integration metrics snapshot TTL must be at least 1 ms");
        }
        return Duration.ofMillis(millis);
    }

    static final class SnapshotCache {

        private static final IntegrationOperationsSnapshot EMPTY_SNAPSHOT = new IntegrationOperationsSnapshot(
                Instant.EPOCH,
                new IntegrationOperationsSnapshot.OutboxSnapshot(0, 0, 0, 0, 0, List.of()),
                new IntegrationOperationsSnapshot.InboxSnapshot(0, 0, 0, 0, 0, List.of()),
                new IntegrationOperationsSnapshot.DeadLetterQueueSnapshot(false, 0));

        private final GetIntegrationOperationsUseCase operationsUseCase;
        private final long ttlNanos;
        private final LongSupplier ticker;
        private volatile CacheState state = new CacheState(EMPTY_SNAPSHOT, false, Long.MIN_VALUE);

        SnapshotCache(
                GetIntegrationOperationsUseCase operationsUseCase,
                Duration snapshotTtl,
                LongSupplier ticker) {
            this.operationsUseCase = Objects.requireNonNull(operationsUseCase, "Operations use case cannot be null");
            Objects.requireNonNull(snapshotTtl, "Snapshot TTL cannot be null");
            if (snapshotTtl.isZero() || snapshotTtl.isNegative()) {
                throw new IllegalArgumentException("Snapshot TTL must be positive");
            }
            this.ttlNanos = snapshotTtl.toNanos();
            this.ticker = Objects.requireNonNull(ticker, "Ticker cannot be null");
        }

        IntegrationOperationsSnapshot snapshot() {
            return currentState().snapshot();
        }

        double available() {
            return currentState().available() ? 1.0 : 0.0;
        }

        private CacheState currentState() {
            long now = ticker.getAsLong();
            CacheState observed = state;
            if (now < observed.expiresAtNanos()) {
                return observed;
            }

            synchronized (this) {
                observed = state;
                now = ticker.getAsLong();
                if (now < observed.expiresAtNanos()) {
                    return observed;
                }

                long expiresAt = now + ttlNanos;
                try {
                    IntegrationOperationsSnapshot refreshed = Objects.requireNonNull(
                            operationsUseCase.getSnapshot(),
                            "Integration operations snapshot cannot be null");
                    state = new CacheState(refreshed, true, expiresAt);
                }
                catch (RuntimeException exception) {
                    log.warn("Could not refresh FleetOps integration operational metrics snapshot", exception);
                    state = new CacheState(observed.snapshot(), false, expiresAt);
                }
                return state;
            }
        }

        private record CacheState(
                IntegrationOperationsSnapshot snapshot,
                boolean available,
                long expiresAtNanos) {
        }
    }
}
