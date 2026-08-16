package com.aalsaeed.fleetops.trip.api.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTripRequest(
        @NotBlank @Size(max = 100) String externalReference) {
}
