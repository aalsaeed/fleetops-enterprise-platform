package com.aalsaeed.fleetops.vehicle.api.rest;

import com.aalsaeed.fleetops.common.concurrency.OptimisticConcurrencyConflictException;
import com.aalsaeed.fleetops.vehicle.application.port.in.ChangeVehicleStatusUseCase;
import com.aalsaeed.fleetops.vehicle.application.port.in.CreateVehicleUseCase;
import com.aalsaeed.fleetops.vehicle.application.port.in.GetVehicleUseCase;
import com.aalsaeed.fleetops.vehicle.domain.VehicleId;
import com.aalsaeed.fleetops.vehicle.domain.VehicleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VehicleConcurrencyConflictTest {

    @Test
    void staleVehicleWriteReturnsStableConflictProblem() throws Exception {
        CreateVehicleUseCase createVehicleUseCase = mock(CreateVehicleUseCase.class);
        GetVehicleUseCase getVehicleUseCase = mock(GetVehicleUseCase.class);
        ChangeVehicleStatusUseCase changeVehicleStatusUseCase = mock(ChangeVehicleStatusUseCase.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new VehicleController(
                        createVehicleUseCase,
                        getVehicleUseCase,
                        changeVehicleStatusUseCase))
                .setControllerAdvice(new VehicleRestExceptionHandler())
                .setValidator(validator)
                .build();

        UUID id = UUID.fromString("bbbbbbbb-2222-4444-8888-bbbbbbbbbbbb");
        when(changeVehicleStatusUseCase.changeStatus(VehicleId.of(id), VehicleStatus.MAINTENANCE))
                .thenThrow(new OptimisticConcurrencyConflictException(
                        "Vehicle",
                        id.toString(),
                        new RuntimeException("persistence detail")));

        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"MAINTENANCE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPTIMISTIC_CONCURRENCY_CONFLICT"))
                .andExpect(jsonPath("$.resourceType").value("Vehicle"))
                .andExpect(jsonPath("$.resourceId").value(id.toString()))
                .andExpect(jsonPath("$.detail").value(
                        "Vehicle " + id + " was modified by another request; reload the current state and retry"));
    }
}
