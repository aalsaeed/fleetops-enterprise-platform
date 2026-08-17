package com.aalsaeed.fleetops.integration.application.service;

import com.aalsaeed.fleetops.integration.application.port.out.OutboxMessagePublisher;
import com.aalsaeed.fleetops.integration.application.port.out.OutboxPublicationStore;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentOperation;
import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;
import com.aalsaeed.fleetops.integration.domain.outbox.IntegrationOutboxMessage;
import com.aalsaeed.fleetops.integration.domain.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutboxPublicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-17T19:50:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final OutboxRetryPolicy RETRY_POLICY = new OutboxRetryPolicy(
            3,
            Duration.ofSeconds(1),
            Duration.ofSeconds(8),
            Duration.ofSeconds(30));

    @Test
    void marksMessagePublishedOnlyAfterPublisherSucceeds() {
        IntegrationOutboxMessage message = claimedMessage("MSG-1", 1);
        RecordingStore store = new RecordingStore(List.of(message));
        OutboxPublicationService service = service(store, ignored -> { });

        int published = service.publishPending(10);

        assertEquals(1, published);
        assertEquals(List.of(message.id()), store.publishedIds);
        assertEquals(List.of(), store.retryIds);
        assertEquals(List.of(), store.failedIds);
        assertEquals(NOW.minusSeconds(30), store.lastStaleBefore);
    }

    @Test
    void schedulesRetryWhenPublicationFailsBeforeMaxAttempts() {
        IntegrationOutboxMessage message = claimedMessage("MSG-2", 1);
        RecordingStore store = new RecordingStore(List.of(message));
        OutboxMessagePublisher failingPublisher = ignored -> {
            throw new IllegalStateException("broker nack");
        };

        int published = service(store, failingPublisher).publishPending(10);

        assertEquals(0, published);
        assertEquals(List.of(message.id()), store.retryIds);
        assertEquals(List.of(), store.failedIds);
        assertEquals(NOW.plusSeconds(1), store.nextAttemptAt);
        assertEquals("IllegalStateException: broker nack", store.lastError);
    }

    @Test
    void marksMessageFailedWhenMaximumAttemptsAreExhausted() {
        IntegrationOutboxMessage message = claimedMessage("MSG-3", 3);
        RecordingStore store = new RecordingStore(List.of(message));
        OutboxMessagePublisher failingPublisher = ignored -> {
            throw new IllegalStateException("confirm timeout");
        };

        int published = service(store, failingPublisher).publishPending(10);

        assertEquals(0, published);
        assertEquals(List.of(), store.retryIds);
        assertEquals(List.of(message.id()), store.failedIds);
        assertEquals("IllegalStateException: confirm timeout", store.lastError);
    }

    @Test
    void retryPolicyUsesBoundedExponentialBackoff() {
        assertEquals(Duration.ofSeconds(1), RETRY_POLICY.delayAfterAttempt(1));
        assertEquals(Duration.ofSeconds(2), RETRY_POLICY.delayAfterAttempt(2));
        assertEquals(Duration.ofSeconds(4), RETRY_POLICY.delayAfterAttempt(3));
        assertEquals(Duration.ofSeconds(8), RETRY_POLICY.delayAfterAttempt(4));
        assertEquals(Duration.ofSeconds(8), RETRY_POLICY.delayAfterAttempt(20));
    }

    @Test
    void rejectsInvalidBatchSize() {
        OutboxPublicationService service = service(new RecordingStore(List.of()), ignored -> { });

        assertThrows(IllegalArgumentException.class, () -> service.publishPending(0));
    }

    private static OutboxPublicationService service(
            RecordingStore store,
            OutboxMessagePublisher publisher) {
        return new OutboxPublicationService(store, publisher, RETRY_POLICY, CLOCK);
    }

    private static IntegrationOutboxMessage claimedMessage(String sourceMessageId, int attempts) {
        ErpShipmentMessage erpMessage = ErpShipmentMessage.create(
                "ERP-DEMO",
                sourceMessageId,
                "SHIP-" + sourceMessageId,
                ErpShipmentOperation.UPSERT,
                NOW,
                "CORR-" + sourceMessageId);
        IntegrationOutboxMessage pending = IntegrationOutboxMessage.pending(
                erpMessage,
                "{\"shipmentReference\":\"SHIP\"}",
                NOW);
        return IntegrationOutboxMessage.restore(
                pending.id(),
                pending.idempotencyKey(),
                pending.eventType(),
                pending.aggregateType(),
                pending.aggregateId(),
                pending.payload(),
                OutboxStatus.PUBLISHING,
                attempts,
                pending.createdAt(),
                null,
                null);
    }

    private static final class RecordingStore implements OutboxPublicationStore {
        private final List<IntegrationOutboxMessage> claimed;
        private final List<IntegrationMessageId> publishedIds = new ArrayList<>();
        private final List<IntegrationMessageId> retryIds = new ArrayList<>();
        private final List<IntegrationMessageId> failedIds = new ArrayList<>();
        private String lastError;
        private Instant nextAttemptAt;
        private Instant lastStaleBefore;

        private RecordingStore(List<IntegrationOutboxMessage> claimed) {
            this.claimed = claimed;
        }

        @Override
        public List<IntegrationOutboxMessage> claimPending(int limit, Instant claimedAt) {
            return claimed;
        }

        @Override
        public void markPublished(IntegrationMessageId id, Instant publishedAt) {
            publishedIds.add(id);
        }

        @Override
        public void scheduleRetry(IntegrationMessageId id, String error, Instant nextAttemptAt) {
            retryIds.add(id);
            lastError = error;
            this.nextAttemptAt = nextAttemptAt;
        }

        @Override
        public void markFailed(IntegrationMessageId id, String error) {
            failedIds.add(id);
            lastError = error;
        }

        @Override
        public int recoverStalePublishing(Instant staleBefore, Instant retryAt) {
            lastStaleBefore = staleBefore;
            return 0;
        }
    }
}
