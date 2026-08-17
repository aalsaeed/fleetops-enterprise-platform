package com.aalsaeed.fleetops.integration.operations.api;

import com.aalsaeed.fleetops.integration.application.port.in.GetIntegrationOperationsUseCase;
import com.aalsaeed.fleetops.integration.application.port.in.IntegrationOperationsSnapshot;
import com.aalsaeed.fleetops.integration.application.port.in.RequeueFailedInboxUseCase;
import com.aalsaeed.fleetops.integration.application.port.in.RequeueFailedOutboxUseCase;
import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integration/operations")
public final class IntegrationOperationsController {

    private final GetIntegrationOperationsUseCase getOperationsUseCase;
    private final RequeueFailedOutboxUseCase requeueFailedOutboxUseCase;
    private final RequeueFailedInboxUseCase requeueFailedInboxUseCase;

    public IntegrationOperationsController(
            GetIntegrationOperationsUseCase getOperationsUseCase,
            RequeueFailedOutboxUseCase requeueFailedOutboxUseCase,
            RequeueFailedInboxUseCase requeueFailedInboxUseCase) {
        this.getOperationsUseCase = Objects.requireNonNull(getOperationsUseCase, "Operations use case cannot be null");
        this.requeueFailedOutboxUseCase = Objects.requireNonNull(
                requeueFailedOutboxUseCase, "Outbox recovery use case cannot be null");
        this.requeueFailedInboxUseCase = Objects.requireNonNull(
                requeueFailedInboxUseCase, "Inbox recovery use case cannot be null");
    }

    @GetMapping
    public IntegrationOperationsSnapshot getSnapshot() {
        return getOperationsUseCase.getSnapshot();
    }

    @PostMapping("/outbox/{messageId}/requeue")
    public ResponseEntity<Void> requeueOutbox(@PathVariable UUID messageId) {
        if (!requeueFailedOutboxUseCase.requeue(IntegrationMessageId.of(messageId))) {
            throw new IntegrationRecoveryNotAvailableException("OUTBOX", messageId);
        }
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/inbox/{messageId}/requeue")
    public ResponseEntity<Void> requeueInbox(@PathVariable UUID messageId) {
        if (!requeueFailedInboxUseCase.requeue(IntegrationMessageId.of(messageId))) {
            throw new IntegrationRecoveryNotAvailableException("INBOX", messageId);
        }
        return ResponseEntity.accepted().build();
    }
}
