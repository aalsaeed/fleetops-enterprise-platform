package com.aalsaeed.fleetops.vehicle.api.rest;

import com.aalsaeed.fleetops.vehicle.domain.Vehicle;
import com.aalsaeed.fleetops.vehicle.domain.VehicleStatus;
import com.aalsaeed.fleetops.vehicle.domain.VehicleType;

import java.util.UUID;

public record VehicleResponse(
        UUID id,
        String externalReference,
        String description,
        VehicleType type,
        String serialNumber,
        VehicleStatus status) {

    static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.id().value(),
                vehicle.externalReference(),
                vehicle.description(),
                vehicle.type(),
                vehicle.serialNumber(),
                vehicle.status());
    }
}
