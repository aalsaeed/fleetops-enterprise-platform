package com.aalsaeed.fleetops.vehicle.application.port.in;

import com.aalsaeed.fleetops.vehicle.domain.Vehicle;
import com.aalsaeed.fleetops.vehicle.domain.VehicleId;

public interface GetVehicleUseCase {

    Vehicle getById(VehicleId id);

    Vehicle getByExternalReference(String externalReference);
}
