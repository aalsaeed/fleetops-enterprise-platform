package com.aalsaeed.fleetops.observability.integration;

import com.aalsaeed.fleetops.integration.application.port.in.GetIntegrationOperationsUseCase;
import com.aalsaeed.fleetops.integration.application.port.in.IntegrationOperationsSnapshot;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationOperationalMetricsTest {

    @Test
    void exposesBoundedIntegrationPipelineGaugesFromOneCachedSnapshot() {
        AtomicInteger calls = new AtomicInteger();
        AtomicLong ticker = new AtomicLong();
        GetIntegrationOperationsUseCase useCase = () -> {
            calls.incrementAndGet();
            return snapshot(3, 2, 20, 4, 1, 5, 6, 30, 7, 2, true, 8);
        };

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new IntegrationOperationalMetrics(useCase, Duration.ofSeconds(5), ticker::get).bindTo(registry);

        assertThat(gauge(registry, "fleetops.integration.outbox.messages", "state", "pending")).isEqualTo(3.0);
        assertThat(gauge(registry, "fleetops.integration.outbox.messages", "state", "publishing")).isEqualTo(2.0);
        assertThat(gauge(registry, "fleetops.integration.outbox.messages", "state", "published")).isEqualTo(20.0);
        assertThat(gauge(registry, "fleetops.integration.outbox.messages", "state", "failed")).isEqualTo(4.0);
        assertThat(gauge(registry, "fleetops.integration.outbox.messages", "state", "stale_publishing")).isEqualTo(1.0);

        assertThat(gauge(registry, "fleetops.integration.inbox.messages", "state", "pending")).isEqualTo(5.0);
        assertThat(gauge(registry, "fleetops.integration.inbox.messages", "state", "processing")).isEqualTo(6.0);
        assertThat(gauge(registry, "fleetops.integration.inbox.messages", "state", "processed")).isEqualTo(30.0);
        assertThat(gauge(registry, "fleetops.integration.inbox.messages", "state", "failed")).isEqualTo(7.0);
        assertThat(gauge(registry, "fleetops.integration.inbox.messages", "state", "stale_processing")).isEqualTo(2.0);

        assertThat(registry.get("fleetops.integration.dlq.messages").gauge().value()).isEqualTo(8.0);
        assertThat(registry.get("fleetops.integration.dlq.available").gauge().value()).isEqualTo(1.0);
        assertThat(registry.get("fleetops.integration.snapshot.available").gauge().value()).isEqualTo(1.0);
        assertThat(calls).hasValue(1);
    }

    @Test
    void refreshesOnlyAfterTtlAndPreservesLastSnapshotWhenRefreshFails() {
        AtomicLong ticker = new AtomicLong();
        AtomicBoolean fail = new AtomicBoolean();
        AtomicReference<IntegrationOperationsSnapshot> current = new AtomicReference<>(
                snapshot(11, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, 0));
        AtomicInteger calls = new AtomicInteger();

        GetIntegrationOperationsUseCase useCase = () -> {
            calls.incrementAndGet();
            if (fail.get()) {
                throw new IllegalStateException("operations store unavailable");
            }
            return current.get();
        };

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new IntegrationOperationalMetrics(useCase, Duration.ofSeconds(5), ticker::get).bindTo(registry);

        assertThat(gauge(registry, "fleetops.integration.outbox.messages", "state", "pending")).isEqualTo(11.0);
        current.set(snapshot(22, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, 0));
        assertThat(gauge(registry, "fleetops.integration.outbox.messages", "state", "pending")).isEqualTo(11.0);
        assertThat(calls).hasValue(1);

        ticker.set(Duration.ofSeconds(5).toNanos());
        assertThat(gauge(registry, "fleetops.integration.outbox.messages", "state", "pending")).isEqualTo(22.0);
        assertThat(calls).hasValue(2);

        fail.set(true);
        ticker.set(Duration.ofSeconds(10).toNanos());
        assertThat(gauge(registry, "fleetops.integration.outbox.messages", "state", "pending")).isEqualTo(22.0);
        assertThat(registry.get("fleetops.integration.snapshot.available").gauge().value()).isEqualTo(0.0);
        assertThat(calls).hasValue(3);
    }

    @Test
    void usesOnlyFixedLowCardinalityStateTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        GetIntegrationOperationsUseCase useCase = () -> snapshot(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, true, 1);

        new IntegrationOperationalMetrics(useCase, Duration.ofSeconds(5), () -> 0L).bindTo(registry);

        Set<String> allowedStates = Set.of(
                "pending", "publishing", "published", "failed", "stale_publishing",
                "processing", "processed", "stale_processing");

        for (Meter meter : registry.getMeters()) {
            meter.getId().getTags().forEach(tag -> {
                assertThat(tag.getKey()).isEqualTo("state");
                assertThat(tag.getValue()).isIn(allowedStates);
            });
        }
    }

    private static double gauge(
            SimpleMeterRegistry registry,
            String name,
            String tagKey,
            String tagValue) {
        return registry.get(name).tag(tagKey, tagValue).gauge().value();
    }

    private static IntegrationOperationsSnapshot snapshot(
            long outboxPending,
            long outboxPublishing,
            long outboxPublished,
            long outboxFailed,
            long outboxStale,
            long inboxPending,
            long inboxProcessing,
            long inboxProcessed,
            long inboxFailed,
            long inboxStale,
            boolean dlqAvailable,
            long dlqMessages) {
        return new IntegrationOperationsSnapshot(
                Instant.parse("2026-08-19T00:00:00Z"),
                new IntegrationOperationsSnapshot.OutboxSnapshot(
                        outboxPending,
                        outboxPublishing,
                        outboxPublished,
                        outboxFailed,
                        outboxStale,
                        List.of()),
                new IntegrationOperationsSnapshot.InboxSnapshot(
                        inboxPending,
                        inboxProcessing,
                        inboxProcessed,
                        inboxFailed,
                        inboxStale,
                        List.of()),
                new IntegrationOperationsSnapshot.DeadLetterQueueSnapshot(dlqAvailable, dlqMessages));
    }
}
