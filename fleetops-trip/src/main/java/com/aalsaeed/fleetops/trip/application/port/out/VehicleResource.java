package com.aalsaeed.fleetops.trip.application.port.out;

import java.util.Objects;
import java.util.UUID;

public record VehicleResource(UUID id, VehicleResourceType type, boolean operational) {

    public VehicleResource {
        Objects.requireNonNull(id, "Vehicle resource ID cannot be null");
        Objects.requireNonNull(type, "Vehicle resource type cannot be null");
    }
}
