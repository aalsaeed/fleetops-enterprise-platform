package com.aalsaeed.fleetops.integration.application.port.in;

public interface ProcessPendingInboxUseCase {

    int processPending(int batchSize);
}
