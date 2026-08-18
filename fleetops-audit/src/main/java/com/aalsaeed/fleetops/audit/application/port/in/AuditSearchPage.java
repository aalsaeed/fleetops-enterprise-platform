package com.aalsaeed.fleetops.audit.application.port.in;

import com.aalsaeed.fleetops.audit.domain.AuditEvent;

import java.util.List;
import java.util.Objects;

public record AuditSearchPage(
        List<AuditEvent> items,
        long total,
        int offset,
        int limit) {

    public AuditSearchPage {
        items = List.copyOf(Objects.requireNonNull(items, "Audit search items cannot be null"));
        if (total < 0) {
            throw new IllegalArgumentException("Audit search total cannot be negative");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Audit search offset cannot be negative");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Audit search limit must be positive");
        }
    }

    public boolean hasMore() {
        return (long) offset + items.size() < total;
    }
}
