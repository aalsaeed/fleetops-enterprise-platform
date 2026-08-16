package com.aalsaeed.fleetops.vehicle.domain;

import java.util.Objects;

public final class Vehicle {

    private final VehicleId id;
    private final String externalReference;
    private final String description;
    private final VehicleType type;
    private final String serialNumber;
    private VehicleStatus status;

    private Vehicle(
            VehicleId id,
            String externalReference,
            String description,
            VehicleType type,
            String serialNumber,
            VehicleStatus status) {
        this.id = Objects.requireNonNull(id, "Vehicle ID cannot be null");
        this.externalReference = requireText(externalReference, "External reference");
        this.description = requireText(description, "Description");
        this.type = Objects.requireNonNull(type, "Vehicle type cannot be null");
        this.serialNumber = normalizeOptionalText(serialNumber);
        this.status = Objects.requireNonNull(status, "Vehicle status cannot be null");
    }

    public static Vehicle create(
            String externalReference,
            String description,
            VehicleType type,
            String serialNumber) {
        return new Vehicle(
                VehicleId.newId(),
                externalReference,
                description,
                type,
                serialNumber,
                VehicleStatus.ACTIVE);
    }

    public static Vehicle restore(
            VehicleId id,
            String externalReference,
            String description,
            VehicleType type,
            String serialNumber,
            VehicleStatus status) {
        return new Vehicle(id, externalReference, description, type, serialNumber, status);
    }

    public void changeStatus(VehicleStatus target) {
        Objects.requireNonNull(target, "Target vehicle status cannot be null");
        if (!status.canTransitionTo(target)) {
            throw new InvalidVehicleStatusTransitionException(status, target);
        }
        status = target;
    }

    public VehicleId id() {
        return id;
    }

    public String externalReference() {
        return externalReference;
    }

    public String description() {
        return description;
    }

    public VehicleType type() {
        return type;
    }

    public String serialNumber() {
        return serialNumber;
    }

    public VehicleStatus status() {
        return status;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
