package com.aalsaeed.fleetops.integration.infrastructure.json;

import com.aalsaeed.fleetops.integration.application.port.out.ErpShipmentPayloadSerializer;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class JacksonErpShipmentPayloadSerializer implements ErpShipmentPayloadSerializer {

    private final JsonMapper jsonMapper;

    public JacksonErpShipmentPayloadSerializer(JsonMapper jsonMapper) {
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "JsonMapper cannot be null");
    }

    @Override
    public String serialize(ErpShipmentMessage message) {
        Objects.requireNonNull(message, "ERP shipment message cannot be null");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageId", message.id().value().toString());
        payload.put("schemaVersion", message.schemaVersion());
        payload.put("sourceSystem", message.sourceSystem());
        payload.put("sourceMessageId", message.sourceMessageId());
        payload.put("shipmentReference", message.shipmentReference());
        payload.put("operation", message.operation().name());
        payload.put("occurredAt", message.occurredAt().toString());
        payload.put("correlationId", message.correlationId());
        payload.put("idempotencyKey", message.idempotencyKey().value());

        try {
            return jsonMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize ERP shipment message", exception);
        }
    }
}
