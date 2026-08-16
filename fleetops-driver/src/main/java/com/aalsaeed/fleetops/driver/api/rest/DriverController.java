package com.aalsaeed.fleetops.driver.api.rest;

import com.aalsaeed.fleetops.driver.application.port.in.ChangeDriverStatusUseCase;
import com.aalsaeed.fleetops.driver.application.port.in.CreateDriverCommand;
import com.aalsaeed.fleetops.driver.application.port.in.CreateDriverUseCase;
import com.aalsaeed.fleetops.driver.application.port.in.GetDriverUseCase;
import com.aalsaeed.fleetops.driver.domain.Driver;
import com.aalsaeed.fleetops.driver.domain.DriverId;
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
@RequestMapping("/api/v1/drivers")
public class DriverController {

    private final CreateDriverUseCase createDriverUseCase;
    private final GetDriverUseCase getDriverUseCase;
    private final ChangeDriverStatusUseCase changeDriverStatusUseCase;

    public DriverController(
            CreateDriverUseCase createDriverUseCase,
            GetDriverUseCase getDriverUseCase,
            ChangeDriverStatusUseCase changeDriverStatusUseCase) {
        this.createDriverUseCase = createDriverUseCase;
        this.getDriverUseCase = getDriverUseCase;
        this.changeDriverStatusUseCase = changeDriverStatusUseCase;
    }

    @PostMapping
    public ResponseEntity<DriverResponse> create(@Valid @RequestBody CreateDriverRequest request) {
        Driver driver = createDriverUseCase.createDriver(new CreateDriverCommand(
                request.externalReference(),
                request.firstName(),
                request.lastName(),
                request.phoneNumber()));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(driver.id().value())
                .toUri();

        return ResponseEntity.created(location).body(DriverResponse.from(driver));
    }

    @GetMapping("/{id}")
    public DriverResponse getById(@PathVariable UUID id) {
        return DriverResponse.from(getDriverUseCase.getById(DriverId.of(id)));
    }

    @GetMapping(params = "externalReference")
    public DriverResponse getByExternalReference(@RequestParam String externalReference) {
        return DriverResponse.from(getDriverUseCase.getByExternalReference(externalReference));
    }

    @PatchMapping("/{id}/status")
    public DriverResponse changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeDriverStatusRequest request) {
        return DriverResponse.from(changeDriverStatusUseCase.changeStatus(
                DriverId.of(id),
                request.status()));
    }
}
