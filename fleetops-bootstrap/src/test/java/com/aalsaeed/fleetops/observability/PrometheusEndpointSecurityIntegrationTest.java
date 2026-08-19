package com.aalsaeed.fleetops.observability;

import com.aalsaeed.fleetops.integration.application.port.in.GetIntegrationOperationsUseCase;
import com.aalsaeed.fleetops.integration.application.port.in.IntegrationOperationsSnapshot;
import com.aalsaeed.fleetops.security.FleetOpsAuthorities;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "fleetops.security.test-permit-all=false")
@AutoConfigureMockMvc
@Testcontainers
@Import(PrometheusEndpointSecurityIntegrationTest.TestOperationsConfiguration.class)
class PrometheusEndpointSecurityIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_prometheus_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousRequestRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void userAuthorityCannotScrapeOperationalMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                        .with(jwt().authorities(new SimpleGrantedAuthority(FleetOpsAuthorities.USER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void operatorAuthorityCanScrapePrometheusMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                        .with(jwt().authorities(new SimpleGrantedAuthority(FleetOpsAuthorities.OPERATOR))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("# HELP")))
                .andExpect(content().string(containsString("jvm_")))
                .andExpect(content().string(containsString("fleetops_integration_outbox_messages")))
                .andExpect(content().string(containsString("fleetops_integration_inbox_messages")))
                .andExpect(content().string(containsString("fleetops_integration_dlq_messages")));
    }

    @Test
    void adminAuthorityCanScrapePrometheusMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                        .with(jwt().authorities(new SimpleGrantedAuthority(FleetOpsAuthorities.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("# HELP")));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestOperationsConfiguration {

        @Bean
        @Primary
        GetIntegrationOperationsUseCase testIntegrationOperationsUseCase() {
            return () -> new IntegrationOperationsSnapshot(
                    Instant.parse("2026-08-19T00:00:00Z"),
                    new IntegrationOperationsSnapshot.OutboxSnapshot(2, 1, 12, 3, 1, List.of()),
                    new IntegrationOperationsSnapshot.InboxSnapshot(4, 1, 10, 2, 1, List.of()),
                    new IntegrationOperationsSnapshot.DeadLetterQueueSnapshot(true, 5));
        }
    }
}
