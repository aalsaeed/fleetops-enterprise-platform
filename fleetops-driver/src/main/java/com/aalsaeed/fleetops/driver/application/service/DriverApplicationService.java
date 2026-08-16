package com.aalsaeed.fleetops.driver.application.service;

import com.aalsaeed.fleetops.driver.application.exception.DriverAlreadyExistsException;
import com.aalsaeed.fleetops.driver.application.exception.DriverNotFoundException;
import com.aalsaeed.fleetops.driver.application.port.in.ChangeDriverStatusUseCase;
import com.aalsaeed.fleetops.driver.application.port.in.CreateDriverCommand;
import com.aalsaeed.fleetops.driver.application.port.in.CreateDriverUseCase;
import com.aalsaeed.fleetops.driver.application.port.in.GetDriverUseCase;
import com.aalsaeed.fleetops.driver.application.port.out.DriverRepository;
import com.aalsaeed.fleetops.driver.domain.Driver;
import com.aalsaeed.fleetops.driver.domain.DriverId;
import com.aalsaeed.fleetops.driver.domain.DriverStatus;

import java.util.Objects;

public final class DriverApplicationService
        implements CreateDriverUseCase, GetDriverUseCase, ChangeDriverStatusUseCase {

    private final DriverRepository driverRepository;

    public DriverApplicationService(DriverRepository driverRepository) {
        this.driverRepository = Objects.requireNonNull(driverRepository, "Driver repository cannot be null");
    }

    @Override
    public Driver createDriver(CreateDriverCommand command) {
        Objects.requireNonNull(command, "Create driver command cannot be null");

        Driver driver = Driver.create(
                command.externalReference(),
                command.firstName(),
                command.lastName(),
                command.phoneNumber());

        if (driverRepository.existsByExternalReference(driver.externalReference())) {
            throw new DriverAlreadyExistsException(driver.externalReference());
        }

        return driverRepository.save(driver);
    }

    @Override
    public Driver getById(DriverId id) {
        Objects.requireNonNull(id, "Driver ID cannot be null");
        return driverRepository.findById(id)
                .orElseThrow(() -> new DriverNotFoundException(id.value().toString()));
    }

    @Override
    public Driver getByExternalReference(String externalReference) {
        String normalizedReference = requireText(externalReference, "External reference");
        return driverRepository.findByExternalReference(normalizedReference)
                .orElseThrow(() -> new DriverNotFoundException(normalizedReference));
    }

    @Override
    public Driver changeStatus(DriverId id, DriverStatus targetStatus) {
        Objects.requireNonNull(targetStatus, "Target driver status cannot be null");

        Driver driver = getById(id);
        driver.changeStatus(targetStatus);
        return driverRepository.save(driver);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }
}
