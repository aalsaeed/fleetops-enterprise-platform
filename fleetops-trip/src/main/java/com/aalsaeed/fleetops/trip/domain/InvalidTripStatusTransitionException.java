package com.aalsaeed.fleetops.trip.domain;

public final class InvalidTripStatusTransitionException extends RuntimeException {

    public InvalidTripStatusTransitionException(TripStatus current, TripStatus target) {
        super("Trip status cannot transition from " + current + " to " + target);
    }
}
