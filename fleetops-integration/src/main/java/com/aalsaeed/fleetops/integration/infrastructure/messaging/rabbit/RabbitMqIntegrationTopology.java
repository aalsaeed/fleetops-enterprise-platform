package com.aalsaeed.fleetops.integration.infrastructure.messaging.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqIntegrationTopology {

    public static final String INTEGRATION_EXCHANGE = "fleetops.integration.events";
    public static final String ERP_SHIPMENT_QUEUE = "fleetops.erp.shipment.v1";
    public static final String ERP_SHIPMENT_ROUTING_KEY = "erp.shipment.v1";

    public static final String INTEGRATION_DEAD_LETTER_EXCHANGE = "fleetops.integration.dlx";
    public static final String ERP_SHIPMENT_DLQ = "fleetops.erp.shipment.v1.dlq";
    public static final String ERP_SHIPMENT_DLQ_ROUTING_KEY = "erp.shipment.v1.dlq";

    @Bean
    TopicExchange integrationEventsExchange() {
        return new TopicExchange(INTEGRATION_EXCHANGE, true, false);
    }

    @Bean
    Queue erpShipmentQueue() {
        return QueueBuilder.durable(ERP_SHIPMENT_QUEUE).build();
    }

    @Bean
    Binding erpShipmentBinding(Queue erpShipmentQueue, TopicExchange integrationEventsExchange) {
        return BindingBuilder.bind(erpShipmentQueue)
                .to(integrationEventsExchange)
                .with(ERP_SHIPMENT_ROUTING_KEY);
    }

    @Bean
    DirectExchange integrationDeadLetterExchange() {
        return new DirectExchange(INTEGRATION_DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue erpShipmentDeadLetterQueue() {
        return QueueBuilder.durable(ERP_SHIPMENT_DLQ).build();
    }

    @Bean
    Binding erpShipmentDeadLetterBinding(
            Queue erpShipmentDeadLetterQueue,
            DirectExchange integrationDeadLetterExchange) {
        return BindingBuilder.bind(erpShipmentDeadLetterQueue)
                .to(integrationDeadLetterExchange)
                .with(ERP_SHIPMENT_DLQ_ROUTING_KEY);
    }
}
