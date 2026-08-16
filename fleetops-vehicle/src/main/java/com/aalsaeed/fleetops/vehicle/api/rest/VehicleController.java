package com.aalsaeed.fleetops.vehicle.api.rest;

import com.aalsaeed.fleetops.vehicle.application.port.in.ChangeVehicleStatusUseCase;
import com.aalsaeed.fleetops.vehicle.application.port.in.CreateVehicleCommand;
import com.aalsaeed.fleetops.vehicle.application.port.in.CreateVehicleUseCase;
import com.aalsaeed.fleetops.vehicle.application.port.in.GetVehicleUseCase;
import com.aalsaeed.fleetops.vehicle.domain.Vehicle;
import com.aalsaeed.fleetops.vehicle.domain.VehicleId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final CreateVehicleUseCase createVehicleUseCase;
    private final GetVehicleUseCase getVehicleUseCase;
    private final ChangeVehicleStatusUseCase changeVehicleStatusUseCase;

    public VehicleController(
            CreateVehicleUseCase createVehicleUseCase,
            GetVehicleUseCase getVehicleUseCase,
            ChangeVehicleStatusUseCase changeVehicleStatusUseCase) {
        this.createVehicleUseCase = createVehicleUseCase;
        this.getVehicleUseCase = getVehicleUseCase;
        this.changeVehicleStatusUseCase = changeVehicleStatusUseCase;
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody CreateVehicleRequest request) {
        Vehicle vehicle = createVehicleUseCase.createVehicle(new CreateVehicleCommand(
                request.externalReference(),
                request.description(),
                request.type(),
                request.serialNumber()));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(vehicle.id().value())
                .toUri();

        return ResponseEntity.created(location).body(VehicleResponse.from(vehicle));
    }

    @GetMapping("/{id}")
    public VehicleResponse getById(@PathVariable UUID id) {
        return VehicleResponse.from(getVehicleUseCase.getById(VehicleId.of(id)));
    }

    @GetMapping(params = "externalReference")
    public VehicleResponse getByExternalReference(@RequestParam String externalReference) {
        return VehicleResponse.from(getVehicleUseCase.getByExternalReference(externalReference));
    }

    @PatchMapping("/{id}/status")
    public VehicleResponse changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeVehicleStatusRequest request) {
        return VehicleResponse.from(changeVehicleStatusUseCase.changeStatus(
                VehicleId.of(id),
                request.status()));
    }
}
