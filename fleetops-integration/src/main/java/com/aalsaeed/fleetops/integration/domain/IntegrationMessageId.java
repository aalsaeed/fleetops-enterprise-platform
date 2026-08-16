package com.aalsaeed.fleetops.integration.domain;

import java.util.Objects;
import java.util.UUID;

public record IntegrationMessageId(UUID value) {

    public IntegrationMessageId {
        Objects.requireNonNull(value, "Integration message ID cannot be null");
    }

    public static IntegrationMessageId newId() {
        return new IntegrationMessageId(UUID.randomUUID());
    }

    public static IntegrationMessageId of(UUID value) {
        return new IntegrationMessageId(value);
    }
}
