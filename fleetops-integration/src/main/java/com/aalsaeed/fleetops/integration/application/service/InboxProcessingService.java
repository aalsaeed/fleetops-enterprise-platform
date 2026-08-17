package com.aalsaeed.fleetops.integration.application.service;

import com.aalsaeed.fleetops.integration.application.port.in.ProcessPendingInboxUseCase;
import com.aalsaeed.fleetops.integration.application.port.in.RecoverStaleInboxUseCase;
import com.aalsaeed.fleetops.integration.application.port.in.RequeueFailedInboxUseCase;
import com.aalsaeed.fleetops.integration.application.port.out.ClaimedInboxMessage;
import com.aalsaeed.fleetops.integration.application.port.out.ErpShipmentPayloadDeserializer;
import com.aalsaeed.fleetops.integration.application.port.out.ErpShipmentTripHandoffPort;
import com.aalsaeed.fleetops.integration.application.port.out.InboxProcessingStore;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class InboxProcessingService implements
        ProcessPendingInboxUseCase,
        RecoverStaleInboxUseCase,
        RequeueFailedInboxUseCase {

    private final InboxProcessingStore processingStore;
    private final ErpShipmentPayloadDeserializer payloadDeserializer;
    private final ErpShipmentTripHandoffPort tripHandoffPort;
    private final Clock clock;

    public InboxProcessingService(
            InboxProcessingStore processingStore,
            ErpShipmentPayloadDeserializer payloadDeserializer,
            ErpShipmentTripHandoffPort tripHandoffPort,
            Clock clock) {
        this.processingStore = Objects.requireNonNull(processingStore, "Inbox processing store cannot be null");
        this.payloadDeserializer = Objects.requireNonNull(payloadDeserializer, "Payload deserializer cannot be null");
        this.tripHandoffPort = Objects.requireNonNull(tripHandoffPort, "Trip handoff port cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    @Override
    public int processPending(int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be at least 1");
        }

        List<ClaimedInboxMessage> claimed = processingStore.claimPending(batchSize, clock.instant());
        int processed = 0;

        for (ClaimedInboxMessage inboxMessage : claimed) {
            try {
                ErpShipmentMessage message = payloadDeserializer.deserialize(inboxMessage.payload());
                tripHandoffPort.handoff(message);
                processingStore.markProcessed(inboxMessage.messageId(), clock.instant());
                processed++;
            } catch (RuntimeException ex) {
                processingStore.markFailed(inboxMessage.messageId(), describe(ex));
            }
        }

        return processed;
    }

    @Override
    public int recoverStale(Duration processingTimeout) {
        Objects.requireNonNull(processingTimeout, "Processing timeout cannot be null");
        if (processingTimeout.isZero() || processingTimeout.isNegative()) {
            throw new IllegalArgumentException("Processing timeout must be positive");
        }
        Instant staleBefore = clock.instant().minus(processingTimeout);
        return processingStore.recoverStaleProcessing(staleBefore);
    }

    @Override
    public boolean requeue(IntegrationMessageId id) {
        Objects.requireNonNull(id, "Message ID cannot be null");
        return processingStore.requeueFailed(id);
    }

    private static String describe(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return ex.getClass().getSimpleName() + ": " + message.trim();
    }
}
