package com.aalsaeed.fleetops.trip.application.port.in;

import com.aalsaeed.fleetops.trip.domain.Trip;
import com.aalsaeed.fleetops.trip.domain.TripId;

public interface GetTripUseCase {

    Trip getById(TripId id);

    Trip getByExternalReference(String externalReference);
}
