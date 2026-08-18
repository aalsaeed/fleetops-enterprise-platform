package com.aalsaeed.fleetops.audit.web;

import com.aalsaeed.fleetops.audit.domain.AuditEvent;
import com.aalsaeed.fleetops.audit.domain.AuditOutcome;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        Instant occurredAt,
        String actorSubject,
        String actorDisplayName,
        Set<String> actorAuthorities,
        String action,
        String resourceType,
        String resourceId,
        AuditOutcome outcome,
        String correlationId,
        Map<String, String> metadata) {

    static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.id().value(),
                event.occurredAt(),
                event.actorSubject(),
                event.actorDisplayName(),
                event.actorAuthorities(),
                event.action(),
                event.resourceType(),
                event.resourceId(),
                event.outcome(),
                event.correlationId(),
                event.metadata());
    }
}
