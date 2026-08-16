package com.aalsaeed.fleetops.trip.domain;

import java.util.Objects;
import java.util.UUID;

public record VehicleReference(UUID value) {

    public VehicleReference {
        Objects.requireNonNull(value, "Vehicle reference cannot be null");
    }

    public static VehicleReference of(UUID value) {
        return new VehicleReference(value);
    }
}
