package com.aalsaeed.fleetops.integration.inbox;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(properties = {
        "fleetops.integration.outbox.publisher.enabled=false",
        "fleetops.integration.inbox.consumer.enabled=true"
})
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InboxRabbitMqConsumerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_inbox_consumer_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4-management-alpine");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ErpShipmentPayloadSerializer payloadSerializer;

    @Test
    void storesOneInboxReceiptAndCountsDuplicateBrokerDelivery() throws Exception {
        ErpShipmentMessage shipmentMessage = ErpShipmentMessage.create(
                "ERP-DEMO",
                "INBOX-MSG-1001",
                "SHIP-INBOX-1001",
                ErpShipmentOperation.UPSERT,
                Instant.parse("2026-08-17T20:25:00Z"),
                "CORR-INBOX-1001");

        Message amqpMessage = toAmqpMessage(shipmentMessage);
        rabbitTemplate.send(
                RabbitMqIntegrationTopology.INTEGRATION_EXCHANGE,
                RabbitMqIntegrationTopology.ERP_SHIPMENT_ROUTING_KEY,
                amqpMessage);
        rabbitTemplate.send(
                RabbitMqIntegrationTopology.INTEGRATION_EXCHANGE,
                RabbitMqIntegrationTopology.ERP_SHIPMENT_ROUTING_KEY,
                toAmqpMessage(shipmentMessage));

        awaitDuplicateReceipt(shipmentMessage.idempotencyKey().value());

        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from integration_inbox where idempotency_key = ?",
                Integer.class,
                shipmentMessage.idempotencyKey().value()));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select duplicate_count from integration_inbox where idempotency_key = ?",
                Integer.class,
                shipmentMessage.idempotencyKey().value()));
        assertEquals("ERP_SHIPMENT", jdbcTemplate.queryForObject(
                "select event_type from integration_inbox where idempotency_key = ?",
                String.class,
                shipmentMessage.idempotencyKey().value()));
        assertEquals("SHIP-INBOX-1001", jdbcTemplate.queryForObject(
                "select aggregate_id from integration_inbox where idempotency_key = ?",
                String.class,
                shipmentMessage.idempotencyKey().value()));

        Instant receivedAt = jdbcTemplate.queryForObject(
                "select received_at from integration_inbox where idempotency_key = ?",
                Instant.class,
                shipmentMessage.idempotencyKey().value());
        Instant lastReceivedAt = jdbcTemplate.queryForObject(
                "select last_received_at from integration_inbox where idempotency_key = ?",
                Instant.class,
                shipmentMessage.idempotencyKey().value());
        assertNotNull(receivedAt);
        assertNotNull(lastReceivedAt);
        assertTrue(!lastReceivedAt.isBefore(receivedAt));
    }

    private Message toAmqpMessage(ErpShipmentMessage shipmentMessage) {
        String payload = payloadSerializer.serialize(shipmentMessage);
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setMessageId(shipmentMessage.id().value().toString());
        properties.setHeader("x-idempotency-key", shipmentMessage.idempotencyKey().value());
        properties.setHeader("x-event-type", "ERP_SHIPMENT");
        properties.setHeader("x-aggregate-type", "SHIPMENT");
        properties.setHeader("x-aggregate-id", shipmentMessage.shipmentReference());
        return new Message(payload.getBytes(StandardCharsets.UTF_8), properties);
    }

    private void awaitDuplicateReceipt(String idempotencyKey) throws InterruptedException {
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
                if (duplicateCount != null && duplicateCount >= 1) {
                    return;
                }
            }
            Thread.sleep(100);
        }
        fail("Timed out waiting for idempotent inbox consumer");
    }
}
