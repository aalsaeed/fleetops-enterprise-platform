package com.aalsaeed.fleetops.driver.api.rest;

import com.aalsaeed.fleetops.common.concurrency.OptimisticConcurrencyConflictException;
import com.aalsaeed.fleetops.driver.application.port.in.ChangeDriverStatusUseCase;
import com.aalsaeed.fleetops.driver.application.port.in.CreateDriverUseCase;
import com.aalsaeed.fleetops.driver.application.port.in.GetDriverUseCase;
import com.aalsaeed.fleetops.driver.domain.DriverId;
import com.aalsaeed.fleetops.driver.domain.DriverStatus;
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

class DriverConcurrencyConflictTest {

    @Test
    void staleDriverWriteReturnsStableConflictProblem() throws Exception {
        CreateDriverUseCase createDriverUseCase = mock(CreateDriverUseCase.class);
        GetDriverUseCase getDriverUseCase = mock(GetDriverUseCase.class);
        ChangeDriverStatusUseCase changeDriverStatusUseCase = mock(ChangeDriverStatusUseCase.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new DriverController(
                        createDriverUseCase,
                        getDriverUseCase,
                        changeDriverStatusUseCase))
                .setControllerAdvice(new DriverRestExceptionHandler())
                .setValidator(validator)
                .build();

        UUID id = UUID.fromString("aaaaaaaa-1111-4444-8888-aaaaaaaaaaaa");
        when(changeDriverStatusUseCase.changeStatus(DriverId.of(id), DriverStatus.SUSPENDED))
                .thenThrow(new OptimisticConcurrencyConflictException(
                        "Driver",
                        id.toString(),
                        new RuntimeException("persistence detail")));

        mockMvc.perform(patch("/api/v1/drivers/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPTIMISTIC_CONCURRENCY_CONFLICT"))
                .andExpect(jsonPath("$.resourceType").value("Driver"))
                .andExpect(jsonPath("$.resourceId").value(id.toString()))
                .andExpect(jsonPath("$.detail").value(
                        "Driver " + id + " was modified by another request; reload the current state and retry"));
    }
}
