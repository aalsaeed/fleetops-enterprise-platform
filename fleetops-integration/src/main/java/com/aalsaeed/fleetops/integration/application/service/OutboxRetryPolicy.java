package com.aalsaeed.fleetops.integration.application.service;

import java.time.Duration;
import java.util.Objects;

public record OutboxRetryPolicy(
        int maxAttempts,
        Duration initialDelay,
        Duration maxDelay,
        Duration stalePublishingTimeout) {

    public OutboxRetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Outbox max attempts must be at least 1");
        }
        Objects.requireNonNull(initialDelay, "Initial retry delay cannot be null");
        Objects.requireNonNull(maxDelay, "Maximum retry delay cannot be null");
        Objects.requireNonNull(stalePublishingTimeout, "Stale publishing timeout cannot be null");
        if (initialDelay.isNegative() || initialDelay.isZero()) {
            throw new IllegalArgumentException("Initial retry delay must be positive");
        }
        if (maxDelay.compareTo(initialDelay) < 0) {
            throw new IllegalArgumentException("Maximum retry delay cannot be less than the initial delay");
        }
        if (stalePublishingTimeout.isNegative() || stalePublishingTimeout.isZero()) {
            throw new IllegalArgumentException("Stale publishing timeout must be positive");
        }
    }

    public Duration delayAfterAttempt(int attemptNumber) {
        if (attemptNumber < 1) {
            throw new IllegalArgumentException("Attempt number must be at least 1");
        }

        Duration delay = initialDelay;
        for (int attempt = 1; attempt < attemptNumber && delay.compareTo(maxDelay) < 0; attempt++) {
            Duration doubled;
            try {
                doubled = delay.multipliedBy(2);
            } catch (ArithmeticException overflow) {
                return maxDelay;
            }
            delay = doubled.compareTo(maxDelay) > 0 ? maxDelay : doubled;
        }
        return delay;
    }
}
