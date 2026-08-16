package com.aalsaeed.fleetops.driver.application.port.in;

import com.aalsaeed.fleetops.driver.domain.Driver;
import com.aalsaeed.fleetops.driver.domain.DriverId;
import com.aalsaeed.fleetops.driver.domain.DriverStatus;

public interface ChangeDriverStatusUseCase {

    Driver changeStatus(DriverId id, DriverStatus targetStatus);
}
