package com.aalsaeed.fleetops.driver.application.port.in;

public record CreateDriverCommand(
        String externalReference,
        String firstName,
        String lastName,
        String phoneNumber) {
}
