package com.aalsaeed.fleetops.integration.application.service;

import com.aalsaeed.fleetops.integration.application.port.in.StageErpShipmentResult;
import com.aalsaeed.fleetops.integration.application.port.in.StageErpShipmentUseCase;
import com.aalsaeed.fleetops.integration.application.port.out.ErpShipmentIngestionStore;
import com.aalsaeed.fleetops.integration.application.port.out.ErpShipmentPayloadSerializer;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;

import java.util.Objects;

public final class ErpShipmentIngestionService implements StageErpShipmentUseCase {

    private final ErpShipmentPayloadSerializer payloadSerializer;
    private final ErpShipmentIngestionStore ingestionStore;

    public ErpShipmentIngestionService(
            ErpShipmentPayloadSerializer payloadSerializer,
            ErpShipmentIngestionStore ingestionStore) {
        this.payloadSerializer = Objects.requireNonNull(payloadSerializer, "Payload serializer cannot be null");
        this.ingestionStore = Objects.requireNonNull(ingestionStore, "Ingestion store cannot be null");
    }

    @Override
    public StageErpShipmentResult stage(ErpShipmentMessage message) {
        Objects.requireNonNull(message, "ERP shipment message cannot be null");
        String payload = payloadSerializer.serialize(message);
        if (payload == null || payload.isBlank()) {
            throw new IllegalStateException("Serialized ERP shipment payload cannot be blank");
        }
        return ingestionStore.stage(message, payload);
    }
}
