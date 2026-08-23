package com.aalsaeed.fleetops.security;

import com.aalsaeed.fleetops.common.concurrency.OptimisticConcurrencyConflictException;
import com.aalsaeed.fleetops.driver.application.service.DriverApplicationService;
import com.aalsaeed.fleetops.driver.domain.DriverId;
import com.aalsaeed.fleetops.driver.domain.DriverStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "fleetops.security.test-permit-all=false")
@AutoConfigureMockMvc
@Testcontainers
class ConcurrencyConflictSecurityIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_concurrency_security_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DriverApplicationService driverApplicationService;

    @Test
    void userAuthorityCannotReachProtectedStaleWriteHandling() throws Exception {
        UUID id = UUID.fromString("dddddddd-4444-4444-8888-dddddddddddd");

        mockMvc.perform(patch("/api/v1/drivers/{id}/status", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority(FleetOpsAuthorities.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void operatorAuthorityReceivesStableConflictWithoutProviderDetails() throws Exception {
        UUID id = UUID.fromString("eeeeeeee-5555-4444-8888-eeeeeeeeeeee");
        when(driverApplicationService.changeStatus(DriverId.of(id), DriverStatus.SUSPENDED))
                .thenThrow(new OptimisticConcurrencyConflictException(
                        "Driver",
                        id.toString(),
                        new RuntimeException("org.hibernate.StaleObjectStateException: sensitive detail")));

        mockMvc.perform(patch("/api/v1/drivers/{id}/status", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority(FleetOpsAuthorities.OPERATOR)))
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
