package com.aalsaeed.fleetops.vehicle.application.port.in;

import com.aalsaeed.fleetops.vehicle.domain.Vehicle;

public interface CreateVehicleUseCase {

    Vehicle createVehicle(CreateVehicleCommand command);
}
