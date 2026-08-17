package com.aalsaeed.fleetops.integration.infrastructure.persistence.operations;

import com.aalsaeed.fleetops.integration.application.port.in.IntegrationOperationsSnapshot;
import com.aalsaeed.fleetops.integration.application.port.out.IntegrationOperationsStore;
import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class JdbcIntegrationOperationsStore implements IntegrationOperationsStore {

    private final JdbcTemplate jdbcTemplate;

    public JdbcIntegrationOperationsStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate cannot be null");
    }

    @Override
    public IntegrationOperationsSnapshot.OutboxSnapshot loadOutbox(Instant staleBefore, int failureLimit) {
        Objects.requireNonNull(staleBefore, "Outbox stale-before timestamp cannot be null");
        validateFailureLimit(failureLimit);

        Counts counts = jdbcTemplate.queryForObject(
                """
                select count(*) filter (where status = 'PENDING') as pending,
                       count(*) filter (where status = 'PUBLISHING') as active,
                       count(*) filter (where status = 'PUBLISHED') as completed,
                       count(*) filter (where status = 'FAILED') as failed,
                       count(*) filter (
                           where status = 'PUBLISHING'
                             and claimed_at is not null
                             and claimed_at < ?
                       ) as stale
                  from integration_outbox
                """,
                (rs, rowNum) -> new Counts(
                        rs.getLong("pending"),
                        rs.getLong("active"),
                        rs.getLong("completed"),
                        rs.getLong("failed"),
                        rs.getLong("stale")),
                Timestamp.from(staleBefore));

        List<IntegrationOperationsSnapshot.FailedMessage> failures = jdbcTemplate.query(
                """
                select id, aggregate_type, aggregate_id, attempts,
                       coalesce(last_error, 'Unknown publication failure') as last_error,
                       created_at
                  from integration_outbox
                 where status = 'FAILED'
                 order by created_at desc
                 limit ?
                """,
                (rs, rowNum) -> new IntegrationOperationsSnapshot.FailedMessage(
                        IntegrationMessageId.of(rs.getObject("id", UUID.class)),
                        rs.getString("aggregate_type"),
                        rs.getString("aggregate_id"),
                        rs.getInt("attempts"),
                        rs.getString("last_error"),
                        rs.getTimestamp("created_at").toInstant()),
                failureLimit);

        Counts value = Objects.requireNonNull(counts, "Outbox counts query returned no row");
        return new IntegrationOperationsSnapshot.OutboxSnapshot(
                value.pending(),
                value.active(),
                value.completed(),
                value.failed(),
                value.stale(),
                failures);
    }

    @Override
    public IntegrationOperationsSnapshot.InboxSnapshot loadInbox(Instant staleBefore, int failureLimit) {
        Objects.requireNonNull(staleBefore, "Inbox stale-before timestamp cannot be null");
        validateFailureLimit(failureLimit);

        Counts counts = jdbcTemplate.queryForObject(
                """
                select count(*) filter (where processing_status = 'PENDING') as pending,
                       count(*) filter (where processing_status = 'PROCESSING') as active,
                       count(*) filter (where processing_status = 'PROCESSED') as completed,
                       count(*) filter (where processing_status = 'FAILED') as failed,
                       count(*) filter (
                           where processing_status = 'PROCESSING'
                             and processing_claimed_at is not null
                             and processing_claimed_at < ?
                       ) as stale
                  from integration_inbox
                """,
                (rs, rowNum) -> new Counts(
                        rs.getLong("pending"),
                        rs.getLong("active"),
                        rs.getLong("completed"),
                        rs.getLong("failed"),
                        rs.getLong("stale")),
                Timestamp.from(staleBefore));

        List<IntegrationOperationsSnapshot.FailedMessage> failures = jdbcTemplate.query(
                """
                select message_id, aggregate_type, aggregate_id, processing_attempts,
                       coalesce(processing_last_error, 'Unknown processing failure') as last_error,
                       received_at
                  from integration_inbox
                 where processing_status = 'FAILED'
                 order by last_received_at desc
                 limit ?
                """,
                (rs, rowNum) -> new IntegrationOperationsSnapshot.FailedMessage(
                        IntegrationMessageId.of(rs.getObject("message_id", UUID.class)),
                        rs.getString("aggregate_type"),
                        rs.getString("aggregate_id"),
                        rs.getInt("processing_attempts"),
                        rs.getString("last_error"),
                        rs.getTimestamp("received_at").toInstant()),
                failureLimit);

        Counts value = Objects.requireNonNull(counts, "Inbox counts query returned no row");
        return new IntegrationOperationsSnapshot.InboxSnapshot(
                value.pending(),
                value.active(),
                value.completed(),
                value.failed(),
                value.stale(),
                failures);
    }

    private static void validateFailureLimit(int failureLimit) {
        if (failureLimit < 1 || failureLimit > 100) {
            throw new IllegalArgumentException("Failure limit must be between 1 and 100");
        }
    }

    private record Counts(long pending, long active, long completed, long failed, long stale) {
    }
}
