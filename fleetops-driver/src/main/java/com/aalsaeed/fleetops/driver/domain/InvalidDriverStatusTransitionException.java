package com.aalsaeed.fleetops.driver.domain;

public final class InvalidDriverStatusTransitionException extends RuntimeException {

    public InvalidDriverStatusTransitionException(DriverStatus current, DriverStatus target) {
        super("Driver status cannot transition from " + current + " to " + target);
    }
}
