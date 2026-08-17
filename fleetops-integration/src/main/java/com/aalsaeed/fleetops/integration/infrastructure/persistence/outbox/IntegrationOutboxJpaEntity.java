package com.aalsaeed.fleetops.integration.infrastructure.persistence.outbox;

import com.aalsaeed.fleetops.integration.domain.outbox.IntegrationOutboxMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "integration_outbox")
public class IntegrationOutboxJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 300)
    private String idempotencyKey;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 150)
    private String aggregateId;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    protected IntegrationOutboxJpaEntity() {
    }

    private IntegrationOutboxJpaEntity(
            UUID id,
            String idempotencyKey,
            String eventType,
            String aggregateType,
            String aggregateId,
            String payload,
            String status,
            int attempts,
            Instant createdAt,
            Instant publishedAt,
            String lastError) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = status;
        this.attempts = attempts;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
        this.lastError = lastError;
    }

    static IntegrationOutboxJpaEntity from(IntegrationOutboxMessage message) {
        return new IntegrationOutboxJpaEntity(
                message.id().value(),
                message.idempotencyKey().value(),
                message.eventType(),
                message.aggregateType(),
                message.aggregateId(),
                message.payload(),
                message.status().name(),
                message.attempts(),
                message.createdAt(),
                message.publishedAt(),
                message.lastError());
    }
}
