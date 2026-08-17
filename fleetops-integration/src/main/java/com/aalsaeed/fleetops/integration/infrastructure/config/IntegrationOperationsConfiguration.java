package com.aalsaeed.fleetops.integration.infrastructure.config;

import com.aalsaeed.fleetops.integration.application.port.in.GetIntegrationOperationsUseCase;
import com.aalsaeed.fleetops.integration.application.port.out.DeadLetterQueueMetricsPort;
import com.aalsaeed.fleetops.integration.application.port.out.IntegrationOperationsStore;
import com.aalsaeed.fleetops.integration.application.service.IntegrationOperationsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class IntegrationOperationsConfiguration {

    @Bean
    GetIntegrationOperationsUseCase getIntegrationOperationsUseCase(
            IntegrationOperationsStore operationsStore,
            DeadLetterQueueMetricsPort deadLetterQueueMetricsPort,
            @Value("${fleetops.integration.outbox.publisher.stale-publishing-timeout-ms:60000}") long outboxStaleTimeoutMs,
            @Value("${fleetops.integration.inbox.processor.stale-processing-timeout-ms:60000}") long inboxStaleTimeoutMs,
            @Value("${fleetops.integration.operations.failure-limit:20}") int failureLimit) {
        return new IntegrationOperationsService(
                operationsStore,
                deadLetterQueueMetricsPort,
                positiveDuration(outboxStaleTimeoutMs, "Outbox stale publishing timeout"),
                positiveDuration(inboxStaleTimeoutMs, "Inbox stale processing timeout"),
                failureLimit,
                Clock.systemUTC());
    }

    private static Duration positiveDuration(long millis, String name) {
        if (millis < 1) {
            throw new IllegalArgumentException(name + " must be at least 1 ms");
        }
        return Duration.ofMillis(millis);
    }
}
