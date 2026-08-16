package com.aalsaeed.fleetops.trip.api.rest;

import com.aalsaeed.fleetops.trip.application.exception.TripNotFoundException;
import com.aalsaeed.fleetops.trip.application.port.in.AssignTripResourcesCommand;
import com.aalsaeed.fleetops.trip.application.port.in.AssignTripResourcesUseCase;
import com.aalsaeed.fleetops.trip.application.port.in.CreateTripCommand;
import com.aalsaeed.fleetops.trip.application.port.in.CreateTripUseCase;
import com.aalsaeed.fleetops.trip.application.port.in.GetTripUseCase;
import com.aalsaeed.fleetops.trip.application.port.in.TripLifecycleUseCase;
import com.aalsaeed.fleetops.trip.domain.DriverReference;
import com.aalsaeed.fleetops.trip.domain.Trip;
import com.aalsaeed.fleetops.trip.domain.TripId;
import com.aalsaeed.fleetops.trip.domain.TripStatus;
import com.aalsaeed.fleetops.trip.domain.VehicleReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TripControllerTest {

    private CreateTripUseCase createTripUseCase;
    private GetTripUseCase getTripUseCase;
    private AssignTripResourcesUseCase assignTripResourcesUseCase;
    private TripLifecycleUseCase tripLifecycleUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createTripUseCase = mock(CreateTripUseCase.class);
        getTripUseCase = mock(GetTripUseCase.class);
        assignTripResourcesUseCase = mock(AssignTripResourcesUseCase.class);
        tripLifecycleUseCase = mock(TripLifecycleUseCase.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TripController(
                        createTripUseCase,
                        getTripUseCase,
                        assignTripResourcesUseCase,
                        tripLifecycleUseCase))
                .setControllerAdvice(new TripRestExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createTripReturnsCreatedResource() throws Exception {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(createTripUseCase.createTrip(any(CreateTripCommand.class)))
                .thenReturn(plannedTrip(id, "TRIP-1001"));

        mockMvc.perform(post("/api/v1/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalReference":"TRIP-1001"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.externalReference").value("TRIP-1001"))
                .andExpect(jsonPath("$.status").value("PLANNED"));
    }

    @Test
    void getTripByIdReturnsTrip() throws Exception {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(getTripUseCase.getById(TripId.of(id)))
                .thenReturn(plannedTrip(id, "TRIP-1002"));

        mockMvc.perform(get("/api/v1/trips/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalReference").value("TRIP-1002"));
    }

    @Test
    void assignResourcesReturnsAssignedTrip() throws Exception {
        UUID tripId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID driverId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID tractorId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID attachmentId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

        when(assignTripResourcesUseCase.assignResources(any(AssignTripResourcesCommand.class)))
                .thenReturn(assignedTrip(tripId, "TRIP-1003", driverId, tractorId, attachmentId, TripStatus.ASSIGNED));

        mockMvc.perform(put("/api/v1/trips/{id}/assignment", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "driverId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                                  "primaryVehicleId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                                  "attachmentVehicleId":"cccccccc-cccc-cccc-cccc-cccccccccccc"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId").value(driverId.toString()))
                .andExpect(jsonPath("$.primaryVehicleId").value(tractorId.toString()))
                .andExpect(jsonPath("$.attachmentVehicleId").value(attachmentId.toString()))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    void startTripReturnsInProgressTrip() throws Exception {
        UUID tripId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID driverId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        UUID tractorId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        when(tripLifecycleUseCase.startTrip(TripId.of(tripId)))
                .thenReturn(assignedTrip(tripId, "TRIP-1004", driverId, tractorId, null, TripStatus.IN_PROGRESS));

        mockMvc.perform(post("/api/v1/trips/{id}/start", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void invalidCreateRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalReference\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void missingTripReturnsNotFound() throws Exception {
        UUID id = UUID.fromString("55555555-5555-5555-5555-555555555555");
        when(getTripUseCase.getById(TripId.of(id)))
                .thenThrow(new TripNotFoundException(id.toString()));

        mockMvc.perform(get("/api/v1/trips/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
    }

    private static Trip plannedTrip(UUID id, String externalReference) {
        return Trip.restore(
                TripId.of(id),
                externalReference,
                null,
                null,
                null,
                TripStatus.PLANNED);
    }

    private static Trip assignedTrip(
            UUID tripId,
            String externalReference,
            UUID driverId,
            UUID tractorId,
            UUID attachmentId,
            TripStatus status) {
        return Trip.restore(
                TripId.of(tripId),
                externalReference,
                DriverReference.of(driverId),
                VehicleReference.of(tractorId),
                attachmentId == null ? null : VehicleReference.of(attachmentId),
                status);
    }
}
