package com.aalsaeed.fleetops.trip.application.port.in;

import com.aalsaeed.fleetops.trip.domain.TripId;

import java.util.UUID;

public record AssignTripResourcesCommand(
        TripId tripId,
        UUID driverId,
        UUID primaryVehicleId,
        UUID attachmentVehicleId) {
}
