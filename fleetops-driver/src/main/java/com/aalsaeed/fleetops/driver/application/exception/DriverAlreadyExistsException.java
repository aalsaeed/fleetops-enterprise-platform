package com.aalsaeed.fleetops.driver.application.exception;

public final class DriverAlreadyExistsException extends RuntimeException {

    public DriverAlreadyExistsException(String externalReference) {
        super("Driver already exists for external reference: " + externalReference);
    }
}
