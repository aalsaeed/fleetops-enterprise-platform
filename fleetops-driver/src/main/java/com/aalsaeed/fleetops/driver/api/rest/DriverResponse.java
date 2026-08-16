package com.aalsaeed.fleetops.driver.api.rest;

import com.aalsaeed.fleetops.driver.domain.Driver;
import com.aalsaeed.fleetops.driver.domain.DriverStatus;

import java.util.UUID;

public record DriverResponse(
        UUID id,
        String externalReference,
        String firstName,
        String lastName,
        String phoneNumber,
        DriverStatus status) {

    static DriverResponse from(Driver driver) {
        return new DriverResponse(
                driver.id().value(),
                driver.externalReference(),
                driver.firstName(),
                driver.lastName(),
                driver.phoneNumber(),
                driver.status());
    }
}
