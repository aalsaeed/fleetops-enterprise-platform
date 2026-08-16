package com.aalsaeed.fleetops.trip.domain;

public enum TripStatus {
    PLANNED,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(TripStatus target) {
        if (target == null || this == target) {
            return false;
        }

        return switch (this) {
            case PLANNED -> target == ASSIGNED || target == CANCELLED;
            case ASSIGNED -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == COMPLETED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
