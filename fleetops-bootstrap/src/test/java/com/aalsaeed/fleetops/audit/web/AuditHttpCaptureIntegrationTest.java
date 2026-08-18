package com.aalsaeed.fleetops.audit.web;

import com.aalsaeed.fleetops.security.FleetOpsAuthorities;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "fleetops.security.test-permit-all=false")
@AutoConfigureMockMvc
@Testcontainers
class AuditHttpCaptureIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_audit_http_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void capturesSuccessfulAndFailedAuthenticatedBusinessWrites() throws Exception {
        String externalReference = "DRV-AUD-" + UUID.randomUUID();
        String createCorrelation = "corr-driver-create-" + UUID.randomUUID();

        MvcResult createResult = mockMvc.perform(post("/api/v1/drivers")
                        .with(operatorJwt())
                        .header(AuditCorrelationFilter.HEADER_NAME, createCorrelation)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference":"%s",
                                  "firstName":"Audit",
                                  "lastName":"Operator",
                                  "phoneNumber":"+966500008811"
                                }
                                """.formatted(externalReference)))
                .andExpect(status().isCreated())
                .andExpect(header().string(AuditCorrelationFilter.HEADER_NAME, createCorrelation))
                .andReturn();

        String location = createResult.getResponse().getHeader("Location");
        assertThat(location).isNotBlank();
        String driverId = location.substring(location.lastIndexOf('/') + 1);

        Map<String, Object> createAudit = auditByCorrelation(createCorrelation);
        assertAudit(createAudit, "DRIVER_CREATE", "DRIVER", driverId, "SUCCESS", "operator-subject");
        assertThat(authorityCount(createAudit.get("id"), FleetOpsAuthorities.OPERATOR)).isEqualTo(1);

        String statusCorrelation = "corr-driver-status-" + UUID.randomUUID();
        mockMvc.perform(patch("/api/v1/drivers/{id}/status", driverId)
                        .with(operatorJwt())
                        .header(AuditCorrelationFilter.HEADER_NAME, statusCorrelation)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(AuditCorrelationFilter.HEADER_NAME, statusCorrelation));

        assertAudit(
                auditByCorrelation(statusCorrelation),
                "DRIVER_STATUS_CHANGE",
                "DRIVER",
                driverId,
                "SUCCESS",
                "operator-subject");

        String failureCorrelation = "corr-driver-failure-" + UUID.randomUUID();
        mockMvc.perform(post("/api/v1/drivers")
                        .with(operatorJwt())
                        .header(AuditCorrelationFilter.HEADER_NAME, failureCorrelation)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference":"%s",
                                  "firstName":"Duplicate",
                                  "lastName":"Driver",
                                  "phoneNumber":"+966500008812"
                                }
                                """.formatted(externalReference)))
                .andExpect(status().isConflict());

        assertAudit(
                auditByCorrelation(failureCorrelation),
                "DRIVER_CREATE",
                "DRIVER",
                null,
                "FAILURE",
                "operator-subject");
    }

    @Test
    void capturesFailedAdministrativeIntegrationRecoveryAttempt() throws Exception {
        UUID messageId = UUID.randomUUID();
        String correlationId = "corr-outbox-requeue-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/integration/operations/outbox/{messageId}/requeue", messageId)
                        .with(adminJwt())
                        .header(AuditCorrelationFilter.HEADER_NAME, correlationId))
                .andExpect(status().isConflict())
                .andExpect(header().string(AuditCorrelationFilter.HEADER_NAME, correlationId));

        assertAudit(
                auditByCorrelation(correlationId),
                "INTEGRATION_OUTBOX_REQUEUE",
                "INTEGRATION_OUTBOX",
                messageId.toString(),
                "FAILURE",
                "admin-subject");
    }

    private Map<String, Object> auditByCorrelation(String correlationId) {
        return jdbcTemplate.queryForMap("""
                select id, actor_subject, actor_display_name, action, resource_type, resource_id, outcome, correlation_id
                from audit_events
                where correlation_id = ?
                """, correlationId);
    }

    private int authorityCount(Object auditEventId, String authority) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from audit_event_authorities
                where audit_event_id = ? and authority = ?
                """, Integer.class, auditEventId, authority);
        return count == null ? 0 : count;
    }

    private static void assertAudit(
            Map<String, Object> row,
            String action,
            String resourceType,
            String resourceId,
            String outcome,
            String actorSubject) {

        assertThat(row.get("action")).isEqualTo(action);
        assertThat(row.get("resource_type")).isEqualTo(resourceType);
        assertThat(row.get("resource_id")).isEqualTo(resourceId);
        assertThat(row.get("outcome")).isEqualTo(outcome);
        assertThat(row.get("actor_subject")).isEqualTo(actorSubject);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor operatorJwt() {
        return jwt()
                .jwt(token -> token.subject("operator-subject").claim("preferred_username", "fleet-operator"))
                .authorities(new SimpleGrantedAuthority(FleetOpsAuthorities.OPERATOR));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt()
                .jwt(token -> token.subject("admin-subject").claim("preferred_username", "fleet-admin"))
                .authorities(new SimpleGrantedAuthority(FleetOpsAuthorities.ADMIN));
    }
}
