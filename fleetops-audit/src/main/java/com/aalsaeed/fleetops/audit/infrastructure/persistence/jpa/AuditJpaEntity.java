package com.aalsaeed.fleetops.audit.infrastructure.persistence.jpa;

import com.aalsaeed.fleetops.audit.domain.AuditEvent;
import com.aalsaeed.fleetops.audit.domain.AuditEventId;
import com.aalsaeed.fleetops.audit.domain.AuditOutcome;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
class AuditJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_subject", nullable = false, updatable = false, length = 255)
    private String actorSubject;

    @Column(name = "actor_display_name", updatable = false, length = 255)
    private String actorDisplayName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "audit_event_authorities",
            joinColumns = @JoinColumn(name = "audit_event_id", nullable = false))
    @Column(name = "authority", nullable = false, length = 100)
    private Set<String> actorAuthorities = new LinkedHashSet<>();

    @Column(name = "action", nullable = false, updatable = false, length = 120)
    private String action;

    @Column(name = "resource_type", nullable = false, updatable = false, length = 100)
    private String resourceType;

    @Column(name = "resource_id", updatable = false, length = 255)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, updatable = false, length = 20)
    private AuditOutcome outcome;

    @Column(name = "correlation_id", nullable = false, updatable = false, length = 128)
    private String correlationId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "audit_event_metadata",
            joinColumns = @JoinColumn(name = "audit_event_id", nullable = false))
    @MapKeyColumn(name = "metadata_key", length = 120)
    @Column(name = "metadata_value", nullable = false, length = 1000)
    private Map<String, String> metadata = new LinkedHashMap<>();

    protected AuditJpaEntity() {
    }

    AuditJpaEntity(AuditEvent event) {
        this.id = event.id().value();
        this.occurredAt = event.occurredAt();
        this.actorSubject = event.actorSubject();
        this.actorDisplayName = event.actorDisplayName();
        this.actorAuthorities.addAll(event.actorAuthorities());
        this.action = event.action();
        this.resourceType = event.resourceType();
        this.resourceId = event.resourceId();
        this.outcome = event.outcome();
        this.correlationId = event.correlationId();
        this.metadata.putAll(event.metadata());
    }

    AuditEvent toDomain() {
        return new AuditEvent(
                AuditEventId.of(id),
                occurredAt,
                actorSubject,
                actorDisplayName,
                actorAuthorities,
                action,
                resourceType,
                resourceId,
                outcome,
                correlationId,
                metadata);
    }
}
