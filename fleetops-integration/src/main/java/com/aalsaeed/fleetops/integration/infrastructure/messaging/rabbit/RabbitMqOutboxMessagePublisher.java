package com.aalsaeed.fleetops.integration.infrastructure.messaging.rabbit;

import com.aalsaeed.fleetops.integration.application.port.out.OutboxMessagePublisher;
import com.aalsaeed.fleetops.integration.domain.outbox.IntegrationOutboxMessage;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class RabbitMqOutboxMessagePublisher implements OutboxMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final long confirmTimeoutMillis;

    public RabbitMqOutboxMessagePublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${fleetops.integration.outbox.publisher.confirm-timeout-ms:5000}") long confirmTimeoutMillis) {
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "RabbitTemplate cannot be null");
        if (confirmTimeoutMillis < 1) {
            throw new IllegalArgumentException("Confirm timeout must be at least 1 ms");
        }
        this.confirmTimeoutMillis = confirmTimeoutMillis;
    }

    @Override
    public void publish(IntegrationOutboxMessage outboxMessage) {
        Objects.requireNonNull(outboxMessage, "Outbox message cannot be null");

        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setMessageId(outboxMessage.id().value().toString());
        properties.setHeader("x-idempotency-key", outboxMessage.idempotencyKey().value());
        properties.setHeader("x-event-type", outboxMessage.eventType());
        properties.setHeader("x-aggregate-type", outboxMessage.aggregateType());
        properties.setHeader("x-aggregate-id", outboxMessage.aggregateId());

        Message message = new Message(outboxMessage.payload().getBytes(StandardCharsets.UTF_8), properties);
        CorrelationData correlationData = new CorrelationData(outboxMessage.id().value().toString());

        try {
            rabbitTemplate.send(
                    RabbitMqIntegrationTopology.INTEGRATION_EXCHANGE,
                    RabbitMqIntegrationTopology.ERP_SHIPMENT_ROUTING_KEY,
                    message,
                    correlationData);

            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(confirmTimeoutMillis, TimeUnit.MILLISECONDS);

            ReturnedMessage returned = correlationData.getReturned();
            if (returned != null) {
                throw new IllegalStateException(
                        "RabbitMQ returned message as unroutable: " + returned.getReplyText());
            }
            if (!confirm.ack()) {
                throw new IllegalStateException(
                        "RabbitMQ negatively acknowledged publication: " + confirm.reason());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for RabbitMQ publisher confirm", ex);
        } catch (TimeoutException ex) {
            throw new IllegalStateException("Timed out waiting for RabbitMQ publisher confirm", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("RabbitMQ publisher confirm failed", ex.getCause());
        } catch (AmqpException ex) {
            throw new IllegalStateException("RabbitMQ publication failed", ex);
        }
    }
}
