package com.aalsaeed.fleetops.vehicle.application.port.in;

import com.aalsaeed.fleetops.vehicle.domain.VehicleType;

public record CreateVehicleCommand(
        String externalReference,
        String description,
        VehicleType type,
        String serialNumber) {
}
