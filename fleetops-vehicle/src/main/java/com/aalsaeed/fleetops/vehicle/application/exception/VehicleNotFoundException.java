package com.aalsaeed.fleetops.vehicle.application.exception;

public final class VehicleNotFoundException extends RuntimeException {

    public VehicleNotFoundException(String identifier) {
        super("Vehicle not found: " + identifier);
    }
}
