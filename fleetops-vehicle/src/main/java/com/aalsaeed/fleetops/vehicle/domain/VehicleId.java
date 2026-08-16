package com.aalsaeed.fleetops.vehicle.domain;

import java.util.Objects;
import java.util.UUID;

public record VehicleId(UUID value) {

    public VehicleId {
        Objects.requireNonNull(value, "Vehicle ID cannot be null");
    }

    public static VehicleId newId() {
        return new VehicleId(UUID.randomUUID());
    }

    public static VehicleId of(UUID value) {
        return new VehicleId(value);
    }
}
