package com.aalsaeed.fleetops.trip.application.port.in;

import com.aalsaeed.fleetops.trip.domain.Trip;
import com.aalsaeed.fleetops.trip.domain.TripId;

public interface TripLifecycleUseCase {

    Trip startTrip(TripId id);

    Trip completeTrip(TripId id);

    Trip cancelTrip(TripId id);
}
