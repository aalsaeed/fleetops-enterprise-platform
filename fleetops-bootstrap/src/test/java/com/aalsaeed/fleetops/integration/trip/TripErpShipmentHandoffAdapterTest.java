package com.aalsaeed.fleetops.integration.trip;

import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentOperation;
import com.aalsaeed.fleetops.trip.application.exception.TripNotFoundException;
import com.aalsaeed.fleetops.trip.application.port.in.CreateTripUseCase;
import com.aalsaeed.fleetops.trip.application.port.in.GetTripUseCase;
import com.aalsaeed.fleetops.trip.application.port.in.TripLifecycleUseCase;
import com.aalsaeed.fleetops.trip.domain.Trip;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TripErpShipmentHandoffAdapterTest {

    @Test
    void createsTripWhenUpsertShipmentHasNoTripYet() {
        CreateTripUseCase createTripUseCase = mock(CreateTripUseCase.class);
        GetTripUseCase getTripUseCase = mock(GetTripUseCase.class);
        TripLifecycleUseCase lifecycleUseCase = mock(TripLifecycleUseCase.class);
        when(getTripUseCase.getByExternalReference("SHIP-HANDOFF-1"))
                .thenThrow(new TripNotFoundException("SHIP-HANDOFF-1"));
        TripErpShipmentHandoffAdapter adapter = new TripErpShipmentHandoffAdapter(
                createTripUseCase, getTripUseCase, lifecycleUseCase);

        adapter.handoff(message("MSG-HANDOFF-1", "SHIP-HANDOFF-1", ErpShipmentOperation.UPSERT));

        verify(createTripUseCase).createTrip(any());
        verify(lifecycleUseCase, never()).cancelTrip(any());
    }

    @Test
    void keepsExistingTripForRepeatedBusinessUpsert() {
        CreateTripUseCase createTripUseCase = mock(CreateTripUseCase.class);
        GetTripUseCase getTripUseCase = mock(GetTripUseCase.class);
        TripLifecycleUseCase lifecycleUseCase = mock(TripLifecycleUseCase.class);
        when(getTripUseCase.getByExternalReference("SHIP-HANDOFF-2"))
                .thenReturn(Trip.create("SHIP-HANDOFF-2"));
        TripErpShipmentHandoffAdapter adapter = new TripErpShipmentHandoffAdapter(
                createTripUseCase, getTripUseCase, lifecycleUseCase);

        adapter.handoff(message("MSG-HANDOFF-2", "SHIP-HANDOFF-2", ErpShipmentOperation.UPSERT));

        verify(createTripUseCase, never()).createTrip(any());
    }

    @Test
    void cancelsExistingTripFromErpCancellation() {
        CreateTripUseCase createTripUseCase = mock(CreateTripUseCase.class);
        GetTripUseCase getTripUseCase = mock(GetTripUseCase.class);
        TripLifecycleUseCase lifecycleUseCase = mock(TripLifecycleUseCase.class);
        Trip trip = Trip.create("SHIP-HANDOFF-3");
        when(getTripUseCase.getByExternalReference("SHIP-HANDOFF-3")).thenReturn(trip);
        TripErpShipmentHandoffAdapter adapter = new TripErpShipmentHandoffAdapter(
                createTripUseCase, getTripUseCase, lifecycleUseCase);

        adapter.handoff(message("MSG-HANDOFF-3", "SHIP-HANDOFF-3", ErpShipmentOperation.CANCEL));

        verify(lifecycleUseCase).cancelTrip(trip.id());
    }

    @Test
    void treatsAlreadyCancelledTripAsIdempotent() {
        CreateTripUseCase createTripUseCase = mock(CreateTripUseCase.class);
        GetTripUseCase getTripUseCase = mock(GetTripUseCase.class);
        TripLifecycleUseCase lifecycleUseCase = mock(TripLifecycleUseCase.class);
        Trip trip = Trip.create("SHIP-HANDOFF-4");
        trip.cancel();
        when(getTripUseCase.getByExternalReference("SHIP-HANDOFF-4")).thenReturn(trip);
        TripErpShipmentHandoffAdapter adapter = new TripErpShipmentHandoffAdapter(
                createTripUseCase, getTripUseCase, lifecycleUseCase);

        adapter.handoff(message("MSG-HANDOFF-4", "SHIP-HANDOFF-4", ErpShipmentOperation.CANCEL));

        verify(lifecycleUseCase, never()).cancelTrip(any());
    }

    private static ErpShipmentMessage message(
            String sourceMessageId,
            String shipmentReference,
            ErpShipmentOperation operation) {
        return ErpShipmentMessage.create(
                "ERP-DEMO",
                sourceMessageId,
                shipmentReference,
                operation,
                Instant.parse("2026-08-17T21:15:00Z"),
                "CORR-" + sourceMessageId);
    }
}
