package com.aalsaeed.fleetops.integration.application.port.out;

import com.aalsaeed.fleetops.integration.application.port.in.IntegrationOperationsSnapshot;

public interface DeadLetterQueueMetricsPort {

    IntegrationOperationsSnapshot.DeadLetterQueueSnapshot getSnapshot();
}
