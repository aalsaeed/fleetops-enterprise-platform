package com.aalsaeed.fleetops.vehicle.application.port.out;

import com.aalsaeed.fleetops.vehicle.domain.Vehicle;
import com.aalsaeed.fleetops.vehicle.domain.VehicleId;

import java.util.Optional;

public interface VehicleRepository {

    Vehicle save(Vehicle vehicle);

    Optional<Vehicle> findById(VehicleId id);

    Optional<Vehicle> findByExternalReference(String externalReference);

    boolean existsByExternalReference(String externalReference);
}
