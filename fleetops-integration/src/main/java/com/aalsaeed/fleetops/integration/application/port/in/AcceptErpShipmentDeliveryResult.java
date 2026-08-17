package com.aalsaeed.fleetops.integration.application.port.in;

import com.aalsaeed.fleetops.integration.domain.IdempotencyKey;
import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;

import java.util.Objects;

public record AcceptErpShipmentDeliveryResult(
        IntegrationMessageId messageId,
        IdempotencyKey idempotencyKey,
        boolean duplicate) {

    public AcceptErpShipmentDeliveryResult {
        Objects.requireNonNull(messageId, "Message ID cannot be null");
        Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
    }
}
