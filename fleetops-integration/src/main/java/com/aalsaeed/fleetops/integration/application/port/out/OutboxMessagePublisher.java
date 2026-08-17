package com.aalsaeed.fleetops.integration.application.port.out;

import com.aalsaeed.fleetops.integration.domain.outbox.IntegrationOutboxMessage;

public interface OutboxMessagePublisher {

    void publish(IntegrationOutboxMessage message);
}
