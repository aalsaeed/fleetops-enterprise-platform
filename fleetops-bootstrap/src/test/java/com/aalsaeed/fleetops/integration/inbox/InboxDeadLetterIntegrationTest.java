package com.aalsaeed.fleetops.integration.inbox;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "fleetops.integration.outbox.publisher.enabled=false",
        "fleetops.integration.inbox.consumer.enabled=true",
        "fleetops.integration.inbox.consumer.retry.max-attempts=3",
        "fleetops.integration.inbox.consumer.retry.initial-interval-ms=25",
        "fleetops.integration.inbox.consumer.retry.multiplier=1.0",
        "fleetops.integration.inbox.consumer.retry.max-interval-ms=25"
})
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InboxDeadLetterIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_inbox_dlq_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4-management-alpine");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void republishesPoisonMessageToDeadLetterQueueAfterRetriesAreExhausted() {
        String messageId = "00000000-0000-0000-0000-00000000d1e7";
        String poisonPayload = "{not-valid-json";

        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setMessageId(messageId);
        properties.setHeader("x-idempotency-key", "ERP-DEMO:POISON-1001");
        properties.setHeader("x-event-type", "ERP_SHIPMENT");
        properties.setHeader("x-aggregate-type", "SHIPMENT");
        properties.setHeader("x-aggregate-id", "SHIP-POISON-1001");

        rabbitTemplate.send(
                RabbitMqIntegrationTopology.INTEGRATION_EXCHANGE,
                RabbitMqIntegrationTopology.ERP_SHIPMENT_ROUTING_KEY,
                new Message(poisonPayload.getBytes(StandardCharsets.UTF_8), properties));

        Message deadLetter = rabbitTemplate.receive(RabbitMqIntegrationTopology.ERP_SHIPMENT_DLQ, 10_000);

        assertNotNull(deadLetter);
        assertEquals(messageId, deadLetter.getMessageProperties().getMessageId());
        assertEquals(poisonPayload, new String(deadLetter.getBody(), StandardCharsets.UTF_8));
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from integration_inbox",
                Integer.class));
    }
}
