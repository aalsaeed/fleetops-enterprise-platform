package com.aalsaeed.fleetops.vehicle.api.rest;

import com.aalsaeed.fleetops.vehicle.application.exception.VehicleAlreadyExistsException;
import com.aalsaeed.fleetops.vehicle.application.exception.VehicleNotFoundException;
import com.aalsaeed.fleetops.vehicle.application.port.in.ChangeVehicleStatusUseCase;
import com.aalsaeed.fleetops.vehicle.application.port.in.CreateVehicleCommand;
import com.aalsaeed.fleetops.vehicle.application.port.in.CreateVehicleUseCase;
import com.aalsaeed.fleetops.vehicle.application.port.in.GetVehicleUseCase;
import com.aalsaeed.fleetops.vehicle.domain.Vehicle;
import com.aalsaeed.fleetops.vehicle.domain.VehicleId;
import com.aalsaeed.fleetops.vehicle.domain.VehicleStatus;
import com.aalsaeed.fleetops.vehicle.domain.VehicleType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VehicleControllerTest {

    private CreateVehicleUseCase createVehicleUseCase;
    private GetVehicleUseCase getVehicleUseCase;
    private ChangeVehicleStatusUseCase changeVehicleStatusUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createVehicleUseCase = mock(CreateVehicleUseCase.class);
        getVehicleUseCase = mock(GetVehicleUseCase.class);
        changeVehicleStatusUseCase = mock(ChangeVehicleStatusUseCase.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new VehicleController(
                        createVehicleUseCase,
                        getVehicleUseCase,
                        changeVehicleStatusUseCase))
                .setControllerAdvice(new VehicleRestExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createVehicleReturnsCreatedResource() throws Exception {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Vehicle vehicle = vehicle(id, "VEH-1001", VehicleType.TRACTOR, VehicleStatus.ACTIVE);
        when(createVehicleUseCase.createVehicle(any(CreateVehicleCommand.class))).thenReturn(vehicle);

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "VEH-1001",
                                  "description": "Fleet tractor 1001",
                                  "type": "TRACTOR",
                                  "serialNumber": "SN-1001"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.externalReference").value("VEH-1001"))
                .andExpect(jsonPath("$.type").value("TRACTOR"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getVehicleByIdReturnsVehicle() throws Exception {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(getVehicleUseCase.getById(VehicleId.of(id)))
                .thenReturn(vehicle(id, "VEH-1002", VehicleType.TRAILER, VehicleStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/vehicles/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.externalReference").value("VEH-1002"))
                .andExpect(jsonPath("$.type").value("TRAILER"));
    }

    @Test
    void getVehicleByExternalReferenceReturnsVehicle() throws Exception {
        UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(getVehicleUseCase.getByExternalReference("VEH-1003"))
                .thenReturn(vehicle(id, "VEH-1003", VehicleType.BULKER, VehicleStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/vehicles")
                        .param("externalReference", "VEH-1003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.externalReference").value("VEH-1003"));
    }

    @Test
    void changeVehicleStatusReturnsUpdatedVehicle() throws Exception {
        UUID id = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(changeVehicleStatusUseCase.changeStatus(VehicleId.of(id), VehicleStatus.MAINTENANCE))
                .thenReturn(vehicle(id, "VEH-1004", VehicleType.TRACTOR, VehicleStatus.MAINTENANCE));

        mockMvc.perform(patch("/api/v1/vehicles/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "MAINTENANCE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));
    }

    @Test
    void invalidCreateRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "",
                                  "description": "",
                                  "type": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void missingVehicleReturnsNotFound() throws Exception {
        UUID id = UUID.fromString("55555555-5555-5555-5555-555555555555");
        when(getVehicleUseCase.getById(VehicleId.of(id)))
                .thenThrow(new VehicleNotFoundException(id.toString()));

        mockMvc.perform(get("/api/v1/vehicles/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
    }

    @Test
    void duplicateVehicleReturnsConflict() throws Exception {
        when(createVehicleUseCase.createVehicle(any(CreateVehicleCommand.class)))
                .thenThrow(new VehicleAlreadyExistsException("VEH-1006"));

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "VEH-1006",
                                  "description": "Duplicate tractor",
                                  "type": "TRACTOR",
                                  "serialNumber": "SN-1006"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_ALREADY_EXISTS"));
    }

    private static Vehicle vehicle(UUID id, String externalReference, VehicleType type, VehicleStatus status) {
        return Vehicle.restore(
                VehicleId.of(id),
                externalReference,
                "Vehicle description",
                type,
                "SN-TEST",
                status);
    }
}
