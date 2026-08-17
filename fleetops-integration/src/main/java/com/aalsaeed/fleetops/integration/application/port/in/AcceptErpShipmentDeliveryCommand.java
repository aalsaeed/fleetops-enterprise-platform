package com.aalsaeed.fleetops.integration.application.port.in;

import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;

import java.time.Instant;
import java.util.Objects;

public record AcceptErpShipmentDeliveryCommand(
        ErpShipmentMessage message,
        String payload,
        String eventType,
        String aggregateType,
        String aggregateId,
        Instant receivedAt) {

    public AcceptErpShipmentDeliveryCommand {
        Objects.requireNonNull(message, "ERP shipment message cannot be null");
        payload = requireText(payload, "Payload");
        eventType = requireText(eventType, "Event type");
        aggregateType = requireText(aggregateType, "Aggregate type");
        aggregateId = requireText(aggregateId, "Aggregate ID");
        Objects.requireNonNull(receivedAt, "Received-at timestamp cannot be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }
}
