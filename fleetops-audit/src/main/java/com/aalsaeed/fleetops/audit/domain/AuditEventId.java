package com.aalsaeed.fleetops.audit.domain;

import java.util.Objects;
import java.util.UUID;

public record AuditEventId(UUID value) {

    public AuditEventId {
        Objects.requireNonNull(value, "Audit event ID cannot be null");
    }

    public static AuditEventId newId() {
        return new AuditEventId(UUID.randomUUID());
    }

    public static AuditEventId of(UUID value) {
        return new AuditEventId(value);
    }
}
