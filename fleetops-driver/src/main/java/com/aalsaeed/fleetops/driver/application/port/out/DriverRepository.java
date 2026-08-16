package com.aalsaeed.fleetops.driver.application.port.out;

import com.aalsaeed.fleetops.driver.domain.Driver;
import com.aalsaeed.fleetops.driver.domain.DriverId;

import java.util.Optional;

public interface DriverRepository {

    Driver save(Driver driver);

    Optional<Driver> findById(DriverId id);

    Optional<Driver> findByExternalReference(String externalReference);

    boolean existsByExternalReference(String externalReference);
}
