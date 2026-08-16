package com.aalsaeed.fleetops.vehicle.api.rest;

import com.aalsaeed.fleetops.vehicle.domain.VehicleStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeVehicleStatusRequest(@NotNull VehicleStatus status) {
}
