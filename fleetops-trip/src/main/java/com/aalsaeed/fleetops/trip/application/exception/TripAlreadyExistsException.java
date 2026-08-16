package com.aalsaeed.fleetops.trip.application.exception;

public final class TripAlreadyExistsException extends RuntimeException {

    public TripAlreadyExistsException(String externalReference) {
        super("Trip already exists with external reference: " + externalReference);
    }
}
