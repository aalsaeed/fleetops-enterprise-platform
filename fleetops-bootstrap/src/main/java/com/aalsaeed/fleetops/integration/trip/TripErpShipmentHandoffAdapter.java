package com.aalsaeed.fleetops.integration.trip;

import com.aalsaeed.fleetops.integration.application.port.out.ErpShipmentTripHandoffPort;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentOperation;
import com.aalsaeed.fleetops.trip.application.exception.TripAlreadyExistsException;
import com.aalsaeed.fleetops.trip.application.exception.TripNotFoundException;
import com.aalsaeed.fleetops.trip.application.port.in.CreateTripCommand;
import com.aalsaeed.fleetops.trip.application.port.in.CreateTripUseCase;
import com.aalsaeed.fleetops.trip.application.port.in.GetTripUseCase;
import com.aalsaeed.fleetops.trip.application.port.in.TripLifecycleUseCase;
import com.aalsaeed.fleetops.trip.domain.Trip;
import com.aalsaeed.fleetops.trip.domain.TripStatus;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class TripErpShipmentHandoffAdapter implements ErpShipmentTripHandoffPort {

    private final CreateTripUseCase createTripUseCase;
    private final GetTripUseCase getTripUseCase;
    private final TripLifecycleUseCase tripLifecycleUseCase;

    public TripErpShipmentHandoffAdapter(
            CreateTripUseCase createTripUseCase,
            GetTripUseCase getTripUseCase,
            TripLifecycleUseCase tripLifecycleUseCase) {
        this.createTripUseCase = Objects.requireNonNull(createTripUseCase, "Create trip use case cannot be null");
        this.getTripUseCase = Objects.requireNonNull(getTripUseCase, "Get trip use case cannot be null");
        this.tripLifecycleUseCase = Objects.requireNonNull(tripLifecycleUseCase, "Trip lifecycle use case cannot be null");
    }

    @Override
    public void handoff(ErpShipmentMessage message) {
        Objects.requireNonNull(message, "ERP shipment message cannot be null");
        if (message.operation() == ErpShipmentOperation.UPSERT) {
            ensureTripExists(message.shipmentReference());
            return;
        }
        cancelTrip(message.shipmentReference());
    }

    private void ensureTripExists(String shipmentReference) {
        try {
            getTripUseCase.getByExternalReference(shipmentReference);
        } catch (TripNotFoundException notFound) {
            try {
                createTripUseCase.createTrip(new CreateTripCommand(shipmentReference));
            } catch (TripAlreadyExistsException concurrentCreate) {
                getTripUseCase.getByExternalReference(shipmentReference);
            }
        }
    }

    private void cancelTrip(String shipmentReference) {
        Trip trip = getTripUseCase.getByExternalReference(shipmentReference);
        if (trip.status() == TripStatus.CANCELLED) {
            return;
        }
        tripLifecycleUseCase.cancelTrip(trip.id());
    }
}
