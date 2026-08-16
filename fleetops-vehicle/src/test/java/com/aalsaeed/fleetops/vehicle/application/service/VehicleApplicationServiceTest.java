package com.aalsaeed.fleetops.vehicle.application.service;

import com.aalsaeed.fleetops.vehicle.application.exception.VehicleAlreadyExistsException;
import com.aalsaeed.fleetops.vehicle.application.exception.VehicleNotFoundException;
import com.aalsaeed.fleetops.vehicle.application.port.in.CreateVehicleCommand;
import com.aalsaeed.fleetops.vehicle.application.port.out.VehicleRepository;
import com.aalsaeed.fleetops.vehicle.domain.Vehicle;
import com.aalsaeed.fleetops.vehicle.domain.VehicleId;
import com.aalsaeed.fleetops.vehicle.domain.VehicleStatus;
import com.aalsaeed.fleetops.vehicle.domain.VehicleType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VehicleApplicationServiceTest {

    @Test
    void createsVehicleThroughRepositoryPort() {
        InMemoryVehicleRepository repository = new InMemoryVehicleRepository();
        VehicleApplicationService service = new VehicleApplicationService(repository);

        Vehicle vehicle = service.createVehicle(new CreateVehicleCommand(
                "  VEH-1001  ",
                "  Tractor 1001  ",
                VehicleType.TRACTOR,
                "  SN-1001  "));

        assertEquals("VEH-1001", vehicle.externalReference());
        assertEquals("Tractor 1001", vehicle.description());
        assertEquals("SN-1001", vehicle.serialNumber());
        assertEquals(VehicleStatus.ACTIVE, vehicle.status());
        assertEquals(vehicle, repository.findById(vehicle.id()).orElseThrow());
    }

    @Test
    void rejectsDuplicateExternalReference() {
        InMemoryVehicleRepository repository = new InMemoryVehicleRepository();
        VehicleApplicationService service = new VehicleApplicationService(repository);

        service.createVehicle(new CreateVehicleCommand(
                "VEH-1001", "Tractor 1001", VehicleType.TRACTOR, null));

        assertThrows(
                VehicleAlreadyExistsException.class,
                () -> service.createVehicle(new CreateVehicleCommand(
                        "VEH-1001", "Another Tractor", VehicleType.TRACTOR, "SN-2001")));
    }

    @Test
    void getsVehicleByNormalizedExternalReference() {
        InMemoryVehicleRepository repository = new InMemoryVehicleRepository();
        VehicleApplicationService service = new VehicleApplicationService(repository);
        Vehicle created = service.createVehicle(new CreateVehicleCommand(
                "VEH-1001", "Tractor 1001", VehicleType.TRACTOR, null));

        Vehicle found = service.getByExternalReference("  VEH-1001  ");

        assertEquals(created.id(), found.id());
    }

    @Test
    void missingVehicleRaisesApplicationException() {
        VehicleApplicationService service = new VehicleApplicationService(new InMemoryVehicleRepository());

        assertThrows(VehicleNotFoundException.class, () -> service.getById(VehicleId.newId()));
    }

    @Test
    void changesStatusAndPersistsUpdatedAggregate() {
        InMemoryVehicleRepository repository = new InMemoryVehicleRepository();
        VehicleApplicationService service = new VehicleApplicationService(repository);
        Vehicle created = service.createVehicle(new CreateVehicleCommand(
                "VEH-1001", "Tractor 1001", VehicleType.TRACTOR, null));

        Vehicle updated = service.changeStatus(created.id(), VehicleStatus.MAINTENANCE);

        assertEquals(VehicleStatus.MAINTENANCE, updated.status());
        assertEquals(
                VehicleStatus.MAINTENANCE,
                repository.findById(created.id()).orElseThrow().status());
    }

    private static final class InMemoryVehicleRepository implements VehicleRepository {

        private final Map<VehicleId, Vehicle> vehicles = new HashMap<>();

        @Override
        public Vehicle save(Vehicle vehicle) {
            vehicles.put(vehicle.id(), vehicle);
            return vehicle;
        }

        @Override
        public Optional<Vehicle> findById(VehicleId id) {
            return Optional.ofNullable(vehicles.get(id));
        }

        @Override
        public Optional<Vehicle> findByExternalReference(String externalReference) {
            return vehicles.values().stream()
                    .filter(vehicle -> vehicle.externalReference().equals(externalReference))
                    .findFirst();
        }

        @Override
        public boolean existsByExternalReference(String externalReference) {
            return findByExternalReference(externalReference).isPresent();
        }
    }
}
