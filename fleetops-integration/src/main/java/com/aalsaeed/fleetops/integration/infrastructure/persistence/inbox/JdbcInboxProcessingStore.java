package com.aalsaeed.fleetops.integration.infrastructure.persistence.inbox;

import com.aalsaeed.fleetops.integration.application.port.out.ClaimedInboxMessage;
import com.aalsaeed.fleetops.integration.application.port.out.InboxProcessingStore;
import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class JdbcInboxProcessingStore implements InboxProcessingStore {

    private static final String CLAIM_SQL = """
            with candidates as (
                select message_id
                  from integration_inbox
                 where processing_status = 'PENDING'
                 order by received_at
                 limit ?
                 for update skip locked
            )
            update integration_inbox i
               set processing_status = 'PROCESSING',
                   processing_attempts = i.processing_attempts + 1,
                   processing_claimed_at = ?,
                   processing_last_error = null
              from candidates c
             where i.message_id = c.message_id
            returning i.message_id, i.payload, i.processing_attempts
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcInboxProcessingStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate cannot be null");
    }

    @Override
    @Transactional
    public List<ClaimedInboxMessage> claimPending(int limit, Instant claimedAt) {
        if (limit < 1) {
            throw new IllegalArgumentException("Claim limit must be at least 1");
        }
        Objects.requireNonNull(claimedAt, "Claim timestamp cannot be null");

        return jdbcTemplate.query(
                CLAIM_SQL,
                (rs, rowNum) -> new ClaimedInboxMessage(
                        IntegrationMessageId.of(rs.getObject("message_id", UUID.class)),
                        rs.getString("payload"),
                        rs.getInt("processing_attempts")),
                limit,
                Timestamp.from(claimedAt));
    }

    @Override
    @Transactional
    public void markProcessed(IntegrationMessageId id, Instant processedAt) {
        Objects.requireNonNull(id, "Message ID cannot be null");
        Objects.requireNonNull(processedAt, "Processed timestamp cannot be null");

        int updated = jdbcTemplate.update(
                """
                update integration_inbox
                   set processing_status = 'PROCESSED',
                       processing_claimed_at = null,
                       processed_at = ?,
                       processing_last_error = null
                 where message_id = ? and processing_status = 'PROCESSING'
                """,
                Timestamp.from(processedAt),
                id.value());
        requireSingleUpdate(id, updated, "processed");
    }

    @Override
    @Transactional
    public void markFailed(IntegrationMessageId id, String error) {
        Objects.requireNonNull(id, "Message ID cannot be null");

        int updated = jdbcTemplate.update(
                """
                update integration_inbox
                   set processing_status = 'FAILED',
                       processing_claimed_at = null,
                       processing_last_error = ?
                 where message_id = ? and processing_status = 'PROCESSING'
                """,
                normalizeError(error),
                id.value());
        requireSingleUpdate(id, updated, "failed");
    }

    @Override
    @Transactional
    public int recoverStaleProcessing(Instant staleBefore) {
        Objects.requireNonNull(staleBefore, "Stale threshold cannot be null");
        return jdbcTemplate.update(
                """
                update integration_inbox
                   set processing_status = 'PENDING',
                       processing_claimed_at = null,
                       processing_last_error = coalesce(processing_last_error, 'Recovered stale processing claim')
                 where processing_status = 'PROCESSING'
                   and processing_claimed_at < ?
                """,
                Timestamp.from(staleBefore));
    }

    @Override
    @Transactional
    public boolean requeueFailed(IntegrationMessageId id) {
        Objects.requireNonNull(id, "Message ID cannot be null");
        return jdbcTemplate.update(
                """
                update integration_inbox
                   set processing_status = 'PENDING',
                       processing_attempts = 0,
                       processing_claimed_at = null,
                       processed_at = null,
                       processing_last_error = null
                 where message_id = ? and processing_status = 'FAILED'
                """,
                id.value()) == 1;
    }

    private static String normalizeError(String error) {
        String value = error == null || error.isBlank() ? "Unknown inbox processing failure" : error.trim();
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    private static void requireSingleUpdate(IntegrationMessageId id, int updated, String targetState) {
        if (updated != 1) {
            throw new IllegalStateException(
                    "Could not mark inbox message " + id.value() + " as " + targetState);
        }
    }
}
