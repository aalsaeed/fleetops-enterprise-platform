package com.aalsaeed.fleetops.trip.infrastructure.resource;

import com.aalsaeed.fleetops.driver.application.port.out.DriverRepository;
import com.aalsaeed.fleetops.driver.domain.Driver;
import com.aalsaeed.fleetops.trip.application.port.out.DriverResource;
import com.aalsaeed.fleetops.trip.application.port.out.VehicleResource;
import com.aalsaeed.fleetops.trip.application.port.out.VehicleResourceType;
import com.aalsaeed.fleetops.vehicle.application.port.out.VehicleRepository;
import com.aalsaeed.fleetops.vehicle.domain.Vehicle;
import com.aalsaeed.fleetops.vehicle.domain.VehicleStatus;
import com.aalsaeed.fleetops.vehicle.domain.VehicleType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TripResourceAdaptersTest {

    @Test
    void mapsActiveDriverToOperationalTripResource() {
        DriverRepository repository = mock(DriverRepository.class);
        Driver driver = Driver.create("DRV-RESOURCE-1", "Ahmed", "Saleh", "+966500000001");
        when(repository.findById(any())).thenReturn(Optional.of(driver));

        DriverResourceAdapter adapter = new DriverResourceAdapter(repository);
        DriverResource resource = adapter.findById(driver.id().value()).orElseThrow();

        assertThat(resource.id()).isEqualTo(driver.id().value());
        assertThat(resource.operational()).isTrue();
    }

    @Test
    void mapsActiveTractorTypeForTripAssignment() {
        VehicleRepository repository = mock(VehicleRepository.class);
        Vehicle tractor = Vehicle.create("VEH-RESOURCE-1", "Primary Tractor", VehicleType.TRACTOR, "SN-1");
        when(repository.findById(any())).thenReturn(Optional.of(tractor));

        VehicleResourceAdapter adapter = new VehicleResourceAdapter(repository);
        VehicleResource resource = adapter.findById(tractor.id().value()).orElseThrow();

        assertThat(resource.id()).isEqualTo(tractor.id().value());
        assertThat(resource.type()).isEqualTo(VehicleResourceType.TRACTOR);
        assertThat(resource.operational()).isTrue();
    }

    @Test
    void mapsMaintenanceVehicleToUnavailableTripResource() {
        VehicleRepository repository = mock(VehicleRepository.class);
        Vehicle trailer = Vehicle.create("VEH-RESOURCE-2", "Trailer", VehicleType.TRAILER, "SN-2");
        trailer.changeStatus(VehicleStatus.MAINTENANCE);
        when(repository.findById(any())).thenReturn(Optional.of(trailer));

        VehicleResourceAdapter adapter = new VehicleResourceAdapter(repository);
        VehicleResource resource = adapter.findById(trailer.id().value()).orElseThrow();

        assertThat(resource.type()).isEqualTo(VehicleResourceType.TRAILER);
        assertThat(resource.operational()).isFalse();
    }
}
