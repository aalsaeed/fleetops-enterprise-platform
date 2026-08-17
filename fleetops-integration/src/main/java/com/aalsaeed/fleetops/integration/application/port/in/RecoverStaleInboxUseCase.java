package com.aalsaeed.fleetops.integration.application.port.in;

import java.time.Duration;

public interface RecoverStaleInboxUseCase {

    int recoverStale(Duration processingTimeout);
}
