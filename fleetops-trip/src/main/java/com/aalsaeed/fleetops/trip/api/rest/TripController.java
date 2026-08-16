package com.aalsaeed.fleetops.trip.api.rest;

import com.aalsaeed.fleetops.trip.application.port.in.AssignTripResourcesCommand;
import com.aalsaeed.fleetops.trip.application.port.in.AssignTripResourcesUseCase;
import com.aalsaeed.fleetops.trip.application.port.in.CreateTripCommand;
import com.aalsaeed.fleetops.trip.application.port.in.CreateTripUseCase;
import com.aalsaeed.fleetops.trip.application.port.in.GetTripUseCase;
import com.aalsaeed.fleetops.trip.application.port.in.TripLifecycleUseCase;
import com.aalsaeed.fleetops.trip.domain.Trip;
import com.aalsaeed.fleetops.trip.domain.TripId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

    private final CreateTripUseCase createTripUseCase;
    private final GetTripUseCase getTripUseCase;
    private final AssignTripResourcesUseCase assignTripResourcesUseCase;
    private final TripLifecycleUseCase tripLifecycleUseCase;

    public TripController(
            CreateTripUseCase createTripUseCase,
            GetTripUseCase getTripUseCase,
            AssignTripResourcesUseCase assignTripResourcesUseCase,
            TripLifecycleUseCase tripLifecycleUseCase) {
        this.createTripUseCase = createTripUseCase;
        this.getTripUseCase = getTripUseCase;
        this.assignTripResourcesUseCase = assignTripResourcesUseCase;
        this.tripLifecycleUseCase = tripLifecycleUseCase;
    }

    @PostMapping
    public ResponseEntity<TripResponse> create(@Valid @RequestBody CreateTripRequest request) {
        Trip trip = createTripUseCase.createTrip(new CreateTripCommand(request.externalReference()));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(trip.id().value())
                .toUri();

        return ResponseEntity.created(location).body(TripResponse.from(trip));
    }

    @GetMapping("/{id}")
    public TripResponse getById(@PathVariable UUID id) {
        return TripResponse.from(getTripUseCase.getById(TripId.of(id)));
    }

    @GetMapping(params = "externalReference")
    public TripResponse getByExternalReference(@RequestParam String externalReference) {
        return TripResponse.from(getTripUseCase.getByExternalReference(externalReference));
    }

    @PutMapping("/{id}/assignment")
    public TripResponse assignResources(
            @PathVariable UUID id,
            @Valid @RequestBody AssignTripResourcesRequest request) {
        return TripResponse.from(assignTripResourcesUseCase.assignResources(new AssignTripResourcesCommand(
                TripId.of(id),
                request.driverId(),
                request.primaryVehicleId(),
                request.attachmentVehicleId())));
    }

    @PostMapping("/{id}/start")
    public TripResponse start(@PathVariable UUID id) {
        return TripResponse.from(tripLifecycleUseCase.startTrip(TripId.of(id)));
    }

    @PostMapping("/{id}/complete")
    public TripResponse complete(@PathVariable UUID id) {
        return TripResponse.from(tripLifecycleUseCase.completeTrip(TripId.of(id)));
    }

    @PostMapping("/{id}/cancel")
    public TripResponse cancel(@PathVariable UUID id) {
        return TripResponse.from(tripLifecycleUseCase.cancelTrip(TripId.of(id)));
    }
}
