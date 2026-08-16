package com.aalsaeed.fleetops.vehicle.domain;

public enum VehicleStatus {
    ACTIVE,
    INACTIVE,
    MAINTENANCE,
    RETIRED;

    public boolean canTransitionTo(VehicleStatus target) {
        if (target == null) {
            return false;
        }
        if (this == target) {
            return true;
        }

        return switch (this) {
            case ACTIVE -> target == INACTIVE || target == MAINTENANCE || target == RETIRED;
            case INACTIVE -> target == ACTIVE || target == MAINTENANCE || target == RETIRED;
            case MAINTENANCE -> target == ACTIVE || target == INACTIVE || target == RETIRED;
            case RETIRED -> false;
        };
    }
}
