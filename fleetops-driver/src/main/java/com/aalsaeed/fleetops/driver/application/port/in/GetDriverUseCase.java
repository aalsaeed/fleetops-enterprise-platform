package com.aalsaeed.fleetops.driver.application.port.in;

import com.aalsaeed.fleetops.driver.domain.Driver;
import com.aalsaeed.fleetops.driver.domain.DriverId;

public interface GetDriverUseCase {

    Driver getById(DriverId id);

    Driver getByExternalReference(String externalReference);
}
