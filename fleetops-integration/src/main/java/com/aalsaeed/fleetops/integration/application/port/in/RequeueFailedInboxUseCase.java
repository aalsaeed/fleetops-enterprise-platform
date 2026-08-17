package com.aalsaeed.fleetops.integration.application.port.in;

import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;

public interface RequeueFailedInboxUseCase {

    boolean requeue(IntegrationMessageId id);
}
