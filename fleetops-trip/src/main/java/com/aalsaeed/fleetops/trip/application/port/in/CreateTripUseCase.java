package com.aalsaeed.fleetops.trip.application.port.in;

import com.aalsaeed.fleetops.trip.domain.Trip;

public interface CreateTripUseCase {

    Trip createTrip(CreateTripCommand command);
}
