package com.aalsaeed.fleetops.integration.outbox;

import com.aalsaeed.fleetops.integration.application.port.in.StageErpShipmentResult;
import com.aalsaeed.fleetops.integration.application.port.in.StageErpShipmentUseCase;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentOperation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class ErpShipmentOutboxIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_integration_outbox_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Autowired
    private StageErpShipmentUseCase stageErpShipmentUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void stagesReceiptAndOutboxOnceForDuplicateSourceDelivery() {
        ErpShipmentMessage firstDelivery = ErpShipmentMessage.create(
                "erp-demo",
                "OUTBOX-MSG-1001",
                "SHIP-OUTBOX-1001",
                ErpShipmentOperation.UPSERT,
                Instant.parse("2026-08-17T10:15:30Z"),
                "CORR-OUTBOX-1001");

        ErpShipmentMessage duplicateDelivery = ErpShipmentMessage.create(
                "ERP-DEMO",
                "OUTBOX-MSG-1001",
                "SHIP-OUTBOX-1001",
                ErpShipmentOperation.UPSERT,
                Instant.parse("2026-08-17T10:15:30Z"),
                "CORR-OUTBOX-1001");

        StageErpShipmentResult first = stageErpShipmentUseCase.stage(firstDelivery);
        StageErpShipmentResult duplicate = stageErpShipmentUseCase.stage(duplicateDelivery);

        assertFalse(first.duplicate());
        assertTrue(duplicate.duplicate());
        assertEquals(first.messageId(), duplicate.messageId());
        assertEquals("ERP-DEMO:OUTBOX-MSG-1001", first.idempotencyKey().value());

        Integer receiptCount = jdbcTemplate.queryForObject(
                "select count(*) from erp_shipment_receipts where idempotency_key = ?",
                Integer.class,
                first.idempotencyKey().value());
        Integer outboxCount = jdbcTemplate.queryForObject(
                "select count(*) from integration_outbox where idempotency_key = ?",
                Integer.class,
                first.idempotencyKey().value());
        String status = jdbcTemplate.queryForObject(
                "select status from integration_outbox where idempotency_key = ?",
                String.class,
                first.idempotencyKey().value());
        Integer attempts = jdbcTemplate.queryForObject(
                "select attempts from integration_outbox where idempotency_key = ?",
                Integer.class,
                first.idempotencyKey().value());
        String payload = jdbcTemplate.queryForObject(
                "select payload from integration_outbox where idempotency_key = ?",
                String.class,
                first.idempotencyKey().value());

        assertEquals(1, receiptCount);
        assertEquals(1, outboxCount);
        assertEquals("PENDING", status);
        assertEquals(0, attempts);
        assertTrue(payload.contains("\"sourceSystem\":\"ERP-DEMO\""));
        assertTrue(payload.contains("\"shipmentReference\":\"SHIP-OUTBOX-1001\""));
        assertTrue(payload.contains("\"idempotencyKey\":\"ERP-DEMO:OUTBOX-MSG-1001\""));
    }
}
