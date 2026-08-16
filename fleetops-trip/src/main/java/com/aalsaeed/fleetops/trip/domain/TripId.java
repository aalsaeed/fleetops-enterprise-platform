package com.aalsaeed.fleetops.trip.domain;

import java.util.Objects;
import java.util.UUID;

public record TripId(UUID value) {

    public TripId {
        Objects.requireNonNull(value, "Trip ID cannot be null");
    }

    public static TripId newId() {
        return new TripId(UUID.randomUUID());
    }

    public static TripId of(UUID value) {
        return new TripId(value);
    }
}
