package com.aalsaeed.fleetops.integration.operations.api;

import java.util.Objects;
import java.util.UUID;

final class IntegrationRecoveryNotAvailableException extends RuntimeException {

    IntegrationRecoveryNotAvailableException(String channel, UUID messageId) {
        super("Integration " + requireText(channel) + " message "
                + Objects.requireNonNull(messageId, "Message ID cannot be null")
                + " is not in a recoverable FAILED state");
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Recovery channel cannot be blank");
        }
        return value.trim();
    }
}
