package com.aalsaeed.fleetops.trip.application.port.out;

import java.util.Objects;
import java.util.UUID;

public record DriverResource(UUID id, boolean operational) {

    public DriverResource {
        Objects.requireNonNull(id, "Driver resource ID cannot be null");
    }
}
