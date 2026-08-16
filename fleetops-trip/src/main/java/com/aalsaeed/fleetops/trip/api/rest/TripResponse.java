package com.aalsaeed.fleetops.trip.api.rest;

import com.aalsaeed.fleetops.trip.domain.Trip;
import com.aalsaeed.fleetops.trip.domain.TripStatus;

import java.util.UUID;

public record TripResponse(
        UUID id,
        String externalReference,
        UUID driverId,
        UUID primaryVehicleId,
        UUID attachmentVehicleId,
        TripStatus status) {

    public static TripResponse from(Trip trip) {
        return new TripResponse(
                trip.id().value(),
                trip.externalReference(),
                trip.driverReference() == null ? null : trip.driverReference().value(),
                trip.primaryVehicleReference() == null ? null : trip.primaryVehicleReference().value(),
                trip.attachmentVehicleReference() == null ? null : trip.attachmentVehicleReference().value(),
                trip.status());
    }
}
