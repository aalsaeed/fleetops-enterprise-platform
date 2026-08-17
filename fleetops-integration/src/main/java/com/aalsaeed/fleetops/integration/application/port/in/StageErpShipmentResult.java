package com.aalsaeed.fleetops.integration.application.port.in;

import com.aalsaeed.fleetops.integration.domain.IdempotencyKey;
import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;

import java.time.Instant;
import java.util.Objects;

public record StageErpShipmentResult(
        IntegrationMessageId messageId,
        IdempotencyKey idempotencyKey,
        boolean duplicate,
        Instant stagedAt) {

    public StageErpShipmentResult {
        Objects.requireNonNull(messageId, "Message ID cannot be null");
        Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        Objects.requireNonNull(stagedAt, "Staged-at timestamp cannot be null");
    }
}
