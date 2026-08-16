package com.aalsaeed.fleetops.vehicle.infrastructure.persistence.jpa;

import com.aalsaeed.fleetops.vehicle.application.port.out.VehicleRepository;
import com.aalsaeed.fleetops.vehicle.domain.Vehicle;
import com.aalsaeed.fleetops.vehicle.domain.VehicleStatus;
import com.aalsaeed.fleetops.vehicle.domain.VehicleType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class JpaVehicleRepositoryAdapterIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    void savesAndRestoresVehicleDomainState() {
        Vehicle vehicle = Vehicle.create(
                "VEH-TC-1001",
                "Primary tractor",
                VehicleType.TRACTOR,
                "SN-TC-1001");
        vehicle.changeStatus(VehicleStatus.MAINTENANCE);

        Vehicle saved = vehicleRepository.save(vehicle);
        Vehicle restored = vehicleRepository.findById(saved.id()).orElseThrow();

        assertThat(restored.id()).isEqualTo(saved.id());
        assertThat(restored.externalReference()).isEqualTo("VEH-TC-1001");
        assertThat(restored.description()).isEqualTo("Primary tractor");
        assertThat(restored.type()).isEqualTo(VehicleType.TRACTOR);
        assertThat(restored.serialNumber()).isEqualTo("SN-TC-1001");
        assertThat(restored.status()).isEqualTo(VehicleStatus.MAINTENANCE);
    }

    @Test
    void findsVehicleByNormalizedExternalReference() {
        Vehicle saved = vehicleRepository.save(Vehicle.create(
                "  VEH-TC-1002  ",
                "Bulk trailer",
                VehicleType.BULKER,
                null));

        assertThat(vehicleRepository.existsByExternalReference("  VEH-TC-1002  ")).isTrue();

        Vehicle restored = vehicleRepository.findByExternalReference(" VEH-TC-1002 ").orElseThrow();
        assertThat(restored.id()).isEqualTo(saved.id());
        assertThat(restored.externalReference()).isEqualTo("VEH-TC-1002");
        assertThat(restored.serialNumber()).isNull();
    }

    @Test
    void rejectsDuplicateExternalReferenceAtDatabaseBoundary() {
        vehicleRepository.save(Vehicle.create(
                "VEH-TC-DUP",
                "First vehicle",
                VehicleType.TRACTOR,
                "SN-DUP-1"));

        assertThatThrownBy(() -> vehicleRepository.save(Vehicle.create(
                "VEH-TC-DUP",
                "Second vehicle",
                VehicleType.TRAILER,
                "SN-DUP-2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
