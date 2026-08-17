package com.aalsaeed.fleetops.integration.domain.outbox;

import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import com.aalsaeed.fleetops.integration.domain.IdempotencyKey;
import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;

import java.time.Instant;
import java.util.Objects;

public final class IntegrationOutboxMessage {

    public static final String ERP_SHIPMENT_EVENT_TYPE = "ERP_SHIPMENT";
    public static final String SHIPMENT_AGGREGATE_TYPE = "SHIPMENT";

    private final IntegrationMessageId id;
    private final IdempotencyKey idempotencyKey;
    private final String eventType;
    private final String aggregateType;
    private final String aggregateId;
    private final String payload;
    private final OutboxStatus status;
    private final int attempts;
    private final Instant createdAt;
    private final Instant publishedAt;
    private final String lastError;

    private IntegrationOutboxMessage(
            IntegrationMessageId id,
            IdempotencyKey idempotencyKey,
            String eventType,
            String aggregateType,
            String aggregateId,
            String payload,
            OutboxStatus status,
            int attempts,
            Instant createdAt,
            Instant publishedAt,
            String lastError) {
        this.id = Objects.requireNonNull(id, "Outbox message ID cannot be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        this.eventType = requireText(eventType, "Event type");
        this.aggregateType = requireText(aggregateType, "Aggregate type");
        this.aggregateId = requireText(aggregateId, "Aggregate ID");
        this.payload = requireText(payload, "Payload");
        this.status = Objects.requireNonNull(status, "Outbox status cannot be null");
        if (attempts < 0) {
            throw new IllegalArgumentException("Outbox attempts cannot be negative");
        }
        this.attempts = attempts;
        this.createdAt = Objects.requireNonNull(createdAt, "Created-at timestamp cannot be null");
        this.publishedAt = publishedAt;
        this.lastError = normalizeOptionalText(lastError);
    }

    public static IntegrationOutboxMessage pending(
            ErpShipmentMessage message,
            String payload,
            Instant createdAt) {
        Objects.requireNonNull(message, "ERP shipment message cannot be null");
        return new IntegrationOutboxMessage(
                message.id(),
                message.idempotencyKey(),
                ERP_SHIPMENT_EVENT_TYPE,
                SHIPMENT_AGGREGATE_TYPE,
                message.shipmentReference(),
                payload,
                OutboxStatus.PENDING,
                0,
                createdAt,
                null,
                null);
    }

    public static IntegrationOutboxMessage restore(
            IntegrationMessageId id,
            IdempotencyKey idempotencyKey,
            String eventType,
            String aggregateType,
            String aggregateId,
            String payload,
            OutboxStatus status,
            int attempts,
            Instant createdAt,
            Instant publishedAt,
            String lastError) {
        return new IntegrationOutboxMessage(
                id,
                idempotencyKey,
                eventType,
                aggregateType,
                aggregateId,
                payload,
                status,
                attempts,
                createdAt,
                publishedAt,
                lastError);
    }

    public IntegrationMessageId id() {
        return id;
    }

    public IdempotencyKey idempotencyKey() {
        return idempotencyKey;
    }

    public String eventType() {
        return eventType;
    }

    public String aggregateType() {
        return aggregateType;
    }

    public String aggregateId() {
        return aggregateId;
    }

    public String payload() {
        return payload;
    }

    public OutboxStatus status() {
        return status;
    }

    public int attempts() {
        return attempts;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant publishedAt() {
        return publishedAt;
    }

    public String lastError() {
        return lastError;
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
