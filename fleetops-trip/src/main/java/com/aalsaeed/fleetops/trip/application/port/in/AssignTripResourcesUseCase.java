package com.aalsaeed.fleetops.trip.application.port.in;

import com.aalsaeed.fleetops.trip.domain.Trip;

public interface AssignTripResourcesUseCase {

    Trip assignResources(AssignTripResourcesCommand command);
}
