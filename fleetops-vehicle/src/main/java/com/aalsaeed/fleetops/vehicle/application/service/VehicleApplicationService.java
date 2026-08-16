package com.aalsaeed.fleetops.vehicle.application.service;

import com.aalsaeed.fleetops.vehicle.application.exception.VehicleAlreadyExistsException;
import com.aalsaeed.fleetops.vehicle.application.exception.VehicleNotFoundException;
import com.aalsaeed.fleetops.vehicle.application.port.in.ChangeVehicleStatusUseCase;
import com.aalsaeed.fleetops.vehicle.application.port.in.CreateVehicleCommand;
import com.aalsaeed.fleetops.vehicle.application.port.in.CreateVehicleUseCase;
import com.aalsaeed.fleetops.vehicle.application.port.in.GetVehicleUseCase;
import com.aalsaeed.fleetops.vehicle.application.port.out.VehicleRepository;
import com.aalsaeed.fleetops.vehicle.domain.Vehicle;
import com.aalsaeed.fleetops.vehicle.domain.VehicleId;
import com.aalsaeed.fleetops.vehicle.domain.VehicleStatus;

import java.util.Objects;

public final class VehicleApplicationService
        implements CreateVehicleUseCase, GetVehicleUseCase, ChangeVehicleStatusUseCase {

    private final VehicleRepository vehicleRepository;

    public VehicleApplicationService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = Objects.requireNonNull(vehicleRepository, "Vehicle repository cannot be null");
    }

    @Override
    public Vehicle createVehicle(CreateVehicleCommand command) {
        Objects.requireNonNull(command, "Create vehicle command cannot be null");

        Vehicle vehicle = Vehicle.create(
                command.externalReference(),
                command.description(),
                command.type(),
                command.serialNumber());

        if (vehicleRepository.existsByExternalReference(vehicle.externalReference())) {
            throw new VehicleAlreadyExistsException(vehicle.externalReference());
        }

        return vehicleRepository.save(vehicle);
    }

    @Override
    public Vehicle getById(VehicleId id) {
        Objects.requireNonNull(id, "Vehicle ID cannot be null");
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(id.value().toString()));
    }

    @Override
    public Vehicle getByExternalReference(String externalReference) {
        String normalizedReference = requireText(externalReference, "External reference");
        return vehicleRepository.findByExternalReference(normalizedReference)
                .orElseThrow(() -> new VehicleNotFoundException(normalizedReference));
    }

    @Override
    public Vehicle changeStatus(VehicleId id, VehicleStatus targetStatus) {
        Objects.requireNonNull(targetStatus, "Target vehicle status cannot be null");

        Vehicle vehicle = getById(id);
        vehicle.changeStatus(targetStatus);
        return vehicleRepository.save(vehicle);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }
}
