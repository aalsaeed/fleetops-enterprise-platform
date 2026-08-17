package com.aalsaeed.fleetops.integration.application.service;

import com.aalsaeed.fleetops.integration.application.port.out.OutboxMessagePublisher;
import com.aalsaeed.fleetops.integration.application.port.out.OutboxPublicationStore;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentOperation;
import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;
import com.aalsaeed.fleetops.integration.domain.outbox.IntegrationOutboxMessage;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutboxPublicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-17T19:50:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void marksMessagePublishedOnlyAfterPublisherSucceeds() {
        IntegrationOutboxMessage message = pendingMessage("MSG-1");
        RecordingStore store = new RecordingStore(List.of(message));
        OutboxPublicationService service = new OutboxPublicationService(store, ignored -> { }, CLOCK);

        int published = service.publishPending(10);

        assertEquals(1, published);
        assertEquals(List.of(message.id()), store.publishedIds);
        assertEquals(List.of(), store.failedIds);
    }

    @Test
    void marksMessageFailedWhenPublisherThrows() {
        IntegrationOutboxMessage message = pendingMessage("MSG-2");
        RecordingStore store = new RecordingStore(List.of(message));
        OutboxMessagePublisher failingPublisher = ignored -> {
            throw new IllegalStateException("broker nack");
        };
        OutboxPublicationService service = new OutboxPublicationService(store, failingPublisher, CLOCK);

        int published = service.publishPending(10);

        assertEquals(0, published);
        assertEquals(List.of(), store.publishedIds);
        assertEquals(List.of(message.id()), store.failedIds);
        assertEquals("IllegalStateException: broker nack", store.lastError);
    }

    @Test
    void rejectsInvalidBatchSize() {
        OutboxPublicationService service = new OutboxPublicationService(
                new RecordingStore(List.of()),
                ignored -> { },
                CLOCK);

        assertThrows(IllegalArgumentException.class, () -> service.publishPending(0));
    }

    private static IntegrationOutboxMessage pendingMessage(String sourceMessageId) {
        ErpShipmentMessage message = ErpShipmentMessage.create(
                "ERP-DEMO",
                sourceMessageId,
                "SHIP-" + sourceMessageId,
                ErpShipmentOperation.UPSERT,
                NOW,
                "CORR-" + sourceMessageId);
        return IntegrationOutboxMessage.pending(message, "{\"shipmentReference\":\"SHIP\"}", NOW);
    }

    private static final class RecordingStore implements OutboxPublicationStore {
        private final List<IntegrationOutboxMessage> claimed;
        private final List<IntegrationMessageId> publishedIds = new ArrayList<>();
        private final List<IntegrationMessageId> failedIds = new ArrayList<>();
        private String lastError;

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
        public void markFailed(IntegrationMessageId id, String error) {
            failedIds.add(id);
            lastError = error;
        }
    }
}
