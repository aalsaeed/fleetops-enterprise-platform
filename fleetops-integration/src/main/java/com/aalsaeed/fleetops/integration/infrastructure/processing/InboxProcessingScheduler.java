package com.aalsaeed.fleetops.integration.infrastructure.processing;

import com.aalsaeed.fleetops.integration.application.port.in.ProcessPendingInboxUseCase;
import com.aalsaeed.fleetops.integration.application.port.in.RecoverStaleInboxUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(
        prefix = "fleetops.integration.inbox.processor",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public final class InboxProcessingScheduler {

    private final ProcessPendingInboxUseCase processPendingInboxUseCase;
    private final RecoverStaleInboxUseCase recoverStaleInboxUseCase;
    private final int batchSize;
    private final Duration staleProcessingTimeout;

    public InboxProcessingScheduler(
            ProcessPendingInboxUseCase processPendingInboxUseCase,
            RecoverStaleInboxUseCase recoverStaleInboxUseCase,
            @Value("${fleetops.integration.inbox.processor.batch-size:25}") int batchSize,
            @Value("${fleetops.integration.inbox.processor.stale-processing-timeout-ms:60000}") long staleProcessingTimeoutMs) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("Inbox processor batch size must be at least 1");
        }
        if (staleProcessingTimeoutMs < 1) {
            throw new IllegalArgumentException("Inbox stale processing timeout must be at least 1 ms");
        }
        this.processPendingInboxUseCase = processPendingInboxUseCase;
        this.recoverStaleInboxUseCase = recoverStaleInboxUseCase;
        this.batchSize = batchSize;
        this.staleProcessingTimeout = Duration.ofMillis(staleProcessingTimeoutMs);
    }

    @Scheduled(fixedDelayString = "${fleetops.integration.inbox.processor.fixed-delay-ms:1000}")
    public void process() {
        recoverStaleInboxUseCase.recoverStale(staleProcessingTimeout);
        processPendingInboxUseCase.processPending(batchSize);
    }
}
