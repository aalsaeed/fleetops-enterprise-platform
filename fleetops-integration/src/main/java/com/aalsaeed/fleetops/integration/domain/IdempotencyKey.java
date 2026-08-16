package com.aalsaeed.fleetops.integration.domain;

import java.util.Locale;

public record IdempotencyKey(String value) {

    public IdempotencyKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Idempotency key cannot be blank");
        }
        value = value.trim();
    }

    public static IdempotencyKey fromSource(String sourceSystem, String sourceMessageId) {
        String normalizedSource = requireText(sourceSystem, "Source system").toUpperCase(Locale.ROOT);
        String normalizedMessageId = requireText(sourceMessageId, "Source message ID");
        return new IdempotencyKey(normalizedSource + ":" + normalizedMessageId);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }
}
