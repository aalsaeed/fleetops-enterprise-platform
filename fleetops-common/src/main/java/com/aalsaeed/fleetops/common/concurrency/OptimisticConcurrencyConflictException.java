package com.aalsaeed.fleetops.common.concurrency;

import java.util.Objects;

/**
 * Provider-neutral stale-write conflict raised when an aggregate revision no longer
 * matches the revision stored by persistence.
 */
public final class OptimisticConcurrencyConflictException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;

    public OptimisticConcurrencyConflictException(
            String resourceType,
            String resourceId,
            Throwable cause) {
        super(message(resourceType, resourceId), cause);
        this.resourceType = requireText(resourceType, "Resource type");
        this.resourceId = requireText(resourceId, "Resource ID");
    }

    public String resourceType() {
        return resourceType;
    }

    public String resourceId() {
        return resourceId;
    }

    private static String message(String resourceType, String resourceId) {
        return "%s %s was modified by another request; reload the current state and retry"
                .formatted(
                        requireText(resourceType, "Resource type"),
                        requireText(resourceId, "Resource ID"));
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }
}
