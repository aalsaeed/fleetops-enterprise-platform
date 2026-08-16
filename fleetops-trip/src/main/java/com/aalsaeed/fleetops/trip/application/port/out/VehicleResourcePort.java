package com.aalsaeed.fleetops.trip.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface VehicleResourcePort {

    Optional<VehicleResource> findById(UUID id);
}
