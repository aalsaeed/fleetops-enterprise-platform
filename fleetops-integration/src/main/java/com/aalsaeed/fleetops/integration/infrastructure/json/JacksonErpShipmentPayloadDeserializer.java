package com.aalsaeed.fleetops.integration.infrastructure.json;

import com.aalsaeed.fleetops.integration.application.port.out.ErpShipmentPayloadDeserializer;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentOperation;
import com.aalsaeed.fleetops.integration.domain.IntegrationMessageId;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
public class JacksonErpShipmentPayloadDeserializer implements ErpShipmentPayloadDeserializer {

    private final JsonMapper jsonMapper;

    public JacksonErpShipmentPayloadDeserializer(JsonMapper jsonMapper) {
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "JsonMapper cannot be null");
    }

    @Override
    public ErpShipmentMessage deserialize(String payload) {
        String normalizedPayload = requireText(payload, "Payload");

        try {
            JsonNode root = jsonMapper.readTree(normalizedPayload);
            ErpShipmentMessage message = ErpShipmentMessage.restore(
                    IntegrationMessageId.of(UUID.fromString(requiredText(root, "messageId"))),
                    requiredInt(root, "schemaVersion"),
                    requiredText(root, "sourceSystem"),
                    requiredText(root, "sourceMessageId"),
                    requiredText(root, "shipmentReference"),
                    ErpShipmentOperation.valueOf(requiredText(root, "operation")),
                    Instant.parse(requiredText(root, "occurredAt")),
                    optionalText(root, "correlationId"));

            String serializedIdempotencyKey = requiredText(root, "idempotencyKey");
            if (!message.idempotencyKey().value().equals(serializedIdempotencyKey)) {
                throw new IllegalArgumentException("ERP shipment idempotency key does not match its source identity");
            }
            return message;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid ERP shipment payload", exception);
        }
    }

    private static String requiredText(JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            throw new IllegalArgumentException("ERP shipment payload field " + fieldName + " is required");
        }
        return node.asText().trim();
    }

    private static int requiredInt(JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        if (!node.isInt()) {
            throw new IllegalArgumentException("ERP shipment payload field " + fieldName + " must be an integer");
        }
        return node.asInt();
    }

    private static String optionalText(JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        return node.asText().trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }
}
