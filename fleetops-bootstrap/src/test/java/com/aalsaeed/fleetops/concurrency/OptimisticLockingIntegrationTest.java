package com.aalsaeed.fleetops.concurrency;

import com.aalsaeed.fleetops.common.concurrency.OptimisticConcurrencyConflictException;
import com.aalsaeed.fleetops.driver.application.port.out.DriverRepository;
import com.aalsaeed.fleetops.driver.domain.Driver;
import com.aalsaeed.fleetops.driver.domain.DriverStatus;
import com.aalsaeed.fleetops.trip.application.port.out.TripRepository;
import com.aalsaeed.fleetops.trip.domain.DriverReference;
import com.aalsaeed.fleetops.trip.domain.Trip;
import com.aalsaeed.fleetops.trip.domain.TripStatus;
import com.aalsaeed.fleetops.trip.domain.VehicleReference;
import com.aalsaeed.fleetops.vehicle.application.port.out.VehicleRepository;
import com.aalsaeed.fleetops.vehicle.domain.Vehicle;
import com.aalsaeed.fleetops.vehicle.domain.VehicleStatus;
import com.aalsaeed.fleetops.vehicle.domain.VehicleType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.OptimisticLockingFailureException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgresSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class OptimisticLockingIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgresSQLContainer POSTGRES = new PostgresSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_concurrency_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TripRepository tripRepository;

    @Test
    void rejectsStaleDriverStatusUpdate() {
        Driver created = driverRepository.save(Driver.create(
                "DRV-LOCK-1001",
                "Concurrency",
                "Driver",
                "+15550001001"));

        assertThat(created.revision()).isZero();

        Driver firstWriter = driverRepository.findById(created.id()).orElseThrow();
        Driver staleWriter = driverRepository.findById(created.id()).orElseThrow();
        assertThat(firstWriter.revision()).isEqualTo(staleWriter.revision());

        firstWriter.changeStatus(DriverStatus.SUSPENDED);
        Driver committed = driverRepository.save(firstWriter);
        assertThat(committed.revision()).isEqualTo(1L);

        staleWriter.changeStatus(DriverStatus.INACTIVE);
        assertThatThrownBy(() -> driverRepository.save(staleWriter))
                .isInstanceOfSatisfying(OptimisticConcurrencyConflictException.class, conflict -> {
                    assertThat(conflict.resourceType()).isEqualTo("Driver");
                    assertThat(conflict.resourceId()).isEqualTo(created.id().value().toString());
                    assertThat(conflict.getCause()).isInstanceOf(OptimisticLockingFailureException.class);
                });

        Driver restored = driverRepository.findById(created.id()).orElseThrow();
        assertThat(restored.status()).isEqualTo(DriverStatus.SUSPENDED);
        assertThat(restored.revision()).isEqualTo(1L);
    }

    @Test
    void rejectsStaleVehicleStatusUpdate() {
        Vehicle created = vehicleRepository.save(Vehicle.create(
                "VEH-LOCK-1001",
                "Concurrency tractor",
                VehicleType.TRACTOR,
                "LOCK-SN-1001"));

        assertThat(created.revision()).isZero();

        Vehicle firstWriter = vehicleRepository.findById(created.id()).orElseThrow();
        Vehicle staleWriter = vehicleRepository.findById(created.id()).orElseThrow();
        assertThat(firstWriter.revision()).isEqualTo(staleWriter.revision());

        firstWriter.changeStatus(VehicleStatus.MAINTENANCE);
        Vehicle committed = vehicleRepository.save(firstWriter);
        assertThat(committed.revision()).isEqualTo(1L);

        staleWriter.changeStatus(VehicleStatus.INACTIVE);
        assertThatThrownBy(() -> vehicleRepository.save(staleWriter))
                .isInstanceOfSatisfying(OptimisticConcurrencyConflictException.class, conflict -> {
                    assertThat(conflict.resourceType()).isEqualTo("Vehicle");
                    assertThat(conflict.resourceId()).isEqualTo(created.id().value().toString());
                    assertThat(conflict.getCause()).isInstanceOf(OptimisticLockingFailureException.class);
                });

        Vehicle restored = vehicleRepository.findById(created.id()).orElseThrow();
        assertThat(restored.status()).isEqualTo(VehicleStatus.MAINTENANCE);
        assertThat(restored.revision()).isEqualTo(1L);
    }

    @Test
    void rejectsStaleTripAssignmentUpdate() {
        Trip created = tripRepository.save(Trip.create("TRIP-LOCK-1001"));
        assertThat(created.revision()).isZero();

        Trip firstWriter = tripRepository.findById(created.id()).orElseThrow();
        Trip staleWriter = tripRepository.findById(created.id()).orElseThrow();
        assertThat(firstWriter.revision()).isEqualTo(staleWriter.revision());

        UUID committedDriverId = UUID.fromString("11111111-aaaa-4444-8888-111111111111");
        UUID committedVehicleId = UUID.fromString("22222222-bbbb-4444-8888-222222222222");
        firstWriter.assignResources(
                DriverReference.of(committedDriverId),
                VehicleReference.of(committedVehicleId),
                null);
        Trip committed = tripRepository.save(firstWriter);
        assertThat(committed.status()).isEqualTo(TripStatus.ASSIGNED);
        assertThat(committed.revision()).isEqualTo(1L);

        staleWriter.assignResources(
                DriverReference.of(UUID.fromString("33333333-cccc-4444-8888-333333333333")),
                VehicleReference.of(UUID.fromString("44444444-dddd-4444-8888-444444444444")),
                null);
        assertThatThrownBy(() -> tripRepository.save(staleWriter))
                .isInstanceOfSatisfying(OptimisticConcurrencyConflictException.class, conflict -> {
                    assertThat(conflict.resourceType()).isEqualTo("Trip");
                    assertThat(conflict.resourceId()).isEqualTo(created.id().value().toString());
                    assertThat(conflict.getCause()).isInstanceOf(OptimisticLockingFailureException.class);
                });

        Trip restored = tripRepository.findById(created.id()).orElseThrow();
        assertThat(restored.status()).isEqualTo(TripStatus.ASSIGNED);
        assertThat(restored.driverReference().value()).isEqualTo(committedDriverId);
        assertThat(restored.primaryVehicleReference().value()).isEqualTo(committedVehicleId);
        assertThat(restored.revision()).isEqualTo(1L);
    }
}
