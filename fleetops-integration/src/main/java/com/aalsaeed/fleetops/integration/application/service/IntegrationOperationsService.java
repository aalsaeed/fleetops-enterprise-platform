package com.aalsaeed.fleetops.integration.application.service;

import com.aalsaeed.fleetops.integration.application.port.in.GetIntegrationOperationsUseCase;
import com.aalsaeed.fleetops.integration.application.port.in.IntegrationOperationsSnapshot;
import com.aalsaeed.fleetops.integration.application.port.out.DeadLetterQueueMetricsPort;
import com.aalsaeed.fleetops.integration.application.port.out.IntegrationOperationsStore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class IntegrationOperationsService implements GetIntegrationOperationsUseCase {

    private final IntegrationOperationsStore operationsStore;
    private final DeadLetterQueueMetricsPort deadLetterQueueMetricsPort;
    private final Duration outboxStaleTimeout;
    private final Duration inboxStaleTimeout;
    private final int failureLimit;
    private final Clock clock;

    public IntegrationOperationsService(
            IntegrationOperationsStore operationsStore,
            DeadLetterQueueMetricsPort deadLetterQueueMetricsPort,
            Duration outboxStaleTimeout,
            Duration inboxStaleTimeout,
            int failureLimit,
            Clock clock) {
        this.operationsStore = Objects.requireNonNull(operationsStore, "Operations store cannot be null");
        this.deadLetterQueueMetricsPort = Objects.requireNonNull(
                deadLetterQueueMetricsPort, "Dead-letter queue metrics port cannot be null");
        this.outboxStaleTimeout = positiveDuration(outboxStaleTimeout, "Outbox stale timeout");
        this.inboxStaleTimeout = positiveDuration(inboxStaleTimeout, "Inbox stale timeout");
        if (failureLimit < 1 || failureLimit > 100) {
            throw new IllegalArgumentException("Failure limit must be between 1 and 100");
        }
        this.failureLimit = failureLimit;
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    @Override
    public IntegrationOperationsSnapshot getSnapshot() {
        Instant now = clock.instant();
        return new IntegrationOperationsSnapshot(
                now,
                operationsStore.loadOutbox(now.minus(outboxStaleTimeout), failureLimit),
                operationsStore.loadInbox(now.minus(inboxStaleTimeout), failureLimit),
                deadLetterQueueMetricsPort.getSnapshot());
    }

    private static Duration positiveDuration(Duration value, String name) {
        Objects.requireNonNull(value, name + " cannot be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
