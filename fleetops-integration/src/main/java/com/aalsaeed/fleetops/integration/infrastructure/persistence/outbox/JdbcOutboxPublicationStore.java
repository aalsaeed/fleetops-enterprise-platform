package com.aalsaeed.fleetops.integration.infrastructure.persistence.outbox;

import com.aalsaeed.fleetops.integration.application.port.out.OutboxPublicationStore;
import com.aalsaeed.fleetops.integration.domain.IdempotencyKey;
import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;
import com.aalsaeed.fleetops.integration.domain.outbox.IntegrationOutboxMessage;
import com.aalsaeed.fleetops.integration.domain.outbox.OutboxStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class JdbcOutboxPublicationStore implements OutboxPublicationStore {

    private static final String CLAIM_SQL = """
            with candidates as (
                select id
                  from integration_outbox
                 where status = 'PENDING'
                 order by created_at
                 limit ?
                 for update skip locked
            )
            update integration_outbox o
               set status = 'PUBLISHING',
                   attempts = o.attempts + 1,
                   claimed_at = ?,
                   last_error = null
              from candidates c
             where o.id = c.id
            returning o.id, o.idempotency_key, o.event_type, o.aggregate_type,
                      o.aggregate_id, o.payload, o.status, o.attempts,
                      o.created_at, o.published_at, o.last_error
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcOutboxPublicationStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate cannot be null");
    }

    @Override
    @Transactional
    public List<IntegrationOutboxMessage> claimPending(int limit, Instant claimedAt) {
        if (limit < 1) {
            throw new IllegalArgumentException("Claim limit must be at least 1");
        }
        Objects.requireNonNull(claimedAt, "Claim timestamp cannot be null");

        return jdbcTemplate.query(
                CLAIM_SQL,
                (rs, rowNum) -> IntegrationOutboxMessage.restore(
                        IntegrationMessageId.of(rs.getObject("id", UUID.class)),
                        new IdempotencyKey(rs.getString("idempotency_key")),
                        rs.getString("event_type"),
                        rs.getString("aggregate_type"),
                        rs.getString("aggregate_id"),
                        rs.getString("payload"),
                        OutboxStatus.valueOf(rs.getString("status")),
                        rs.getInt("attempts"),
                        rs.getTimestamp("created_at").toInstant(),
                        toInstant(rs.getTimestamp("published_at")),
                        rs.getString("last_error")),
                limit,
                Timestamp.from(claimedAt));
    }

    @Override
    @Transactional
    public void markPublished(IntegrationMessageId id, Instant publishedAt) {
        Objects.requireNonNull(id, "Message ID cannot be null");
        Objects.requireNonNull(publishedAt, "Published timestamp cannot be null");

        int updated = jdbcTemplate.update(
                """
                update integration_outbox
                   set status = 'PUBLISHED',
                       published_at = ?,
                       claimed_at = null,
                       last_error = null
                 where id = ? and status = 'PUBLISHING'
                """,
                Timestamp.from(publishedAt),
                id.value());
        requireSingleUpdate(id, updated, "published");
    }

    @Override
    @Transactional
    public void markFailed(IntegrationMessageId id, String error) {
        Objects.requireNonNull(id, "Message ID cannot be null");
        String normalizedError = normalizeError(error);

        int updated = jdbcTemplate.update(
                """
                update integration_outbox
                   set status = 'FAILED',
                       claimed_at = null,
                       last_error = ?
                 where id = ? and status = 'PUBLISHING'
                """,
                normalizedError,
                id.value());
        requireSingleUpdate(id, updated, "failed");
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static String normalizeError(String error) {
        String value = error == null || error.isBlank() ? "Unknown publication failure" : error.trim();
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    private static void requireSingleUpdate(IntegrationMessageId id, int updated, String targetState) {
        if (updated != 1) {
            throw new IllegalStateException(
                    "Could not mark outbox message " + id.value() + " as " + targetState);
        }
    }
}
