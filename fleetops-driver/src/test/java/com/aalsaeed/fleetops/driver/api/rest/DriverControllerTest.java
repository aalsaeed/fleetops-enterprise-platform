package com.aalsaeed.fleetops.driver.api.rest;

import com.aalsaeed.fleetops.driver.application.exception.DriverAlreadyExistsException;
import com.aalsaeed.fleetops.driver.application.exception.DriverNotFoundException;
import com.aalsaeed.fleetops.driver.application.port.in.ChangeDriverStatusUseCase;
import com.aalsaeed.fleetops.driver.application.port.in.CreateDriverCommand;
import com.aalsaeed.fleetops.driver.application.port.in.CreateDriverUseCase;
import com.aalsaeed.fleetops.driver.application.port.in.GetDriverUseCase;
import com.aalsaeed.fleetops.driver.domain.Driver;
import com.aalsaeed.fleetops.driver.domain.DriverId;
import com.aalsaeed.fleetops.driver.domain.DriverStatus;
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

class DriverControllerTest {

    private CreateDriverUseCase createDriverUseCase;
    private GetDriverUseCase getDriverUseCase;
    private ChangeDriverStatusUseCase changeDriverStatusUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createDriverUseCase = mock(CreateDriverUseCase.class);
        getDriverUseCase = mock(GetDriverUseCase.class);
        changeDriverStatusUseCase = mock(ChangeDriverStatusUseCase.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new DriverController(
                        createDriverUseCase,
                        getDriverUseCase,
                        changeDriverStatusUseCase))
                .setControllerAdvice(new DriverRestExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createDriverReturnsCreatedResource() throws Exception {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Driver driver = driver(id, "DRV-1001", DriverStatus.ACTIVE);
        when(createDriverUseCase.createDriver(any(CreateDriverCommand.class))).thenReturn(driver);

        mockMvc.perform(post("/api/v1/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "DRV-1001",
                                  "firstName": "Ahmed",
                                  "lastName": "Saleh",
                                  "phoneNumber": "+966500000001"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.externalReference").value("DRV-1001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getDriverByIdReturnsDriver() throws Exception {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(getDriverUseCase.getById(DriverId.of(id)))
                .thenReturn(driver(id, "DRV-1002", DriverStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/drivers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.externalReference").value("DRV-1002"));
    }

    @Test
    void getDriverByExternalReferenceReturnsDriver() throws Exception {
        UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(getDriverUseCase.getByExternalReference("DRV-1003"))
                .thenReturn(driver(id, "DRV-1003", DriverStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/drivers")
                        .param("externalReference", "DRV-1003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.externalReference").value("DRV-1003"));
    }

    @Test
    void changeDriverStatusReturnsUpdatedDriver() throws Exception {
        UUID id = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(changeDriverStatusUseCase.changeStatus(DriverId.of(id), DriverStatus.SUSPENDED))
                .thenReturn(driver(id, "DRV-1004", DriverStatus.SUSPENDED));

        mockMvc.perform(patch("/api/v1/drivers/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "SUSPENDED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void invalidCreateRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "",
                                  "firstName": "",
                                  "lastName": "",
                                  "phoneNumber": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void missingDriverReturnsNotFound() throws Exception {
        UUID id = UUID.fromString("55555555-5555-5555-5555-555555555555");
        when(getDriverUseCase.getById(DriverId.of(id)))
                .thenThrow(new DriverNotFoundException(id.toString()));

        mockMvc.perform(get("/api/v1/drivers/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DRIVER_NOT_FOUND"));
    }

    @Test
    void duplicateDriverReturnsConflict() throws Exception {
        when(createDriverUseCase.createDriver(any(CreateDriverCommand.class)))
                .thenThrow(new DriverAlreadyExistsException("DRV-1006"));

        mockMvc.perform(post("/api/v1/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "DRV-1006",
                                  "firstName": "Ahmed",
                                  "lastName": "Saleh",
                                  "phoneNumber": "+966500000006"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DRIVER_ALREADY_EXISTS"));
    }

    private static Driver driver(UUID id, String externalReference, DriverStatus status) {
        return Driver.restore(
                DriverId.of(id),
                externalReference,
                "Ahmed",
                "Saleh",
                "+966500000001",
                status);
    }
}
