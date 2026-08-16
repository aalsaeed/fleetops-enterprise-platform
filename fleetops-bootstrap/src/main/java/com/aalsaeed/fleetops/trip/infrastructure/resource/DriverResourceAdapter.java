package com.aalsaeed.fleetops.trip.infrastructure.resource;

import com.aalsaeed.fleetops.driver.application.port.out.DriverRepository;
import com.aalsaeed.fleetops.driver.domain.DriverId;
import com.aalsaeed.fleetops.driver.domain.DriverStatus;
import com.aalsaeed.fleetops.trip.application.port.out.DriverResource;
import com.aalsaeed.fleetops.trip.application.port.out.DriverResourcePort;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class DriverResourceAdapter implements DriverResourcePort {

    private final DriverRepository driverRepository;

    public DriverResourceAdapter(DriverRepository driverRepository) {
        this.driverRepository = Objects.requireNonNull(driverRepository, "Driver repository cannot be null");
    }

    @Override
    public Optional<DriverResource> findById(UUID id) {
        Objects.requireNonNull(id, "Driver ID cannot be null");
        return driverRepository.findById(DriverId.of(id))
                .map(driver -> new DriverResource(
                        driver.id().value(),
                        driver.status() == DriverStatus.ACTIVE));
    }
}
