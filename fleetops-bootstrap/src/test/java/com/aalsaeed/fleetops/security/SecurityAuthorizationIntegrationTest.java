package com.aalsaeed.fleetops.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "fleetops.security.test-permit-all=false")
@AutoConfigureMockMvc
@Testcontainers
class SecurityAuthorizationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_security_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousApiRequestReturnsBearer401Problem() throws Exception {
        mockMvc.perform(get("/api/v1/drivers/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void userAuthorityCanReadButCannotWrite() throws Exception {
        mockMvc.perform(get("/api/v1/drivers/{id}", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority(FleetOpsAuthorities.USER))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/drivers")
                        .with(jwt().authorities(new SimpleGrantedAuthority(FleetOpsAuthorities.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference":"DRV-SEC-USER-1001",
                                  "firstName":"Read",
                                  "lastName":"Only",
                                  "phoneNumber":"+966500008001"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void operatorAuthorityCanCreateBusinessResources() throws Exception {
        mockMvc.perform(post("/api/v1/drivers")
                        .with(jwt().authorities(new SimpleGrantedAuthority(FleetOpsAuthorities.OPERATOR)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference":"DRV-SEC-OPERATOR-1001",
                                  "firstName":"Fleet",
                                  "lastName":"Operator",
                                  "phoneNumber":"+966500008002"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalReference").value("DRV-SEC-OPERATOR-1001"));
    }

    @Test
    void integrationRecoveryRequiresAdminAuthority() throws Exception {
        String recoveryPath = "/api/v1/integration/operations/outbox/"
                + UUID.randomUUID()
                + "/requeue";

        mockMvc.perform(post(recoveryPath)
                        .with(jwt().authorities(new SimpleGrantedAuthority(FleetOpsAuthorities.OPERATOR))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post(recoveryPath)
                        .with(jwt().authorities(new SimpleGrantedAuthority(FleetOpsAuthorities.ADMIN))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INTEGRATION_RECOVERY_NOT_AVAILABLE"));
    }
}
