package com.aalsaeed.fleetops.integration.application.service;

import com.aalsaeed.fleetops.integration.application.port.in.PublishPendingOutboxUseCase;
import com.aalsaeed.fleetops.integration.application.port.out.OutboxMessagePublisher;
import com.aalsaeed.fleetops.integration.application.port.out.OutboxPublicationStore;
import com.aalsaeed.fleetops.integration.domain.outbox.IntegrationOutboxMessage;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class OutboxPublicationService implements PublishPendingOutboxUseCase {

    private final OutboxPublicationStore publicationStore;
    private final OutboxMessagePublisher messagePublisher;
    private final OutboxRetryPolicy retryPolicy;
    private final Clock clock;

    public OutboxPublicationService(
            OutboxPublicationStore publicationStore,
            OutboxMessagePublisher messagePublisher,
            OutboxRetryPolicy retryPolicy,
            Clock clock) {
        this.publicationStore = Objects.requireNonNull(publicationStore, "Publication store cannot be null");
        this.messagePublisher = Objects.requireNonNull(messagePublisher, "Message publisher cannot be null");
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "Retry policy cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    @Override
    public int publishPending(int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be at least 1");
        }

        Instant now = clock.instant();
        publicationStore.recoverStalePublishing(
                now.minus(retryPolicy.stalePublishingTimeout()),
                now);

        List<IntegrationOutboxMessage> claimed = publicationStore.claimPending(batchSize, now);
        int published = 0;

        for (IntegrationOutboxMessage message : claimed) {
            try {
                messagePublisher.publish(message);
                publicationStore.markPublished(message.id(), clock.instant());
                published++;
            } catch (RuntimeException ex) {
                handleFailure(message, ex);
            }
        }

        return published;
    }

    private void handleFailure(IntegrationOutboxMessage message, RuntimeException exception) {
        String error = describe(exception);
        if (message.attempts() >= retryPolicy.maxAttempts()) {
            publicationStore.markFailed(message.id(), error);
            return;
        }

        Instant nextAttemptAt = clock.instant().plus(retryPolicy.delayAfterAttempt(message.attempts()));
        publicationStore.scheduleRetry(message.id(), error, nextAttemptAt);
    }

    private static String describe(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return ex.getClass().getSimpleName() + ": " + message.trim();
    }
}
