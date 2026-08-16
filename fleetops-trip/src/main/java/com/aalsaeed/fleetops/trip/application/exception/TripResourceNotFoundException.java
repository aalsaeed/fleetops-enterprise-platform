package com.aalsaeed.fleetops.trip.application.exception;

import java.util.UUID;

public final class TripResourceNotFoundException extends RuntimeException {

    public TripResourceNotFoundException(String resourceType, UUID id) {
        super(resourceType + " resource not found: " + id);
    }
}
