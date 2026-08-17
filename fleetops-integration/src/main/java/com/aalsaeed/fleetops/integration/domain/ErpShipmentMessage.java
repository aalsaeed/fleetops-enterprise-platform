package com.aalsaeed.fleetops.integration.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public final class ErpShipmentMessage {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    private final IntegrationMessageId id;
    private final int schemaVersion;
    private final String sourceSystem;
    private final String sourceMessageId;
    private final String shipmentReference;
    private final ErpShipmentOperation operation;
    private final Instant occurredAt;
    private final String correlationId;
    private final IdempotencyKey idempotencyKey;

    private ErpShipmentMessage(
            IntegrationMessageId id,
            int schemaVersion,
            String sourceSystem,
            String sourceMessageId,
            String shipmentReference,
            ErpShipmentOperation operation,
            Instant occurredAt,
            String correlationId) {
        this.id = Objects.requireNonNull(id, "Integration message ID cannot be null");
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("Schema version must be at least 1");
        }
        this.schemaVersion = schemaVersion;
        this.sourceSystem = requireText(sourceSystem, "Source system").toUpperCase(Locale.ROOT);
        this.sourceMessageId = requireText(sourceMessageId, "Source message ID");
        this.shipmentReference = requireText(shipmentReference, "Shipment reference");
        this.operation = Objects.requireNonNull(operation, "Shipment operation cannot be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred-at timestamp cannot be null");
        this.correlationId = normalizeOptionalText(correlationId);
        this.idempotencyKey = IdempotencyKey.fromSource(this.sourceSystem, this.sourceMessageId);
    }

    public static ErpShipmentMessage create(
            String sourceSystem,
            String sourceMessageId,
            String shipmentReference,
            ErpShipmentOperation operation,
            Instant occurredAt,
            String correlationId) {
        return new ErpShipmentMessage(
                IntegrationMessageId.newId(),
                CURRENT_SCHEMA_VERSION,
                sourceSystem,
                sourceMessageId,
                shipmentReference,
                operation,
                occurredAt,
                correlationId);
    }

    public static ErpShipmentMessage restore(
            IntegrationMessageId id,
            int schemaVersion,
            String sourceSystem,
            String sourceMessageId,
            String shipmentReference,
            ErpShipmentOperation operation,
            Instant occurredAt,
            String correlationId) {
        return new ErpShipmentMessage(
                id,
                schemaVersion,
                sourceSystem,
                sourceMessageId,
                shipmentReference,
                operation,
                occurredAt,
                correlationId);
    }

    public IntegrationMessageId id() {
        return id;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public String sourceSystem() {
        return sourceSystem;
    }

    public String sourceMessageId() {
        return sourceMessageId;
    }

    public String shipmentReference() {
        return shipmentReference;
    }

    public ErpShipmentOperation operation() {
        return operation;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public String correlationId() {
        return correlationId;
    }

    public IdempotencyKey idempotencyKey() {
        return idempotencyKey;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
