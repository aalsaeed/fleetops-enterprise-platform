package com.aalsaeed.fleetops.integration.infrastructure.persistence.outbox;

import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "erp_shipment_receipts")
public class ErpShipmentReceiptJpaEntity {

    @Id
    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 300)
    private String idempotencyKey;

    @Column(name = "source_system", nullable = false, length = 50)
    private String sourceSystem;

    @Column(name = "source_message_id", nullable = false, length = 150)
    private String sourceMessageId;

    @Column(name = "shipment_reference", nullable = false, length = 100)
    private String shipmentReference;

    @Column(name = "operation", nullable = false, length = 20)
    private String operation;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "correlation_id", length = 150)
    private String correlationId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected ErpShipmentReceiptJpaEntity() {
    }

    private ErpShipmentReceiptJpaEntity(
            UUID messageId,
            String idempotencyKey,
            String sourceSystem,
            String sourceMessageId,
            String shipmentReference,
            String operation,
            int schemaVersion,
            Instant occurredAt,
            String correlationId,
            Instant receivedAt) {
        this.messageId = messageId;
        this.idempotencyKey = idempotencyKey;
        this.sourceSystem = sourceSystem;
        this.sourceMessageId = sourceMessageId;
        this.shipmentReference = shipmentReference;
        this.operation = operation;
        this.schemaVersion = schemaVersion;
        this.occurredAt = occurredAt;
        this.correlationId = correlationId;
        this.receivedAt = receivedAt;
    }

    static ErpShipmentReceiptJpaEntity from(ErpShipmentMessage message, Instant receivedAt) {
        return new ErpShipmentReceiptJpaEntity(
                message.id().value(),
                message.idempotencyKey().value(),
                message.sourceSystem(),
                message.sourceMessageId(),
                message.shipmentReference(),
                message.operation().name(),
                message.schemaVersion(),
                message.occurredAt(),
                message.correlationId(),
                receivedAt);
    }

    UUID getMessageId() {
        return messageId;
    }

    String getIdempotencyKey() {
        return idempotencyKey;
    }

    Instant getReceivedAt() {
        return receivedAt;
    }
}
