package com.aalsaeed.fleetops.trip.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TripTest {

    @Test
    void newTripStartsPlannedWithoutAssignment() {
        Trip trip = Trip.create("TRIP-1001");

        assertNotNull(trip.id());
        assertEquals(TripStatus.PLANNED, trip.status());
        assertNull(trip.driverReference());
        assertNull(trip.primaryVehicleReference());
        assertNull(trip.attachmentVehicleReference());
    }

    @Test
    void externalReferenceIsTrimmed() {
        Trip trip = Trip.create("  TRIP-1001  ");

        assertEquals("TRIP-1001", trip.externalReference());
    }

    @Test
    void blankExternalReferenceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Trip.create(" "));
    }

    @Test
    void assigningResourcesMovesTripToAssigned() {
        Trip trip = Trip.create("TRIP-1002");
        DriverReference driver = driver("11111111-1111-1111-1111-111111111111");
        VehicleReference tractor = vehicle("22222222-2222-2222-2222-222222222222");
        VehicleReference trailer = vehicle("33333333-3333-3333-3333-333333333333");

        trip.assignResources(driver, tractor, trailer);

        assertEquals(TripStatus.ASSIGNED, trip.status());
        assertEquals(driver, trip.driverReference());
        assertEquals(tractor, trip.primaryVehicleReference());
        assertEquals(trailer, trip.attachmentVehicleReference());
    }

    @Test
    void attachmentIsOptional() {
        Trip trip = Trip.create("TRIP-1003");

        trip.assignResources(
                driver("44444444-4444-4444-4444-444444444444"),
                vehicle("55555555-5555-5555-5555-555555555555"),
                null);

        assertEquals(TripStatus.ASSIGNED, trip.status());
        assertNull(trip.attachmentVehicleReference());
    }

    @Test
    void primaryAndAttachmentMustBeDifferentVehicles() {
        Trip trip = Trip.create("TRIP-1004");
        VehicleReference sameVehicle = vehicle("66666666-6666-6666-6666-666666666666");

        assertThrows(
                InvalidTripAssignmentException.class,
                () -> trip.assignResources(
                        driver("77777777-7777-7777-7777-777777777777"),
                        sameVehicle,
                        sameVehicle));
    }

    @Test
    void resourcesCanBeReassignedBeforeTripStarts() {
        Trip trip = Trip.create("TRIP-1005");
        trip.assignResources(
                driver("88888888-8888-8888-8888-888888888888"),
                vehicle("99999999-9999-9999-9999-999999999999"),
                null);

        DriverReference replacementDriver = driver("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        VehicleReference replacementVehicle = vehicle("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        trip.assignResources(replacementDriver, replacementVehicle, null);

        assertEquals(TripStatus.ASSIGNED, trip.status());
        assertEquals(replacementDriver, trip.driverReference());
        assertEquals(replacementVehicle, trip.primaryVehicleReference());
    }

    @Test
    void resourcesCannotBeReassignedAfterTripStarts() {
        Trip trip = assignedTrip("TRIP-1006");
        trip.start();

        assertThrows(
                InvalidTripAssignmentException.class,
                () -> trip.assignResources(
                        driver("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                        vehicle("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                        null));
    }

    @Test
    void plannedTripCannotStartWithoutAssignment() {
        Trip trip = Trip.create("TRIP-1007");

        assertThrows(InvalidTripStatusTransitionException.class, trip::start);
    }

    @Test
    void assignedTripCanProgressToCompletion() {
        Trip trip = assignedTrip("TRIP-1008");

        trip.start();
        assertEquals(TripStatus.IN_PROGRESS, trip.status());

        trip.complete();
        assertEquals(TripStatus.COMPLETED, trip.status());
    }

    @Test
    void tripCanBeCancelledBeforeCompletion() {
        Trip trip = assignedTrip("TRIP-1009");
        trip.start();

        trip.cancel();

        assertEquals(TripStatus.CANCELLED, trip.status());
    }

    @Test
    void completedTripIsTerminal() {
        Trip trip = assignedTrip("TRIP-1010");
        trip.start();
        trip.complete();

        assertThrows(InvalidTripStatusTransitionException.class, trip::cancel);
    }

    @Test
    void restoredInProgressTripRequiresAssignment() {
        assertThrows(
                InvalidTripAssignmentException.class,
                () -> Trip.restore(
                        TripId.newId(),
                        "TRIP-1011",
                        null,
                        null,
                        null,
                        TripStatus.IN_PROGRESS));
    }

    private static Trip assignedTrip(String externalReference) {
        Trip trip = Trip.create(externalReference);
        trip.assignResources(
                driver("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"),
                vehicle("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                null);
        return trip;
    }

    private static DriverReference driver(String value) {
        return DriverReference.of(UUID.fromString(value));
    }

    private static VehicleReference vehicle(String value) {
        return VehicleReference.of(UUID.fromString(value));
    }
}
