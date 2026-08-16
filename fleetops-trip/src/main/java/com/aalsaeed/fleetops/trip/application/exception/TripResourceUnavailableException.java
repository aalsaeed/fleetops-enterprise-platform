package com.aalsaeed.fleetops.trip.application.exception;

import java.util.UUID;

public final class TripResourceUnavailableException extends RuntimeException {

    public TripResourceUnavailableException(String resourceType, UUID id) {
        super(resourceType + " resource is not operational: " + id);
    }
}
