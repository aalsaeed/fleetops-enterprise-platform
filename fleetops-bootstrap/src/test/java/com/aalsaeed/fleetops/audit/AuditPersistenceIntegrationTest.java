package com.aalsaeed.fleetops.audit;

import com.aalsaeed.fleetops.audit.application.port.in.RecordAuditEventCommand;
import com.aalsaeed.fleetops.audit.application.port.in.RecordAuditEventUseCase;
import com.aalsaeed.fleetops.audit.domain.AuditEvent;
import com.aalsaeed.fleetops.audit.domain.AuditOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_audit_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Autowired
    private RecordAuditEventUseCase recordAuditEventUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appendsAuditRecordAndDatabaseRejectsMutation() {
        AuditEvent event = recordAuditEventUseCase.record(new RecordAuditEventCommand(
                "security-subject-1001",
                "Fleet Admin",
                Set.of("FLEETOPS_ADMIN", "FLEETOPS_OPERATOR"),
                "INTEGRATION_OUTBOX_REQUEUE",
                "INTEGRATION_OUTBOX",
                "message-1001",
                AuditOutcome.SUCCESS,
                "corr-audit-1001",
                Map.of("channel", "OUTBOX")));

        Integer eventCount = jdbcTemplate.queryForObject(
                "select count(*) from audit_events where id = ?",
                Integer.class,
                event.id().value());
        Integer authorityCount = jdbcTemplate.queryForObject(
                "select count(*) from audit_event_authorities where audit_event_id = ?",
                Integer.class,
                event.id().value());
        Integer metadataCount = jdbcTemplate.queryForObject(
                "select count(*) from audit_event_metadata where audit_event_id = ?",
                Integer.class,
                event.id().value());

        assertEquals(1, eventCount);
        assertEquals(2, authorityCount);
        assertEquals(1, metadataCount);
        assertEquals("SUCCESS", jdbcTemplate.queryForObject(
                "select outcome from audit_events where id = ?",
                String.class,
                event.id().value()));

        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "update audit_events set action = 'TAMPERED' where id = ?",
                event.id().value()));
        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "delete from audit_event_metadata where audit_event_id = ?",
                event.id().value()));
    }
}
