package com.aalsaeed.fleetops.integration.application.service;

import com.aalsaeed.fleetops.integration.application.port.in.RequeueFailedOutboxUseCase;
import com.aalsaeed.fleetops.integration.application.port.out.OutboxPublicationStore;
import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;

import java.time.Clock;
import java.util.Objects;

public final class OutboxRecoveryService implements RequeueFailedOutboxUseCase {

    private final OutboxPublicationStore publicationStore;
    private final Clock clock;

    public OutboxRecoveryService(OutboxPublicationStore publicationStore, Clock clock) {
        this.publicationStore = Objects.requireNonNull(publicationStore, "Publication store cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    @Override
    public boolean requeue(IntegrationMessageId messageId) {
        Objects.requireNonNull(messageId, "Message ID cannot be null");
        return publicationStore.requeueFailed(messageId, clock.instant());
    }
}
