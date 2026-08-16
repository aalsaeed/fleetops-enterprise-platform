package com.aalsaeed.fleetops.driver.application.exception;

public final class DriverNotFoundException extends RuntimeException {

    public DriverNotFoundException(String reference) {
        super("Driver not found: " + reference);
    }
}
