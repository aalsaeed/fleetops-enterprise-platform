package com.aalsaeed.fleetops.trip.infrastructure.persistence.jpa;

import com.aalsaeed.fleetops.trip.application.port.out.TripRepository;
import com.aalsaeed.fleetops.trip.domain.DriverReference;
import com.aalsaeed.fleetops.trip.domain.Trip;
import com.aalsaeed.fleetops.trip.domain.TripStatus;
import com.aalsaeed.fleetops.trip.domain.VehicleReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class JpaTripRepositoryAdapterIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_trip_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Autowired
    private TripRepository tripRepository;

    @Test
    void savesAndRestoresAssignedTripState() {
        UUID driverId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID tractorId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID trailerId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        Trip trip = Trip.create("TRIP-TC-1001");
        trip.assignResources(
                DriverReference.of(driverId),
                VehicleReference.of(tractorId),
                VehicleReference.of(trailerId));

        Trip saved = tripRepository.save(trip);
        Trip restored = tripRepository.findById(saved.id()).orElseThrow();

        assertThat(restored.id()).isEqualTo(saved.id());
        assertThat(restored.externalReference()).isEqualTo("TRIP-TC-1001");
        assertThat(restored.driverReference().value()).isEqualTo(driverId);
        assertThat(restored.primaryVehicleReference().value()).isEqualTo(tractorId);
        assertThat(restored.attachmentVehicleReference().value()).isEqualTo(trailerId);
        assertThat(restored.status()).isEqualTo(TripStatus.ASSIGNED);
    }

    @Test
    void findsTripByNormalizedExternalReferenceAndPersistsLifecycleChanges() {
        Trip trip = Trip.create("  TRIP-TC-1002  ");
        trip.assignResources(
                DriverReference.of(UUID.fromString("44444444-4444-4444-4444-444444444444")),
                VehicleReference.of(UUID.fromString("55555555-5555-5555-5555-555555555555")),
                null);
        trip.start();

        tripRepository.save(trip);

        assertThat(tripRepository.existsByExternalReference(" TRIP-TC-1002 ")).isTrue();

        Trip restored = tripRepository.findByExternalReference("  TRIP-TC-1002  ").orElseThrow();
        assertThat(restored.externalReference()).isEqualTo("TRIP-TC-1002");
        assertThat(restored.status()).isEqualTo(TripStatus.IN_PROGRESS);
        assertThat(restored.attachmentVehicleReference()).isNull();
    }

    @Test
    void rejectsDuplicateExternalReferenceAtDatabaseBoundary() {
        tripRepository.save(Trip.create("TRIP-TC-DUP"));

        assertThatThrownBy(() -> tripRepository.save(Trip.create("TRIP-TC-DUP")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
