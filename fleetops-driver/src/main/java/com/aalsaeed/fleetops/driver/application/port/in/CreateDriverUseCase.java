package com.aalsaeed.fleetops.driver.application.port.in;

import com.aalsaeed.fleetops.driver.domain.Driver;

public interface CreateDriverUseCase {

    Driver createDriver(CreateDriverCommand command);
}
