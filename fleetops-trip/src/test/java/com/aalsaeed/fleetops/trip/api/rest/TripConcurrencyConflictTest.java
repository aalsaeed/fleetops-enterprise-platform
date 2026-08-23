package com.aalsaeed.fleetops.trip.api.rest;

import com.aalsaeed.fleetops.common.concurrency.OptimisticConcurrencyConflictException;
import com.aalsaeed.fleetops.trip.application.port.in.AssignTripResourcesUseCase;
import com.aalsaeed.fleetops.trip.application.port.in.CreateTripUseCase;
import com.aalsaeed.fleetops.trip.application.port.in.GetTripUseCase;
import com.aalsaeed.fleetops.trip.application.port.in.TripLifecycleUseCase;
import com.aalsaeed.fleetops.trip.domain.TripId;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TripConcurrencyConflictTest {

    @Test
    void staleTripWriteReturnsStableConflictProblem() throws Exception {
        CreateTripUseCase createTripUseCase = mock(CreateTripUseCase.class);
        GetTripUseCase getTripUseCase = mock(GetTripUseCase.class);
        AssignTripResourcesUseCase assignTripResourcesUseCase = mock(AssignTripResourcesUseCase.class);
        TripLifecycleUseCase tripLifecycleUseCase = mock(TripLifecycleUseCase.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TripController(
                        createTripUseCase,
                        getTripUseCase,
                        assignTripResourcesUseCase,
                        tripLifecycleUseCase))
                .setControllerAdvice(new TripRestExceptionHandler())
                .setValidator(validator)
                .build();

        UUID id = UUID.fromString("cccccccc-3333-4444-8888-cccccccccccc");
        when(tripLifecycleUseCase.startTrip(TripId.of(id)))
                .thenThrow(new OptimisticConcurrencyConflictException(
                        "Trip",
                        id.toString(),
                        new RuntimeException("persistence detail")));

        mockMvc.perform(post("/api/v1/trips/{id}/start", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPTIMISTIC_CONCURRENCY_CONFLICT"))
                .andExpect(jsonPath("$.resourceType").value("Trip"))
                .andExpect(jsonPath("$.resourceId").value(id.toString()))
                .andExpect(jsonPath("$.detail").value(
                        "Trip " + id + " was modified by another request; reload the current state and retry"));
    }
}
