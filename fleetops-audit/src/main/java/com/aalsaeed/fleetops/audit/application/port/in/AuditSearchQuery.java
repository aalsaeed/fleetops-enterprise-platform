package com.aalsaeed.fleetops.audit.application.port.in;

import com.aalsaeed.fleetops.audit.domain.AuditOutcome;

import java.time.Instant;

public record AuditSearchQuery(
        Instant from,
        Instant to,
        String actorSubject,
        String action,
        String resourceType,
        String resourceId,
        AuditOutcome outcome,
        String correlationId,
        int offset,
        int limit) {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;

    public AuditSearchQuery {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Audit search 'from' cannot be after 'to'");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Audit search offset cannot be negative");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("Audit search limit must be between 1 and " + MAX_LIMIT);
        }

        actorSubject = optionalText(actorSubject);
        action = optionalText(action);
        resourceType = optionalText(resourceType);
        resourceId = optionalText(resourceId);
        correlationId = optionalText(correlationId);
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
