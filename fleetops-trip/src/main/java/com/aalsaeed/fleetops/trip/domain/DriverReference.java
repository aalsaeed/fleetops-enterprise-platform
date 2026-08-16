package com.aalsaeed.fleetops.trip.domain;

import java.util.Objects;
import java.util.UUID;

public record DriverReference(UUID value) {

    public DriverReference {
        Objects.requireNonNull(value, "Driver reference cannot be null");
    }

    public static DriverReference of(UUID value) {
        return new DriverReference(value);
    }
}
