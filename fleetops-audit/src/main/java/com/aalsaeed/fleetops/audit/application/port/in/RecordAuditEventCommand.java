package com.aalsaeed.fleetops.audit.application.port.in;

import com.aalsaeed.fleetops.audit.domain.AuditOutcome;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record RecordAuditEventCommand(
        String actorSubject,
        String actorDisplayName,
        Set<String> actorAuthorities,
        String action,
        String resourceType,
        String resourceId,
        AuditOutcome outcome,
        String correlationId,
        Map<String, String> metadata) {

    public RecordAuditEventCommand {
        actorSubject = requireText(actorSubject, "Actor subject");
        actorDisplayName = optionalText(actorDisplayName);
        actorAuthorities = immutableAuthorities(actorAuthorities);
        action = requireText(action, "Audit action");
        resourceType = requireText(resourceType, "Resource type");
        resourceId = optionalText(resourceId);
        Objects.requireNonNull(outcome, "Audit outcome cannot be null");
        correlationId = requireText(correlationId, "Correlation ID");
        metadata = immutableMetadata(metadata);
    }

    private static Set<String> immutableAuthorities(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.trim());
            }
        }
        return Set.copyOf(normalized);
    }

    private static Map<String, String> immutableMetadata(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(
                requireText(key, "Metadata key"),
                requireText(value, "Metadata value")));
        return Map.copyOf(copy);
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value.trim();
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
