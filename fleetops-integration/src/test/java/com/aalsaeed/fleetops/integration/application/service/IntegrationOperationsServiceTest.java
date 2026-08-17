package com.aalsaeed.fleetops.integration.application.service;

import com.aalsaeed.fleetops.integration.application.port.in.IntegrationOperationsSnapshot;
import com.aalsaeed.fleetops.integration.application.port.out.DeadLetterQueueMetricsPort;
import com.aalsaeed.fleetops.integration.application.port.out.IntegrationOperationsStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntegrationOperationsServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-18T00:00:00Z");

    @Test
    void buildsSnapshotUsingConfiguredStaleThresholds() {
        CapturingStore store = new CapturingStore();
        DeadLetterQueueMetricsPort dlq = () ->
                new IntegrationOperationsSnapshot.DeadLetterQueueSnapshot(true, 4);

        IntegrationOperationsService service = new IntegrationOperationsService(
                store,
                dlq,
                Duration.ofSeconds(60),
                Duration.ofSeconds(90),
                20,
                Clock.fixed(NOW, ZoneOffset.UTC));

        IntegrationOperationsSnapshot snapshot = service.getSnapshot();

        assertEquals(NOW, snapshot.capturedAt());
        assertEquals(NOW.minusSeconds(60), store.outboxStaleBefore);
        assertEquals(NOW.minusSeconds(90), store.inboxStaleBefore);
        assertEquals(20, store.failureLimit);
        assertEquals(4, snapshot.deadLetterQueue().messageCount());
        assertEquals(2, snapshot.outbox().failed());
        assertEquals(3, snapshot.inbox().failed());
    }

    @Test
    void rejectsInvalidFailureLimit() {
        assertThrows(IllegalArgumentException.class, () -> new IntegrationOperationsService(
                new CapturingStore(),
                () -> new IntegrationOperationsSnapshot.DeadLetterQueueSnapshot(true, 0),
                Duration.ofSeconds(60),
                Duration.ofSeconds(60),
                0,
                Clock.systemUTC()));
    }

    private static final class CapturingStore implements IntegrationOperationsStore {
        private Instant outboxStaleBefore;
        private Instant inboxStaleBefore;
        private int failureLimit;

        @Override
        public IntegrationOperationsSnapshot.OutboxSnapshot loadOutbox(Instant staleBefore, int failureLimit) {
            this.outboxStaleBefore = staleBefore;
            this.failureLimit = failureLimit;
            return new IntegrationOperationsSnapshot.OutboxSnapshot(1, 1, 5, 2, 1, List.of());
        }

        @Override
        public IntegrationOperationsSnapshot.InboxSnapshot loadInbox(Instant staleBefore, int failureLimit) {
            this.inboxStaleBefore = staleBefore;
            this.failureLimit = failureLimit;
            return new IntegrationOperationsSnapshot.InboxSnapshot(1, 1, 7, 3, 1, List.of());
        }
    }
}
