package com.aalsaeed.fleetops.trip.domain;

import java.util.Objects;

public final class Trip {

    private final TripId id;
    private final String externalReference;
    private DriverReference driverReference;
    private VehicleReference primaryVehicleReference;
    private VehicleReference attachmentVehicleReference;
    private TripStatus status;
    private final Long revision;

    private Trip(
            TripId id,
            String externalReference,
            DriverReference driverReference,
            VehicleReference primaryVehicleReference,
            VehicleReference attachmentVehicleReference,
            TripStatus status,
            Long revision) {
        this.id = Objects.requireNonNull(id, "Trip ID cannot be null");
        this.externalReference = requireText(externalReference, "External reference");
        this.driverReference = driverReference;
        this.primaryVehicleReference = primaryVehicleReference;
        this.attachmentVehicleReference = attachmentVehicleReference;
        this.status = Objects.requireNonNull(status, "Trip status cannot be null");
        this.revision = requireRevision(revision);
        validateState();
    }

    public static Trip create(String externalReference) {
        return new Trip(
                TripId.newId(),
                externalReference,
                null,
                null,
                null,
                TripStatus.PLANNED,
                null);
    }

    public static Trip restore(
            TripId id,
            String externalReference,
            DriverReference driverReference,
            VehicleReference primaryVehicleReference,
            VehicleReference attachmentVehicleReference,
            TripStatus status) {
        return restore(
                id,
                externalReference,
                driverReference,
                primaryVehicleReference,
                attachmentVehicleReference,
                status,
                null);
    }

    public static Trip restore(
            TripId id,
            String externalReference,
            DriverReference driverReference,
            VehicleReference primaryVehicleReference,
            VehicleReference attachmentVehicleReference,
            TripStatus status,
            Long revision) {
        return new Trip(
                id,
                externalReference,
                driverReference,
                primaryVehicleReference,
                attachmentVehicleReference,
                status,
                revision);
    }

    public void assignResources(
            DriverReference driverReference,
            VehicleReference primaryVehicleReference,
            VehicleReference attachmentVehicleReference) {
        if (status != TripStatus.PLANNED && status != TripStatus.ASSIGNED) {
            throw new InvalidTripAssignmentException(
                    "Trip resources can only be assigned while the trip is PLANNED or ASSIGNED");
        }

        DriverReference driver = Objects.requireNonNull(driverReference, "Driver reference cannot be null");
        VehicleReference primaryVehicle = Objects.requireNonNull(
                primaryVehicleReference,
                "Primary vehicle reference cannot be null");
        validateDistinctVehicles(primaryVehicle, attachmentVehicleReference);

        this.driverReference = driver;
        this.primaryVehicleReference = primaryVehicle;
        this.attachmentVehicleReference = attachmentVehicleReference;

        if (status == TripStatus.PLANNED) {
            transitionTo(TripStatus.ASSIGNED);
        }
    }

    public void start() {
        transitionTo(TripStatus.IN_PROGRESS);
    }

    public void complete() {
        transitionTo(TripStatus.COMPLETED);
    }

    public void cancel() {
        transitionTo(TripStatus.CANCELLED);
    }

    public TripId id() {
        return id;
    }

    public String externalReference() {
        return externalReference;
    }

    public DriverReference driverReference() {
        return driverReference;
    }

    public VehicleReference primaryVehicleReference() {
        return primaryVehicleReference;
    }

    public VehicleReference attachmentVehicleReference() {
        return attachmentVehicleReference;
    }

    public TripStatus status() {
        return status;
    }

    public Long revision() {
        return revision;
    }

    private void transitionTo(TripStatus target) {
        Objects.requireNonNull(target, "Target trip status cannot be null");
        if (!status.canTransitionTo(target)) {
            throw new InvalidTripStatusTransitionException(status, target);
        }
        status = target;
    }

    private void validateState() {
        validateDistinctVehicles(primaryVehicleReference, attachmentVehicleReference);

        if (status == TripStatus.PLANNED
                && (driverReference != null || primaryVehicleReference != null || attachmentVehicleReference != null)) {
            throw new InvalidTripAssignmentException("A PLANNED trip cannot already contain an assignment");
        }

        if ((status == TripStatus.ASSIGNED
                || status == TripStatus.IN_PROGRESS
                || status == TripStatus.COMPLETED)
                && (driverReference == null || primaryVehicleReference == null)) {
            throw new InvalidTripAssignmentException(
                    "Trip status " + status + " requires both driver and primary vehicle assignments");
        }
    }

    private static void validateDistinctVehicles(
            VehicleReference primaryVehicleReference,
            VehicleReference attachmentVehicleReference) {
        if (primaryVehicleReference != null
                && primaryVehicleReference.equals(attachmentVehicleReference)) {
            throw new InvalidTripAssignmentException(
                    "Primary vehicle and attachment vehicle must be different resources");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    private static Long requireRevision(Long revision) {
        if (revision != null && revision < 0) {
            throw new IllegalArgumentException("Trip revision cannot be negative");
        }
        return revision;
    }
}
