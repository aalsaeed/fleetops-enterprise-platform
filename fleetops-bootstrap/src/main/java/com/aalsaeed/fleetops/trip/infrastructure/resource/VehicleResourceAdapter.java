package com.aalsaeed.fleetops.trip.infrastructure.resource;

import com.aalsaeed.fleetops.trip.application.port.out.VehicleResource;
import com.aalsaeed.fleetops.trip.application.port.out.VehicleResourcePort;
import com.aalsaeed.fleetops.trip.application.port.out.VehicleResourceType;
import com.aalsaeed.fleetops.vehicle.application.port.out.VehicleRepository;
import com.aalsaeed.fleetops.vehicle.domain.VehicleId;
import com.aalsaeed.fleetops.vehicle.domain.VehicleStatus;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class VehicleResourceAdapter implements VehicleResourcePort {

    private final VehicleRepository vehicleRepository;

    public VehicleResourceAdapter(VehicleRepository vehicleRepository) {
        this.vehicleRepository = Objects.requireNonNull(vehicleRepository, "Vehicle repository cannot be null");
    }

    @Override
    public Optional<VehicleResource> findById(UUID id) {
        Objects.requireNonNull(id, "Vehicle ID cannot be null");
        return vehicleRepository.findById(VehicleId.of(id))
                .map(vehicle -> new VehicleResource(
                        vehicle.id().value(),
                        VehicleResourceType.valueOf(vehicle.type().name()),
                        vehicle.status() == VehicleStatus.ACTIVE));
    }
}
