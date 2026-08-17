package com.aalsaeed.fleetops.integration.infrastructure.messaging.rabbit;

import com.aalsaeed.fleetops.integration.application.port.in.PublishPendingOutboxUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "fleetops.integration.outbox.publisher",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public final class OutboxPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherScheduler.class);

    private final PublishPendingOutboxUseCase publishPendingOutboxUseCase;
    private final int batchSize;

    public OutboxPublisherScheduler(
            PublishPendingOutboxUseCase publishPendingOutboxUseCase,
            @Value("${fleetops.integration.outbox.publisher.batch-size:25}") int batchSize) {
        this.publishPendingOutboxUseCase = publishPendingOutboxUseCase;
        if (batchSize < 1) {
            throw new IllegalArgumentException("Outbox publisher batch size must be at least 1");
        }
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${fleetops.integration.outbox.publisher.fixed-delay-ms:1000}")
    public void publishPending() {
        int published = publishPendingOutboxUseCase.publishPending(batchSize);
        if (published > 0) {
            log.debug("Published {} outbox message(s)", published);
        }
    }
}
