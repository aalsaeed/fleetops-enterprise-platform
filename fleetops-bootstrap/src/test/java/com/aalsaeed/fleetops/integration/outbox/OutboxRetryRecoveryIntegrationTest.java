package com.aalsaeed.fleetops.integration.outbox;

import com.aalsaeed.fleetops.integration.application.port.in.RequeueFailedOutboxUseCase;
import com.aalsaeed.fleetops.integration.application.port.in.StageErpShipmentUseCase;
import com.aalsaeed.fleetops.integration.application.port.out.OutboxPublicationStore;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentOperation;
import com.aalsaeed.fleetops.integration.domain.outbox.IntegrationOutboxMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class OutboxRetryRecoveryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-17T20:45:00Z");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_outbox_retry_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Autowired private StageErpShipmentUseCase stageErpShipmentUseCase;
    @Autowired private OutboxPublicationStore publicationStore;
    @Autowired private RequeueFailedOutboxUseCase requeueFailedOutboxUseCase;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void recoversStalePublishingAndHonorsRetrySchedule() {
        ErpShipmentMessage message = shipment("RETRY-MSG-1001", "SHIP-RETRY-1001");
        stageErpShipmentUseCase.stage(message);
        jdbcTemplate.update(
                "update integration_outbox set status = 'PUBLISHING', attempts = 1, claimed_at = ? where id = ?",
                NOW.minusSeconds(120), message.id().value());

        int recovered = publicationStore.recoverStalePublishing(NOW.minusSeconds(60), NOW);
        assertEquals(1, recovered);
        assertEquals("PENDING", jdbcTemplate.queryForObject(
                "select status from integration_outbox where id = ?", String.class, message.id().value()));
        assertNull(jdbcTemplate.queryForObject(
                "select claimed_at from integration_outbox where id = ?", Instant.class, message.id().value()));
        assertEquals(NOW, jdbcTemplate.queryForObject(
                "select next_attempt_at from integration_outbox where id = ?", Instant.class, message.id().value()));

        List<IntegrationOutboxMessage> firstClaim = publicationStore.claimPending(10, NOW);
        assertEquals(1, firstClaim.size());
        assertEquals(2, firstClaim.getFirst().attempts());

        Instant retryAt = NOW.plusSeconds(30);
        publicationStore.scheduleRetry(message.id(), "temporary RabbitMQ failure", retryAt);
        assertTrue(publicationStore.claimPending(10, NOW.plusSeconds(29)).isEmpty());
        List<IntegrationOutboxMessage> retryClaim = publicationStore.claimPending(10, NOW.plusSeconds(31));
        assertEquals(1, retryClaim.size());
        assertEquals(3, retryClaim.getFirst().attempts());
    }

    @Test
    void requeuesTerminalFailureForOperatorRecovery() {
        ErpShipmentMessage message = shipment("FAILED-MSG-1001", "SHIP-FAILED-1001");
        stageErpShipmentUseCase.stage(message);
        jdbcTemplate.update(
                "update integration_outbox set status = 'FAILED', attempts = 5, last_error = 'broker unavailable' where id = ?",
                message.id().value());

        assertTrue(requeueFailedOutboxUseCase.requeue(message.id()));
        assertEquals("PENDING", jdbcTemplate.queryForObject(
                "select status from integration_outbox where id = ?", String.class, message.id().value()));
        assertEquals(0, jdbcTemplate.queryForObject(
                "select attempts from integration_outbox where id = ?", Integer.class, message.id().value()));
        assertNotNull(jdbcTemplate.queryForObject(
                "select next_attempt_at from integration_outbox where id = ?", Instant.class, message.id().value()));
    }

    private static ErpShipmentMessage shipment(String sourceMessageId, String shipmentReference) {
        return ErpShipmentMessage.create(
                "ERP-DEMO", sourceMessageId, shipmentReference,
                ErpShipmentOperation.UPSERT, NOW.minusSeconds(300), "CORR-" + sourceMessageId);
    }
}
