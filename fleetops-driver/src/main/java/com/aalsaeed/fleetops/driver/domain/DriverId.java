package com.aalsaeed.fleetops.driver.domain;

import java.util.Objects;
import java.util.UUID;

public record DriverId(UUID value) {

    public DriverId {
        Objects.requireNonNull(value, "Driver ID cannot be null");
    }

    public static DriverId newId() {
        return new DriverId(UUID.randomUUID());
    }

    public static DriverId of(UUID value) {
        return new DriverId(value);
    }
}
