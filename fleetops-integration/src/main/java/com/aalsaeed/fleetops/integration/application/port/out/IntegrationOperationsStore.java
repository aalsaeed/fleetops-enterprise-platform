package com.aalsaeed.fleetops.integration.application.port.out;

import com.aalsaeed.fleetops.integration.application.port.in.IntegrationOperationsSnapshot;

import java.time.Instant;

public interface IntegrationOperationsStore {

    IntegrationOperationsSnapshot.OutboxSnapshot loadOutbox(Instant staleBefore, int failureLimit);

    IntegrationOperationsSnapshot.InboxSnapshot loadInbox(Instant staleBefore, int failureLimit);
}
