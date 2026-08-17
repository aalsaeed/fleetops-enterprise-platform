package com.aalsaeed.fleetops.integration.application.service;

import com.aalsaeed.fleetops.integration.application.port.out.ClaimedInboxMessage;
import com.aalsaeed.fleetops.integration.application.port.out.InboxProcessingStore;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentOperation;
import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InboxProcessingServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-17T21:10:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void handsOffClaimedMessageAndMarksItProcessed() {
        ErpShipmentMessage message = shipment("INBOX-PROCESS-1", "SHIP-PROCESS-1");
        RecordingStore store = new RecordingStore(List.of(
                new ClaimedInboxMessage(message.id(), "payload", 1)));
        List<ErpShipmentMessage> handedOff = new ArrayList<>();
        InboxProcessingService service = new InboxProcessingService(
                store,
                ignored -> message,
                handedOff::add,
                CLOCK);

        assertEquals(1, service.processPending(10));
        assertEquals(List.of(message), handedOff);
        assertEquals(List.of(message.id()), store.processedIds);
        assertTrue(store.failedIds.isEmpty());
        assertEquals(NOW, store.processedAt);
    }

    @Test
    void marksInboxMessageFailedWhenTripHandoffFails() {
        ErpShipmentMessage message = shipment("INBOX-PROCESS-2", "SHIP-PROCESS-2");
        RecordingStore store = new RecordingStore(List.of(
                new ClaimedInboxMessage(message.id(), "payload", 1)));
        InboxProcessingService service = new InboxProcessingService(
                store,
                ignored -> message,
                ignored -> { throw new IllegalStateException("trip rejected"); },
                CLOCK);

        assertEquals(0, service.processPending(10));
        assertEquals(List.of(message.id()), store.failedIds);
        assertEquals("IllegalStateException: trip rejected", store.lastError);
    }

    @Test
    void recoversStaleProcessingAndRequeuesTerminalFailure() {
        ErpShipmentMessage message = shipment("INBOX-PROCESS-3", "SHIP-PROCESS-3");
        RecordingStore store = new RecordingStore(List.of());
        store.recovered = 2;
        store.requeueResult = true;
        InboxProcessingService service = new InboxProcessingService(
                store,
                ignored -> message,
                ignored -> { },
                CLOCK);

        assertEquals(2, service.recoverStale(Duration.ofSeconds(60)));
        assertEquals(NOW.minusSeconds(60), store.staleBefore);
        assertTrue(service.requeue(message.id()));
        assertEquals(message.id(), store.requeuedId);
        assertFalse(service.requeue(IntegrationMessageId.newId()));
    }

    private static ErpShipmentMessage shipment(String sourceMessageId, String shipmentReference) {
        return ErpShipmentMessage.create(
                "ERP-DEMO",
                sourceMessageId,
                shipmentReference,
                ErpShipmentOperation.UPSERT,
                NOW.minusSeconds(30),
                "CORR-" + sourceMessageId);
    }

    private static final class RecordingStore implements InboxProcessingStore {
        private final List<ClaimedInboxMessage> claimed;
        private final List<IntegrationMessageId> processedIds = new ArrayList<>();
        private final List<IntegrationMessageId> failedIds = new ArrayList<>();
        private Instant processedAt;
        private String lastError;
        private int recovered;
        private Instant staleBefore;
        private boolean requeueResult;
        private IntegrationMessageId requeuedId;

        private RecordingStore(List<ClaimedInboxMessage> claimed) {
            this.claimed = claimed;
        }

        @Override
        public List<ClaimedInboxMessage> claimPending(int limit, Instant claimedAt) {
            return claimed;
        }

        @Override
        public void markProcessed(IntegrationMessageId id, Instant processedAt) {
            processedIds.add(id);
            this.processedAt = processedAt;
        }

        @Override
        public void markFailed(IntegrationMessageId id, String error) {
            failedIds.add(id);
            lastError = error;
        }

        @Override
        public int recoverStaleProcessing(Instant staleBefore) {
            this.staleBefore = staleBefore;
            return recovered;
        }

        @Override
        public boolean requeueFailed(IntegrationMessageId id) {
            requeuedId = id;
            boolean result = requeueResult;
            requeueResult = false;
            return result;
        }
    }
}
