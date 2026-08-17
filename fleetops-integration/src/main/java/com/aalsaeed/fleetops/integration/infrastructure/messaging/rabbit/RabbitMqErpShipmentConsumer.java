package com.aalsaeed.fleetops.integration.infrastructure.messaging.rabbit;

import com.aalsaeed.fleetops.integration.application.port.in.AcceptErpShipmentDeliveryCommand;
import com.aalsaeed.fleetops.integration.application.port.in.AcceptErpShipmentDeliveryResult;
import com.aalsaeed.fleetops.integration.application.port.in.AcceptErpShipmentDeliveryUseCase;
import com.aalsaeed.fleetops.integration.application.port.out.ErpShipmentPayloadDeserializer;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import com.aalsaeed.fleetops.integration.infrastructure.config.RabbitMqConsumerRetryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "fleetops.integration.inbox.consumer",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public final class RabbitMqErpShipmentConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqErpShipmentConsumer.class);
    private static final String IDEMPOTENCY_HEADER = "x-idempotency-key";
    private static final String EVENT_TYPE_HEADER = "x-event-type";
    private static final String AGGREGATE_TYPE_HEADER = "x-aggregate-type";
    private static final String AGGREGATE_ID_HEADER = "x-aggregate-id";
    private static final String ERP_SHIPMENT_EVENT_TYPE = "ERP_SHIPMENT";
    private static final String SHIPMENT_AGGREGATE_TYPE = "SHIPMENT";

    private final ErpShipmentPayloadDeserializer payloadDeserializer;
    private final AcceptErpShipmentDeliveryUseCase acceptDeliveryUseCase;

    public RabbitMqErpShipmentConsumer(
            ErpShipmentPayloadDeserializer payloadDeserializer,
            AcceptErpShipmentDeliveryUseCase acceptDeliveryUseCase) {
        this.payloadDeserializer = Objects.requireNonNull(payloadDeserializer, "Payload deserializer cannot be null");
        this.acceptDeliveryUseCase = Objects.requireNonNull(acceptDeliveryUseCase, "Inbox use case cannot be null");
    }

    @RabbitListener(
            queues = RabbitMqIntegrationTopology.ERP_SHIPMENT_QUEUE,
            containerFactory = RabbitMqConsumerRetryConfiguration.LISTENER_CONTAINER_FACTORY)
    public void consume(Message amqpMessage) {
        Objects.requireNonNull(amqpMessage, "RabbitMQ message cannot be null");

        String payload = new String(amqpMessage.getBody(), StandardCharsets.UTF_8);
        ErpShipmentMessage message = payloadDeserializer.deserialize(payload);
        MessageProperties properties = amqpMessage.getMessageProperties();

        verifyMessageId(properties, message);
        String idempotencyKey = requiredHeader(properties, IDEMPOTENCY_HEADER);
        String eventType = requiredHeader(properties, EVENT_TYPE_HEADER);
        String aggregateType = requiredHeader(properties, AGGREGATE_TYPE_HEADER);
        String aggregateId = requiredHeader(properties, AGGREGATE_ID_HEADER);

        if (!message.idempotencyKey().value().equals(idempotencyKey)) {
            throw new IllegalArgumentException("RabbitMQ idempotency header does not match ERP shipment payload");
        }
        if (!ERP_SHIPMENT_EVENT_TYPE.equals(eventType)) {
            throw new IllegalArgumentException("Unsupported integration event type: " + eventType);
        }
        if (!SHIPMENT_AGGREGATE_TYPE.equals(aggregateType)) {
            throw new IllegalArgumentException("Unsupported integration aggregate type: " + aggregateType);
        }
        if (!message.shipmentReference().equals(aggregateId)) {
            throw new IllegalArgumentException("RabbitMQ aggregate ID does not match shipment reference");
        }

        AcceptErpShipmentDeliveryResult result = acceptDeliveryUseCase.accept(
                new AcceptErpShipmentDeliveryCommand(
                        message,
                        payload,
                        eventType,
                        aggregateType,
                        aggregateId,
                        Instant.now()));

        if (result.duplicate()) {
            log.debug("Ignored duplicate ERP shipment delivery for {}", result.idempotencyKey().value());
        }
    }

    private static void verifyMessageId(MessageProperties properties, ErpShipmentMessage message) {
        String messageId = properties.getMessageId();
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("RabbitMQ message ID is required");
        }
        if (!message.id().value().toString().equals(messageId)) {
            throw new IllegalArgumentException("RabbitMQ message ID does not match ERP shipment payload");
        }
    }

    private static String requiredHeader(MessageProperties properties, String headerName) {
        Object value = properties.getHeaders().get(headerName);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("RabbitMQ header " + headerName + " is required");
        }
        return value.toString().trim();
    }
}
