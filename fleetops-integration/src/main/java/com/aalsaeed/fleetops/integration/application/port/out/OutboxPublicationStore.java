package com.aalsaeed.fleetops.integration.application.port.out;

import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;
import com.aalsaeed.fleetops.integration.domain.outbox.IntegrationOutboxMessage;

import java.time.Instant;
import java.util.List;

public interface OutboxPublicationStore {

    List<IntegrationOutboxMessage> claimPending(int limit, Instant claimedAt);

    void markPublished(IntegrationMessageId id, Instant publishedAt);

    void scheduleRetry(IntegrationMessageId id, String error, Instant nextAttemptAt);

    void markFailed(IntegrationMessageId id, String error);

    int recoverStalePublishing(Instant staleBefore, Instant retryAt);
}
