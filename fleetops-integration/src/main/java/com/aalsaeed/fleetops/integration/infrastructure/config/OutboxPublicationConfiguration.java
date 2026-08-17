package com.aalsaeed.fleetops.integration.infrastructure.config;

import com.aalsaeed.fleetops.integration.application.port.in.PublishPendingOutboxUseCase;
import com.aalsaeed.fleetops.integration.application.port.in.RequeueFailedOutboxUseCase;
import com.aalsaeed.fleetops.integration.application.port.out.OutboxMessagePublisher;
import com.aalsaeed.fleetops.integration.application.port.out.OutboxPublicationStore;
import com.aalsaeed.fleetops.integration.application.service.OutboxPublicationService;
import com.aalsaeed.fleetops.integration.application.service.OutboxRecoveryService;
import com.aalsaeed.fleetops.integration.application.service.OutboxRetryPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableScheduling
public class OutboxPublicationConfiguration {

    @Bean
    PublishPendingOutboxUseCase publishPendingOutboxUseCase(
            OutboxPublicationStore publicationStore,
            OutboxMessagePublisher messagePublisher,
            @Value("${fleetops.integration.outbox.publisher.max-attempts:5}") int maxAttempts,
            @Value("${fleetops.integration.outbox.publisher.retry-initial-delay-ms:1000}") long retryInitialDelayMs,
            @Value("${fleetops.integration.outbox.publisher.retry-max-delay-ms:60000}") long retryMaxDelayMs,
            @Value("${fleetops.integration.outbox.publisher.stale-publishing-timeout-ms:60000}") long stalePublishingTimeoutMs) {
        OutboxRetryPolicy retryPolicy = new OutboxRetryPolicy(
                maxAttempts,
                positiveDuration(retryInitialDelayMs, "Outbox retry initial delay"),
                positiveDuration(retryMaxDelayMs, "Outbox retry maximum delay"),
                positiveDuration(stalePublishingTimeoutMs, "Outbox stale publishing timeout"));
        return new OutboxPublicationService(publicationStore, messagePublisher, retryPolicy, Clock.systemUTC());
    }

    @Bean
    RequeueFailedOutboxUseCase requeueFailedOutboxUseCase(OutboxPublicationStore publicationStore) {
        return new OutboxRecoveryService(publicationStore, Clock.systemUTC());
    }

    private static Duration positiveDuration(long millis, String name) {
        if (millis < 1) {
            throw new IllegalArgumentException(name + " must be at least 1 ms");
        }
        return Duration.ofMillis(millis);
    }
}
