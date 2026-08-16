package com.aalsaeed.fleetops.trip.application.exception;

public final class TripNotFoundException extends RuntimeException {

    public TripNotFoundException(String reference) {
        super("Trip not found: " + reference);
    }
}
