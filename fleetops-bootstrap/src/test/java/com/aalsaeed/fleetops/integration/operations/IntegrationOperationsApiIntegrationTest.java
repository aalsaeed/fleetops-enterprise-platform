package com.aalsaeed.fleetops.integration.operations;

import com.aalsaeed.fleetops.integration.infrastructure.messaging.rabbit.RabbitMqIntegrationTopology;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntegrationOperationsApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_integration_operations_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4-management-alpine");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private RabbitTemplate rabbitTemplate;

    @Test
    void exposesFailedAndStaleWorkAndAllowsExplicitRecovery() throws Exception {
        Instant now = Instant.now();
        UUID failedOutbox = UUID.randomUUID();
        UUID staleOutbox = UUID.randomUUID();
        UUID failedInbox = UUID.randomUUID();
        UUID staleInbox = UUID.randomUUID();

        insertOutbox(failedOutbox, "FAILED", 5, now.minusSeconds(300), null, "broker unavailable");
        insertOutbox(staleOutbox, "PUBLISHING", 1, now.minusSeconds(300), now.minusSeconds(120), null);
        insertInbox(failedInbox, "FAILED", 3, now.minusSeconds(300), null, "trip handoff failed");
        insertInbox(staleInbox, "PROCESSING", 1, now.minusSeconds(300), now.minusSeconds(120), null);

        rabbitTemplate.convertAndSend(
                RabbitMqIntegrationTopology.INTEGRATION_DEAD_LETTER_EXCHANGE,
                RabbitMqIntegrationTopology.ERP_SHIPMENT_DLQ_ROUTING_KEY,
                "poison-message");

        HttpResponse<String> snapshot = send("GET", "/api/v1/integration/operations");
        assertEquals(200, snapshot.statusCode());
        assertTrue(snapshot.body().contains("\"stalePublishing\":1"));
        assertTrue(snapshot.body().contains("\"staleProcessing\":1"));
        assertTrue(snapshot.body().contains("\"messageCount\":1"));
        assertTrue(snapshot.body().contains(failedOutbox.toString()));
        assertTrue(snapshot.body().contains(failedInbox.toString()));
        assertTrue(snapshot.body().contains("broker unavailable"));
        assertTrue(snapshot.body().contains("trip handoff failed"));

        assertEquals(202, send(
                "POST",
                "/api/v1/integration/operations/outbox/" + failedOutbox + "/requeue").statusCode());
        assertEquals(202, send(
                "POST",
                "/api/v1/integration/operations/inbox/" + failedInbox + "/requeue").statusCode());

        assertEquals("PENDING", outboxStatus(failedOutbox));
        assertEquals(0, outboxAttempts(failedOutbox));
        assertEquals("PENDING", inboxStatus(failedInbox));
        assertEquals(0, inboxAttempts(failedInbox));

        HttpResponse<String> conflict = send(
                "POST",
                "/api/v1/integration/operations/outbox/" + failedOutbox + "/requeue");
        assertEquals(409, conflict.statusCode());
        assertTrue(conflict.body().contains("INTEGRATION_RECOVERY_NOT_AVAILABLE"));
    }

    private void insertOutbox(
            UUID id,
            String status,
            int attempts,
            Instant createdAt,
            Instant claimedAt,
            String lastError) {
        jdbcTemplate.update(
                """
                insert into integration_outbox (
                    id, idempotency_key, event_type, aggregate_type, aggregate_id,
                    payload, status, attempts, created_at, claimed_at, last_error
                ) values (?, ?, 'ERP_SHIPMENT', 'SHIPMENT', ?, '{}', ?, ?, ?, ?, ?)
                """,
                id,
                "OPS-OUTBOX:" + id,
                "SHIP-" + id,
                status,
                attempts,
                Timestamp.from(createdAt),
                timestamp(claimedAt),
                lastError);
    }

    private void insertInbox(
            UUID id,
            String processingStatus,
            int attempts,
            Instant receivedAt,
            Instant claimedAt,
            String lastError) {
        jdbcTemplate.update(
                """
                insert into integration_inbox (
                    message_id, idempotency_key, event_type, aggregate_type, aggregate_id,
                    payload, received_at, last_received_at, duplicate_count,
                    processing_status, processing_attempts, processing_claimed_at,
                    processing_last_error
                ) values (?, ?, 'ERP_SHIPMENT', 'SHIPMENT', ?, '{}', ?, ?, 0, ?, ?, ?, ?)
                """,
                id,
                "OPS-INBOX:" + id,
                "SHIP-" + id,
                Timestamp.from(receivedAt),
                Timestamp.from(receivedAt),
                processingStatus,
                attempts,
                timestamp(claimedAt),
                lastError);
    }

    private String outboxStatus(UUID id) {
        return jdbcTemplate.queryForObject(
                "select status from integration_outbox where id = ?",
                String.class,
                id);
    }

    private int outboxAttempts(UUID id) {
        Integer value = jdbcTemplate.queryForObject(
                "select attempts from integration_outbox where id = ?",
                Integer.class,
                id);
        return value == null ? -1 : value;
    }

    private String inboxStatus(UUID id) {
        return jdbcTemplate.queryForObject(
                "select processing_status from integration_inbox where message_id = ?",
                String.class,
                id);
    }

    private int inboxAttempts(UUID id) {
        Integer value = jdbcTemplate.queryForObject(
                "select processing_attempts from integration_inbox where message_id = ?",
                Integer.class,
                id);
        return value == null ? -1 : value;
    }

    private HttpResponse<String> send(String method, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Accept", "application/json")
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
