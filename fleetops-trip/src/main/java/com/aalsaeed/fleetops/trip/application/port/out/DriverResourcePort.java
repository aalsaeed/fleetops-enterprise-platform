package com.aalsaeed.fleetops.trip.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface DriverResourcePort {

    Optional<DriverResource> findById(UUID id);
}
