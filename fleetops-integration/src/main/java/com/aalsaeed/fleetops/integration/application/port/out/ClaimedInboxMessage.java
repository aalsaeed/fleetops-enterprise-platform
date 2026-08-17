package com.aalsaeed.fleetops.integration.application.port.out;

import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;

import java.util.Objects;

public record ClaimedInboxMessage(
        IntegrationMessageId messageId,
        String payload,
        int attempts) {

    public ClaimedInboxMessage {
        Objects.requireNonNull(messageId, "Message ID cannot be null");
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Inbox payload cannot be blank");
        }
        if (attempts < 1) {
            throw new IllegalArgumentException("Inbox processing attempts must be at least 1");
        }
    }
}
