package com.aalsaeed.fleetops.trip.application.port.out;

import com.aalsaeed.fleetops.trip.domain.Trip;
import com.aalsaeed.fleetops.trip.domain.TripId;

import java.util.Optional;

public interface TripRepository {

    Trip save(Trip trip);

    Optional<Trip> findById(TripId id);

    Optional<Trip> findByExternalReference(String externalReference);

    boolean existsByExternalReference(String externalReference);
}
