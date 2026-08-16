package com.aalsaeed.fleetops.vehicle.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VehicleTest {

    @Test
    void newVehicleStartsActive() {
        Vehicle vehicle = Vehicle.create(
                "VEH-1001",
                "Primary tractor",
                VehicleType.TRACTOR,
                "VIN-1001");

        assertNotNull(vehicle.id());
        assertEquals(VehicleStatus.ACTIVE, vehicle.status());
    }

    @Test
    void textualValuesAreNormalized() {
        Vehicle vehicle = Vehicle.create(
                "  VEH-1001  ",
                "  Primary tractor  ",
                VehicleType.TRACTOR,
                "  VIN-1001  ");

        assertEquals("VEH-1001", vehicle.externalReference());
        assertEquals("Primary tractor", vehicle.description());
        assertEquals("VIN-1001", vehicle.serialNumber());
    }

    @Test
    void serialNumberIsOptional() {
        Vehicle vehicle = Vehicle.create(
                "VEH-1001",
                "Primary tractor",
                VehicleType.TRACTOR,
                "   ");

        assertNull(vehicle.serialNumber());
    }

    @Test
    void blankRequiredValuesAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Vehicle.create(" ", "Primary tractor", VehicleType.TRACTOR, null));

        assertThrows(
                IllegalArgumentException.class,
                () -> Vehicle.create("VEH-1001", " ", VehicleType.TRACTOR, null));
    }

    @Test
    void vehicleTypeIsRequired() {
        assertThrows(
                NullPointerException.class,
                () -> Vehicle.create("VEH-1001", "Primary tractor", null, null));
    }

    @Test
    void activeVehicleCanEnterAndLeaveMaintenance() {
        Vehicle vehicle = Vehicle.create(
                "VEH-1001",
                "Primary tractor",
                VehicleType.TRACTOR,
                null);

        vehicle.changeStatus(VehicleStatus.MAINTENANCE);
        assertEquals(VehicleStatus.MAINTENANCE, vehicle.status());

        vehicle.changeStatus(VehicleStatus.ACTIVE);
        assertEquals(VehicleStatus.ACTIVE, vehicle.status());
    }

    @Test
    void retiredVehicleCannotReturnToService() {
        Vehicle vehicle = Vehicle.create(
                "VEH-1001",
                "Primary tractor",
                VehicleType.TRACTOR,
                null);
        vehicle.changeStatus(VehicleStatus.RETIRED);

        assertThrows(
                InvalidVehicleStatusTransitionException.class,
                () -> vehicle.changeStatus(VehicleStatus.ACTIVE));
    }
}
