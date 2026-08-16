package com.aalsaeed.fleetops.trip.application.service;

import com.aalsaeed.fleetops.trip.application.exception.InvalidTripResourceRoleException;
import com.aalsaeed.fleetops.trip.application.exception.TripAlreadyExistsException;
import com.aalsaeed.fleetops.trip.application.exception.TripNotFoundException;
import com.aalsaeed.fleetops.trip.application.exception.TripResourceNotFoundException;
import com.aalsaeed.fleetops.trip.application.exception.TripResourceUnavailableException;
import com.aalsaeed.fleetops.trip.application.port.in.AssignTripResourcesCommand;
import com.aalsaeed.fleetops.trip.application.port.in.CreateTripCommand;
import com.aalsaeed.fleetops.trip.application.port.out.DriverResource;
import com.aalsaeed.fleetops.trip.application.port.out.DriverResourcePort;
import com.aalsaeed.fleetops.trip.application.port.out.TripRepository;
import com.aalsaeed.fleetops.trip.application.port.out.VehicleResource;
import com.aalsaeed.fleetops.trip.application.port.out.VehicleResourcePort;
import com.aalsaeed.fleetops.trip.application.port.out.VehicleResourceType;
import com.aalsaeed.fleetops.trip.domain.Trip;
import com.aalsaeed.fleetops.trip.domain.TripId;
import com.aalsaeed.fleetops.trip.domain.TripStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TripApplicationServiceTest {

    private static final UUID DRIVER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TRACTOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TRAILER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BULKER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private InMemoryTripRepository tripRepository;
    private InMemoryDriverResourcePort driverResourcePort;
    private InMemoryVehicleResourcePort vehicleResourcePort;
    private TripApplicationService service;

    @BeforeEach
    void setUp() {
        tripRepository = new InMemoryTripRepository();
        driverResourcePort = new InMemoryDriverResourcePort();
        vehicleResourcePort = new InMemoryVehicleResourcePort();
        service = new TripApplicationService(tripRepository, driverResourcePort, vehicleResourcePort);

        driverResourcePort.put(new DriverResource(DRIVER_ID, true));
        vehicleResourcePort.put(new VehicleResource(TRACTOR_ID, VehicleResourceType.TRACTOR, true));
        vehicleResourcePort.put(new VehicleResource(TRAILER_ID, VehicleResourceType.TRAILER, true));
        vehicleResourcePort.put(new VehicleResource(BULKER_ID, VehicleResourceType.BULKER, true));
    }

    @Test
    void createsPlannedTripWithNormalizedExternalReference() {
        Trip trip = service.createTrip(new CreateTripCommand("  TRIP-1001  "));

        assertEquals("TRIP-1001", trip.externalReference());
        assertEquals(TripStatus.PLANNED, trip.status());
        assertNull(trip.driverReference());
        assertEquals(trip.id(), service.getByExternalReference(" TRIP-1001 ").id());
    }

    @Test
    void rejectsDuplicateExternalReference() {
        service.createTrip(new CreateTripCommand("TRIP-DUP"));

        assertThrows(
                TripAlreadyExistsException.class,
                () -> service.createTrip(new CreateTripCommand(" TRIP-DUP ")));
    }

    @Test
    void missingTripRaisesNotFound() {
        TripId missing = TripId.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

        assertThrows(TripNotFoundException.class, () -> service.getById(missing));
    }

    @Test
    void assignsOperationalDriverTractorAndTrailer() {
        Trip trip = service.createTrip(new CreateTripCommand("TRIP-ASSIGN"));

        Trip assigned = service.assignResources(new AssignTripResourcesCommand(
                trip.id(), DRIVER_ID, TRACTOR_ID, TRAILER_ID));

        assertEquals(TripStatus.ASSIGNED, assigned.status());
        assertEquals(DRIVER_ID, assigned.driverReference().value());
        assertEquals(TRACTOR_ID, assigned.primaryVehicleReference().value());
        assertEquals(TRAILER_ID, assigned.attachmentVehicleReference().value());
    }

    @Test
    void supportsAssignmentWithoutAttachment() {
        Trip trip = service.createTrip(new CreateTripCommand("TRIP-NO-ATTACHMENT"));

        Trip assigned = service.assignResources(new AssignTripResourcesCommand(
                trip.id(), DRIVER_ID, TRACTOR_ID, null));

        assertEquals(TripStatus.ASSIGNED, assigned.status());
        assertNull(assigned.attachmentVehicleReference());
    }

    @Test
    void rejectsMissingDriverResource() {
        Trip trip = service.createTrip(new CreateTripCommand("TRIP-MISSING-DRIVER"));
        UUID missingDriver = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        assertThrows(
                TripResourceNotFoundException.class,
                () -> service.assignResources(new AssignTripResourcesCommand(
                        trip.id(), missingDriver, TRACTOR_ID, null)));
    }

    @Test
    void rejectsUnavailableDriverResource() {
        Trip trip = service.createTrip(new CreateTripCommand("TRIP-INACTIVE-DRIVER"));
        driverResourcePort.put(new DriverResource(DRIVER_ID, false));

        assertThrows(
                TripResourceUnavailableException.class,
                () -> service.assignResources(new AssignTripResourcesCommand(
                        trip.id(), DRIVER_ID, TRACTOR_ID, null)));
    }

    @Test
    void rejectsNonTractorAsPrimaryVehicle() {
        Trip trip = service.createTrip(new CreateTripCommand("TRIP-WRONG-PRIMARY"));

        assertThrows(
                InvalidTripResourceRoleException.class,
                () -> service.assignResources(new AssignTripResourcesCommand(
                        trip.id(), DRIVER_ID, TRAILER_ID, null)));
    }

    @Test
    void rejectsTractorAsAttachmentVehicle() {
        Trip trip = service.createTrip(new CreateTripCommand("TRIP-WRONG-ATTACHMENT"));
        UUID secondTractor = UUID.fromString("55555555-5555-5555-5555-555555555555");
        vehicleResourcePort.put(new VehicleResource(secondTractor, VehicleResourceType.TRACTOR, true));

        assertThrows(
                InvalidTripResourceRoleException.class,
                () -> service.assignResources(new AssignTripResourcesCommand(
                        trip.id(), DRIVER_ID, TRACTOR_ID, secondTractor)));
    }

    @Test
    void acceptsBulkerAsAttachmentVehicle() {
        Trip trip = service.createTrip(new CreateTripCommand("TRIP-BULKER"));

        Trip assigned = service.assignResources(new AssignTripResourcesCommand(
                trip.id(), DRIVER_ID, TRACTOR_ID, BULKER_ID));

        assertEquals(BULKER_ID, assigned.attachmentVehicleReference().value());
    }

    @Test
    void startRevalidatesPreviouslyAssignedResources() {
        Trip trip = assignedTrip("TRIP-REVALIDATE");
        driverResourcePort.put(new DriverResource(DRIVER_ID, false));

        assertThrows(TripResourceUnavailableException.class, () -> service.startTrip(trip.id()));
        assertEquals(TripStatus.ASSIGNED, service.getById(trip.id()).status());
    }

    @Test
    void executesHappyPathFromAssignmentToCompletion() {
        Trip trip = assignedTrip("TRIP-LIFECYCLE");

        assertEquals(TripStatus.IN_PROGRESS, service.startTrip(trip.id()).status());
        assertEquals(TripStatus.COMPLETED, service.completeTrip(trip.id()).status());
    }

    @Test
    void cancelsPlannedTrip() {
        Trip trip = service.createTrip(new CreateTripCommand("TRIP-CANCEL"));

        assertEquals(TripStatus.CANCELLED, service.cancelTrip(trip.id()).status());
    }

    private Trip assignedTrip(String externalReference) {
        Trip trip = service.createTrip(new CreateTripCommand(externalReference));
        return service.assignResources(new AssignTripResourcesCommand(
                trip.id(), DRIVER_ID, TRACTOR_ID, TRAILER_ID));
    }

    private static final class InMemoryTripRepository implements TripRepository {
        private final Map<TripId, Trip> byId = new HashMap<>();
        private final Map<String, TripId> byExternalReference = new HashMap<>();

        @Override
        public Trip save(Trip trip) {
            byId.put(trip.id(), trip);
            byExternalReference.put(trip.externalReference(), trip.id());
            return trip;
        }

        @Override
        public Optional<Trip> findById(TripId id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<Trip> findByExternalReference(String externalReference) {
            TripId id = byExternalReference.get(externalReference.trim());
            return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
        }

        @Override
        public boolean existsByExternalReference(String externalReference) {
            return byExternalReference.containsKey(externalReference.trim());
        }
    }

    private static final class InMemoryDriverResourcePort implements DriverResourcePort {
        private final Map<UUID, DriverResource> resources = new HashMap<>();

        void put(DriverResource resource) {
            resources.put(resource.id(), resource);
        }

        @Override
        public Optional<DriverResource> findById(UUID id) {
            return Optional.ofNullable(resources.get(id));
        }
    }

    private static final class InMemoryVehicleResourcePort implements VehicleResourcePort {
        private final Map<UUID, VehicleResource> resources = new HashMap<>();

        void put(VehicleResource resource) {
            resources.put(resource.id(), resource);
        }

        @Override
        public Optional<VehicleResource> findById(UUID id) {
            return Optional.ofNullable(resources.get(id));
        }
    }
}
