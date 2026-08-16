package com.aalsaeed.fleetops.driver.api.rest;

import com.aalsaeed.fleetops.driver.domain.DriverStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeDriverStatusRequest(
        @NotNull DriverStatus status) {
}
