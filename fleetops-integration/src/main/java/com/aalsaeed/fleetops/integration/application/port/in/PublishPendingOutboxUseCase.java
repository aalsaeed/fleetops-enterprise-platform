package com.aalsaeed.fleetops.integration.application.port.in;

public interface PublishPendingOutboxUseCase {

    int publishPending(int batchSize);
}
