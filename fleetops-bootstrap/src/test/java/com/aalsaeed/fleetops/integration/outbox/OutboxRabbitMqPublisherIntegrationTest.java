package com.aalsaeed.fleetops.integration.outbox;

import com.aalsaeed.fleetops.integration.application.port.in.PublishPendingOutboxUseCase;
import com.aalsaeed.fleetops.integration.application.port.in.StageErpShipmentUseCase;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentOperation;
import com.aalsaeed.fleetops.integration.infrastructure.messaging.rabbit.RabbitMqIntegrationTopology;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "fleetops.integration.outbox.publisher.enabled=false")
@Testcontainers
class OutboxRabbitMqPublisherIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_rabbit_publisher_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4-management-alpine");

    @Autowired
    private StageErpShipmentUseCase stageErpShipmentUseCase;

    @Autowired
    private PublishPendingOutboxUseCase publishPendingOutboxUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void publishesPendingMessageAndMarksItPublishedAfterBrokerConfirm() {
        ErpShipmentMessage message = ErpShipmentMessage.create(
                "ERP-DEMO",
                "RABBIT-MSG-1001",
                "SHIP-RABBIT-1001",
                ErpShipmentOperation.UPSERT,
                Instant.parse("2026-08-17T19:55:00Z"),
                "CORR-RABBIT-1001");

        stageErpShipmentUseCase.stage(message);

        int published = publishPendingOutboxUseCase.publishPending(10);

        assertEquals(1, published);
        assertEquals("PUBLISHED", jdbcTemplate.queryForObject(
                "select status from integration_outbox where id = ?",
                String.class,
                message.id().value()));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select attempts from integration_outbox where id = ?",
                Integer.class,
                message.id().value()));
        assertNotNull(jdbcTemplate.queryForObject(
                "select published_at from integration_outbox where id = ?",
                Timestamp.class,
                message.id().value()));
        assertNull(jdbcTemplate.queryForObject(
                "select claimed_at from integration_outbox where id = ?",
                Timestamp.class,
                message.id().value()));

        Message delivered = rabbitTemplate.receive(RabbitMqIntegrationTopology.ERP_SHIPMENT_QUEUE, 5000);
        assertNotNull(delivered);
        String body = new String(delivered.getBody(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"shipmentReference\":\"SHIP-RABBIT-1001\""));
        assertEquals(
                "ERP-DEMO:RABBIT-MSG-1001",
                delivered.getMessageProperties().getHeader("x-idempotency-key"));
    }
}
