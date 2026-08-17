package com.aalsaeed.fleetops.integration.application.port.in;

import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record IntegrationOperationsSnapshot(
        Instant capturedAt,
        OutboxSnapshot outbox,
        InboxSnapshot inbox,
        DeadLetterQueueSnapshot deadLetterQueue) {

    public IntegrationOperationsSnapshot {
        Objects.requireNonNull(capturedAt, "Captured-at timestamp cannot be null");
        Objects.requireNonNull(outbox, "Outbox snapshot cannot be null");
        Objects.requireNonNull(inbox, "Inbox snapshot cannot be null");
        Objects.requireNonNull(deadLetterQueue, "Dead-letter queue snapshot cannot be null");
    }

    public record OutboxSnapshot(
            long pending,
            long publishing,
            long published,
            long failed,
            long stalePublishing,
            List<FailedMessage> failures) {

        public OutboxSnapshot {
            failures = List.copyOf(Objects.requireNonNull(failures, "Outbox failures cannot be null"));
        }
    }

    public record InboxSnapshot(
            long pending,
            long processing,
            long processed,
            long failed,
            long staleProcessing,
            List<FailedMessage> failures) {

        public InboxSnapshot {
            failures = List.copyOf(Objects.requireNonNull(failures, "Inbox failures cannot be null"));
        }
    }

    public record FailedMessage(
            IntegrationMessageId messageId,
            String aggregateType,
            String aggregateId,
            int attempts,
            String lastError,
            Instant occurredAt) {

        public FailedMessage {
            Objects.requireNonNull(messageId, "Message ID cannot be null");
            aggregateType = requireText(aggregateType, "Aggregate type");
            aggregateId = requireText(aggregateId, "Aggregate ID");
            if (attempts < 0) {
                throw new IllegalArgumentException("Attempts cannot be negative");
            }
            lastError = requireText(lastError, "Last error");
            Objects.requireNonNull(occurredAt, "Occurred-at timestamp cannot be null");
        }
    }

    public record DeadLetterQueueSnapshot(boolean available, long messageCount) {

        public DeadLetterQueueSnapshot {
            if (messageCount < 0) {
                throw new IllegalArgumentException("Dead-letter queue message count cannot be negative");
            }
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value.trim();
    }
}
