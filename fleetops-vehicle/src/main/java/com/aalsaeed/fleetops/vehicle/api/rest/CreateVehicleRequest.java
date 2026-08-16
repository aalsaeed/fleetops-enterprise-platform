package com.aalsaeed.fleetops.vehicle.api.rest;

import com.aalsaeed.fleetops.vehicle.domain.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVehicleRequest(
        @NotBlank @Size(max = 100) String externalReference,
        @NotBlank @Size(max = 200) String description,
        @NotNull VehicleType type,
        @Size(max = 100) String serialNumber) {
}
