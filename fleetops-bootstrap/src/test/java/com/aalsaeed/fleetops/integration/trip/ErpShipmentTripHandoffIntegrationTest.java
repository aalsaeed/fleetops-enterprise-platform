package com.aalsaeed.fleetops.integration.trip;

import com.aalsaeed.fleetops.integration.application.port.in.ProcessPendingInboxUseCase;
import com.aalsaeed.fleetops.integration.application.port.out.ErpShipmentPayloadSerializer;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentOperation;
import com.aalsaeed.fleetops.integration.infrastructure.messaging.rabbit.RabbitMqIntegrationTopology;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(properties = {
        "fleetops.integration.outbox.publisher.enabled=false",
        "fleetops.integration.inbox.consumer.enabled=true",
        "fleetops.integration.inbox.processor.enabled=false"
})
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ErpShipmentTripHandoffIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_erp_trip_handoff_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4-management-alpine");

    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ErpShipmentPayloadSerializer payloadSerializer;
    @Autowired private ProcessPendingInboxUseCase processPendingInboxUseCase;

    @Test
    void brokerDeliveryCreatesTripOnceAndLaterCancellationCancelsIt() throws Exception {
        ErpShipmentMessage upsert = shipment(
                "ERP-HANDOFF-MSG-1001",
                "SHIP-ERP-HANDOFF-1001",
                ErpShipmentOperation.UPSERT);

        send(upsert);
        send(upsert);
        awaitInbox(upsert.idempotencyKey().value(), 1);

        assertEquals("PENDING", inboxStatus(upsert.idempotencyKey().value()));
        assertEquals(1, processPendingInboxUseCase.processPending(10));
        assertEquals(1, tripCount(upsert.shipmentReference()));
        assertEquals("PLANNED", tripStatus(upsert.shipmentReference()));
        assertEquals("PROCESSED", inboxStatus(upsert.idempotencyKey().value()));
        assertEquals(0, processPendingInboxUseCase.processPending(10));
        assertEquals(1, tripCount(upsert.shipmentReference()));

        ErpShipmentMessage cancel = shipment(
                "ERP-HANDOFF-MSG-1002",
                upsert.shipmentReference(),
                ErpShipmentOperation.CANCEL);
        send(cancel);
        awaitInbox(cancel.idempotencyKey().value(), 0);

        assertEquals(1, processPendingInboxUseCase.processPending(10));
        assertEquals("CANCELLED", tripStatus(upsert.shipmentReference()));
        assertEquals("PROCESSED", inboxStatus(cancel.idempotencyKey().value()));
    }

    private ErpShipmentMessage shipment(
            String sourceMessageId,
            String shipmentReference,
            ErpShipmentOperation operation) {
        return ErpShipmentMessage.create(
                "ERP-DEMO",
                sourceMessageId,
                shipmentReference,
                operation,
                Instant.parse("2026-08-17T21:20:00Z"),
                "CORR-" + sourceMessageId);
    }

    private void send(ErpShipmentMessage shipmentMessage) {
        String payload = payloadSerializer.serialize(shipmentMessage);
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setMessageId(shipmentMessage.id().value().toString());
        properties.setHeader("x-idempotency-key", shipmentMessage.idempotencyKey().value());
        properties.setHeader("x-event-type", "ERP_SHIPMENT");
        properties.setHeader("x-aggregate-type", "SHIPMENT");
        properties.setHeader("x-aggregate-id", shipmentMessage.shipmentReference());
        rabbitTemplate.send(
                RabbitMqIntegrationTopology.INTEGRATION_EXCHANGE,
                RabbitMqIntegrationTopology.ERP_SHIPMENT_ROUTING_KEY,
                new Message(payload.getBytes(StandardCharsets.UTF_8), properties));
    }

    private void awaitInbox(String idempotencyKey, int expectedDuplicateCount) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from integration_inbox where idempotency_key = ?",
                    Integer.class,
                    idempotencyKey);
            if (count != null && count == 1) {
                Integer duplicateCount = jdbcTemplate.queryForObject(
                        "select duplicate_count from integration_inbox where idempotency_key = ?",
                        Integer.class,
                        idempotencyKey);
                if (duplicateCount != null && duplicateCount >= expectedDuplicateCount) {
                    return;
                }
            }
            Thread.sleep(100);
        }
        fail("Timed out waiting for ERP shipment Inbox receipt " + idempotencyKey);
    }

    private String inboxStatus(String idempotencyKey) {
        return jdbcTemplate.queryForObject(
                "select processing_status from integration_inbox where idempotency_key = ?",
                String.class,
                idempotencyKey);
    }

    private int tripCount(String externalReference) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from trips where external_reference = ?",
                Integer.class,
                externalReference);
        return count == null ? 0 : count;
    }

    private String tripStatus(String externalReference) {
        return jdbcTemplate.queryForObject(
                "select status from trips where external_reference = ?",
                String.class,
                externalReference);
    }
}
