package com.aalsaeed.fleetops.trip.api.rest;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignTripResourcesRequest(
        @NotNull UUID driverId,
        @NotNull UUID primaryVehicleId,
        UUID attachmentVehicleId) {
}
