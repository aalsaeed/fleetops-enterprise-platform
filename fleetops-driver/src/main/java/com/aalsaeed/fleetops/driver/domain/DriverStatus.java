package com.aalsaeed.fleetops.driver.domain;

public enum DriverStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED;

    public boolean canTransitionTo(DriverStatus target) {
        if (target == null) {
            return false;
        }
        if (this == target) {
            return true;
        }

        return switch (this) {
            case ACTIVE -> target == INACTIVE || target == SUSPENDED;
            case INACTIVE -> target == ACTIVE;
            case SUSPENDED -> target == ACTIVE || target == INACTIVE;
        };
    }
}
