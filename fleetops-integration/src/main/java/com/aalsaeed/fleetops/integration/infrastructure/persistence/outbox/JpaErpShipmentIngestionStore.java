package com.aalsaeed.fleetops.integration.infrastructure.persistence.outbox;

import com.aalsaeed.fleetops.integration.application.port.in.StageErpShipmentResult;
import com.aalsaeed.fleetops.integration.application.port.out.ErpShipmentIngestionStore;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import com.aalsaeed.fleetops.integration.domain.IdempotencyKey;
import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;
import com.aalsaeed.fleetops.integration.domain.outbox.IntegrationOutboxMessage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Component
public class JpaErpShipmentIngestionStore implements ErpShipmentIngestionStore {

    private final SpringDataErpShipmentReceiptRepository receiptRepository;
    private final SpringDataIntegrationOutboxRepository outboxRepository;

    public JpaErpShipmentIngestionStore(
            SpringDataErpShipmentReceiptRepository receiptRepository,
            SpringDataIntegrationOutboxRepository outboxRepository) {
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "Receipt repository cannot be null");
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "Outbox repository cannot be null");
    }

    @Override
    @Transactional
    public StageErpShipmentResult stage(ErpShipmentMessage message, String payload) {
        Objects.requireNonNull(message, "ERP shipment message cannot be null");
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Payload cannot be blank");
        }

        return receiptRepository.findByIdempotencyKey(message.idempotencyKey().value())
                .map(existing -> new StageErpShipmentResult(
                        IntegrationMessageId.of(existing.getMessageId()),
                        new IdempotencyKey(existing.getIdempotencyKey()),
                        true,
                        existing.getReceivedAt()))
                .orElseGet(() -> persistNewMessage(message, payload));
    }

    private StageErpShipmentResult persistNewMessage(ErpShipmentMessage message, String payload) {
        Instant stagedAt = Instant.now();
        IntegrationOutboxMessage outboxMessage = IntegrationOutboxMessage.pending(message, payload, stagedAt);

        receiptRepository.save(ErpShipmentReceiptJpaEntity.from(message, stagedAt));
        outboxRepository.save(IntegrationOutboxJpaEntity.from(outboxMessage));

        return new StageErpShipmentResult(
                message.id(),
                message.idempotencyKey(),
                false,
                stagedAt);
    }
}
