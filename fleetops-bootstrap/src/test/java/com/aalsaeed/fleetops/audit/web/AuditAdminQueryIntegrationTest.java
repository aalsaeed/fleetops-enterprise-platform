package com.aalsaeed.fleetops.audit.web;

import com.aalsaeed.fleetops.audit.application.port.in.AuditSearchQuery;
import com.aalsaeed.fleetops.audit.application.port.in.RecordAuditEventCommand;
import com.aalsaeed.fleetops.audit.application.port.in.RecordAuditEventUseCase;
import com.aalsaeed.fleetops.audit.domain.AuditOutcome;
import com.aalsaeed.fleetops.security.FleetOpsAuthorities;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.DirtiesContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "fleetops.security.test-permit-all=false")
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditAdminQueryIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_audit_query_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordAuditEventUseCase recordAuditEventUseCase;

    @Test
    void adminCanFilterAndPageImmutableAuditEvents() throws Exception {
        String actor = "query-actor-" + UUID.randomUUID();
        String firstCorrelation = "query-corr-" + UUID.randomUUID();
        String secondCorrelation = "query-corr-" + UUID.randomUUID();
        String thirdCorrelation = "query-corr-" + UUID.randomUUID();
        Instant from = Instant.now().minusSeconds(30);

        record(actor, "TRIP_CREATE", "TRIP", "trip-1001", AuditOutcome.SUCCESS, firstCorrelation);
        record(actor, "TRIP_CANCEL", "TRIP", "trip-1001", AuditOutcome.FAILURE, secondCorrelation);
        record(actor, "DRIVER_CREATE", "DRIVER", "driver-1001", AuditOutcome.SUCCESS, thirdCorrelation);

        Instant to = Instant.now().plusSeconds(30);

        mockMvc.perform(get("/api/v1/audit/events")
                        .with(adminJwt())
                        .param("actor", actor)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("offset", "0")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.limit").value(2))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.items.length()").value(2));

        mockMvc.perform(get("/api/v1/audit/events")
                        .with(adminJwt())
                        .param("actor", actor)
                        .param("offset", "2")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.offset").value(2))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.items.length()").value(1));

        mockMvc.perform(get("/api/v1/audit/events")
                        .with(adminJwt())
                        .param("actor", actor)
                        .param("action", "TRIP_CANCEL")
                        .param("resourceType", "TRIP")
                        .param("resourceId", "trip-1001")
                        .param("outcome", "FAILURE")
                        .param("correlationId", secondCorrelation))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].actorSubject").value(actor))
                .andExpect(jsonPath("$.items[0].action").value("TRIP_CANCEL"))
                .andExpect(jsonPath("$.items[0].resourceType").value("TRIP"))
                .andExpect(jsonPath("$.items[0].resourceId").value("trip-1001"))
                .andExpect(jsonPath("$.items[0].outcome").value("FAILURE"))
                .andExpect(jsonPath("$.items[0].correlationId").value(secondCorrelation));
    }

    @Test
    void auditQueryIsAdminOnlyAndPaginationIsBounded() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/audit/events")
                        .with(operatorJwt()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/audit/events")
                        .with(adminJwt())
                        .param("limit", Integer.toString(AuditSearchQuery.MAX_LIMIT + 1)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/audit/events")
                        .with(adminJwt())
                        .param("offset", "-1"))
                .andExpect(status().isBadRequest());
    }

    private void record(
            String actor,
            String action,
            String resourceType,
            String resourceId,
            AuditOutcome outcome,
            String correlationId) {

        recordAuditEventUseCase.record(new RecordAuditEventCommand(
                actor,
                "Audit Query Test",
                Set.of(FleetOpsAuthorities.ADMIN),
                action,
                resourceType,
                resourceId,
                outcome,
                correlationId,
                Map.of("test", "audit-query")));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt()
                .jwt(token -> token.subject("audit-admin"))
                .authorities(new SimpleGrantedAuthority(FleetOpsAuthorities.ADMIN));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor operatorJwt() {
        return jwt()
                .jwt(token -> token.subject("audit-operator"))
                .authorities(new SimpleGrantedAuthority(FleetOpsAuthorities.OPERATOR));
    }
}
