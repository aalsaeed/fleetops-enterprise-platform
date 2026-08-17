package com.aalsaeed.fleetops.integration.infrastructure.persistence.inbox;

import com.aalsaeed.fleetops.integration.application.port.in.AcceptErpShipmentDeliveryCommand;
import com.aalsaeed.fleetops.integration.application.port.in.AcceptErpShipmentDeliveryResult;
import com.aalsaeed.fleetops.integration.application.port.out.IntegrationInboxStore;
import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;

@Repository
public class JdbcIntegrationInboxStore implements IntegrationInboxStore {

    private final JdbcTemplate jdbcTemplate;

    public JdbcIntegrationInboxStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate cannot be null");
    }

    @Override
    @Transactional
    public AcceptErpShipmentDeliveryResult accept(AcceptErpShipmentDeliveryCommand command) {
        Objects.requireNonNull(command, "ERP shipment delivery command cannot be null");

        int inserted = jdbcTemplate.update(
                """
                insert into integration_inbox (
                    message_id,
                    idempotency_key,
                    event_type,
                    aggregate_type,
                    aggregate_id,
                    payload,
                    received_at,
                    last_received_at,
                    duplicate_count
                ) values (?, ?, ?, ?, ?, ?, ?, ?, 0)
                on conflict (idempotency_key) do nothing
                """,
                command.message().id().value(),
                command.message().idempotencyKey().value(),
                command.eventType(),
                command.aggregateType(),
                command.aggregateId(),
                command.payload(),
                Timestamp.from(command.receivedAt()),
                Timestamp.from(command.receivedAt()));

        if (inserted == 1) {
            return new AcceptErpShipmentDeliveryResult(
                    command.message().id(),
                    command.message().idempotencyKey(),
                    false);
        }

        int updated = jdbcTemplate.update(
                """
                update integration_inbox
                   set duplicate_count = duplicate_count + 1,
                       last_received_at = ?
                 where idempotency_key = ?
                """,
                Timestamp.from(command.receivedAt()),
                command.message().idempotencyKey().value());

        if (updated != 1) {
            throw new IllegalStateException(
                    "Could not update duplicate inbox receipt for " + command.message().idempotencyKey().value());
        }

        UUID existingMessageId = jdbcTemplate.queryForObject(
                "select message_id from integration_inbox where idempotency_key = ?",
                UUID.class,
                command.message().idempotencyKey().value());

        return new AcceptErpShipmentDeliveryResult(
                IntegrationMessageId.of(Objects.requireNonNull(existingMessageId)),
                command.message().idempotencyKey(),
                true);
    }
}
