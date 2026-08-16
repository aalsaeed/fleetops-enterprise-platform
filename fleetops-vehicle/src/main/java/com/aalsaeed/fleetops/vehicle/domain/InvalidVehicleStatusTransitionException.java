package com.aalsaeed.fleetops.vehicle.domain;

public final class InvalidVehicleStatusTransitionException extends IllegalStateException {

    public InvalidVehicleStatusTransitionException(VehicleStatus current, VehicleStatus target) {
        super("Vehicle status cannot transition from " + current + " to " + target);
    }
}
