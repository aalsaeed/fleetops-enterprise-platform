package com.aalsaeed.fleetops.integration.infrastructure.messaging.rabbit;

import com.aalsaeed.fleetops.integration.application.port.in.IntegrationOperationsSnapshot;
import com.aalsaeed.fleetops.integration.application.port.out.DeadLetterQueueMetricsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class RabbitMqDeadLetterQueueMetricsAdapter implements DeadLetterQueueMetricsPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqDeadLetterQueueMetricsAdapter.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqDeadLetterQueueMetricsAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "RabbitTemplate cannot be null");
    }

    @Override
    public IntegrationOperationsSnapshot.DeadLetterQueueSnapshot getSnapshot() {
        try {
            Integer messageCount = rabbitTemplate.execute(channel ->
                    channel.queueDeclarePassive(RabbitMqIntegrationTopology.ERP_SHIPMENT_DLQ)
                            .getMessageCount());
            return new IntegrationOperationsSnapshot.DeadLetterQueueSnapshot(
                    true,
                    messageCount == null ? 0 : messageCount.longValue());
        }
        catch (RuntimeException exception) {
            log.warn("Could not read ERP shipment dead-letter queue depth", exception);
            return new IntegrationOperationsSnapshot.DeadLetterQueueSnapshot(false, 0);
        }
    }
}
