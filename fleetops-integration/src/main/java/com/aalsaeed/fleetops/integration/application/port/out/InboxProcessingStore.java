package com.aalsaeed.fleetops.integration.application.port.out;

import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;

import java.time.Instant;
import java.util.List;

public interface InboxProcessingStore {

    List<ClaimedInboxMessage> claimPending(int limit, Instant claimedAt);

    void markProcessed(IntegrationMessageId id, Instant processedAt);

    void markFailed(IntegrationMessageId id, String error);

    int recoverStaleProcessing(Instant staleBefore);

    boolean requeueFailed(IntegrationMessageId id);
}
