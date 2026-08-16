package com.aalsaeed.fleetops.trip.application.service;

import com.aalsaeed.fleetops.trip.application.exception.InvalidTripResourceRoleException;
import com.aalsaeed.fleetops.trip.application.exception.TripAlreadyExistsException;
import com.aalsaeed.fleetops.trip.application.exception.TripNotFoundException;
import com.aalsaeed.fleetops.trip.application.exception.TripResourceNotFoundException;
import com.aalsaeed.fleetops.trip.application.exception.TripResourceUnavailableException;
import com.aalsaeed.fleetops.trip.application.port.in.AssignTripResourcesCommand;
import com.aalsaeed.fleetops.trip.application.port.in.AssignTripResourcesUseCase;
import com.aalsaeed.fleetops.trip.application.port.in.CreateTripCommand;
import com.aalsaeed.fleetops.trip.application.port.in.CreateTripUseCase;
import com.aalsaeed.fleetops.trip.application.port.in.GetTripUseCase;
import com.aalsaeed.fleetops.trip.application.port.in.TripLifecycleUseCase;
import com.aalsaeed.fleetops.trip.application.port.out.DriverResource;
import com.aalsaeed.fleetops.trip.application.port.out.DriverResourcePort;
import com.aalsaeed.fleetops.trip.application.port.out.TripRepository;
import com.aalsaeed.fleetops.trip.application.port.out.VehicleResource;
import com.aalsaeed.fleetops.trip.application.port.out.VehicleResourcePort;
import com.aalsaeed.fleetops.trip.application.port.out.VehicleResourceType;
import com.aalsaeed.fleetops.trip.domain.DriverReference;
import com.aalsaeed.fleetops.trip.domain.Trip;
import com.aalsaeed.fleetops.trip.domain.TripId;
import com.aalsaeed.fleetops.trip.domain.TripStatus;
import com.aalsaeed.fleetops.trip.domain.VehicleReference;

import java.util.Objects;
import java.util.UUID;

public final class TripApplicationService implements
        CreateTripUseCase,
        GetTripUseCase,
        AssignTripResourcesUseCase,
        TripLifecycleUseCase {

    private final TripRepository tripRepository;
    private final DriverResourcePort driverResourcePort;
    private final VehicleResourcePort vehicleResourcePort;

    public TripApplicationService(
            TripRepository tripRepository,
            DriverResourcePort driverResourcePort,
            VehicleResourcePort vehicleResourcePort) {
        this.tripRepository = Objects.requireNonNull(tripRepository, "Trip repository cannot be null");
        this.driverResourcePort = Objects.requireNonNull(driverResourcePort, "Driver resource port cannot be null");
        this.vehicleResourcePort = Objects.requireNonNull(vehicleResourcePort, "Vehicle resource port cannot be null");
    }

    @Override
    public Trip createTrip(CreateTripCommand command) {
        Objects.requireNonNull(command, "Create trip command cannot be null");

        Trip trip = Trip.create(command.externalReference());
        if (tripRepository.existsByExternalReference(trip.externalReference())) {
            throw new TripAlreadyExistsException(trip.externalReference());
        }

        return tripRepository.save(trip);
    }

    @Override
    public Trip getById(TripId id) {
        Objects.requireNonNull(id, "Trip ID cannot be null");
        return tripRepository.findById(id)
                .orElseThrow(() -> new TripNotFoundException(id.value().toString()));
    }

    @Override
    public Trip getByExternalReference(String externalReference) {
        String normalizedReference = requireText(externalReference, "External reference");
        return tripRepository.findByExternalReference(normalizedReference)
                .orElseThrow(() -> new TripNotFoundException(normalizedReference));
    }

    @Override
    public Trip assignResources(AssignTripResourcesCommand command) {
        Objects.requireNonNull(command, "Assign resources command cannot be null");
        Objects.requireNonNull(command.tripId(), "Trip ID cannot be null");
        UUID driverId = Objects.requireNonNull(command.driverId(), "Driver ID cannot be null");
        UUID primaryVehicleId = Objects.requireNonNull(command.primaryVehicleId(), "Primary vehicle ID cannot be null");

        Trip trip = getById(command.tripId());

        requireOperationalDriver(driverId);
        requirePrimaryVehicle(primaryVehicleId);
        if (command.attachmentVehicleId() != null) {
            requireAttachmentVehicle(command.attachmentVehicleId());
        }

        trip.assignResources(
                DriverReference.of(driverId),
                VehicleReference.of(primaryVehicleId),
                command.attachmentVehicleId() == null
                        ? null
                        : VehicleReference.of(command.attachmentVehicleId()));

        return tripRepository.save(trip);
    }

    @Override
    public Trip startTrip(TripId id) {
        Trip trip = getById(id);

        if (trip.status() == TripStatus.ASSIGNED) {
            validateCurrentAssignment(trip);
        }

        trip.start();
        return tripRepository.save(trip);
    }

    @Override
    public Trip completeTrip(TripId id) {
        Trip trip = getById(id);
        trip.complete();
        return tripRepository.save(trip);
    }

    @Override
    public Trip cancelTrip(TripId id) {
        Trip trip = getById(id);
        trip.cancel();
        return tripRepository.save(trip);
    }

    private void validateCurrentAssignment(Trip trip) {
        requireOperationalDriver(trip.driverReference().value());
        requirePrimaryVehicle(trip.primaryVehicleReference().value());
        if (trip.attachmentVehicleReference() != null) {
            requireAttachmentVehicle(trip.attachmentVehicleReference().value());
        }
    }

    private DriverResource requireOperationalDriver(UUID id) {
        DriverResource resource = driverResourcePort.findById(id)
                .orElseThrow(() -> new TripResourceNotFoundException("Driver", id));
        if (!resource.operational()) {
            throw new TripResourceUnavailableException("Driver", id);
        }
        return resource;
    }

    private VehicleResource requirePrimaryVehicle(UUID id) {
        VehicleResource resource = requireOperationalVehicle(id);
        if (resource.type() != VehicleResourceType.TRACTOR) {
            throw new InvalidTripResourceRoleException(
                    "Primary vehicle must be a TRACTOR: " + id + " is " + resource.type());
        }
        return resource;
    }

    private VehicleResource requireAttachmentVehicle(UUID id) {
        VehicleResource resource = requireOperationalVehicle(id);
        if (resource.type() != VehicleResourceType.TRAILER
                && resource.type() != VehicleResourceType.BULKER) {
            throw new InvalidTripResourceRoleException(
                    "Attachment vehicle must be a TRAILER or BULKER: " + id + " is " + resource.type());
        }
        return resource;
    }

    private VehicleResource requireOperationalVehicle(UUID id) {
        VehicleResource resource = vehicleResourcePort.findById(id)
                .orElseThrow(() -> new TripResourceNotFoundException("Vehicle", id));
        if (!resource.operational()) {
            throw new TripResourceUnavailableException("Vehicle", id);
        }
        return resource;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }
}
