package com.aalsaeed.fleetops.vehicle.application.port.in;

import com.aalsaeed.fleetops.vehicle.domain.Vehicle;
import com.aalsaeed.fleetops.vehicle.domain.VehicleId;
import com.aalsaeed.fleetops.vehicle.domain.VehicleStatus;

public interface ChangeVehicleStatusUseCase {

    Vehicle changeStatus(VehicleId id, VehicleStatus targetStatus);
}
