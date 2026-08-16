package com.aalsaeed.fleetops.vehicle.application.exception;

public final class VehicleAlreadyExistsException extends RuntimeException {

    public VehicleAlreadyExistsException(String externalReference) {
        super("Vehicle already exists with external reference: " + externalReference);
    }
}
