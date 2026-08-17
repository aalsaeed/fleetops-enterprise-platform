package com.aalsaeed.fleetops.integration.infrastructure.messaging.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqIntegrationTopology {

    public static final String INTEGRATION_EXCHANGE = "fleetops.integration.events";
    public static final String ERP_SHIPMENT_QUEUE = "fleetops.erp.shipment.v1";
    public static final String ERP_SHIPMENT_ROUTING_KEY = "erp.shipment.v1";

    @Bean
    TopicExchange integrationEventsExchange() {
        return ExchangeBuilder.topicExchange(INTEGRATION_EXCHANGE)
                .durable(true)
                .build();
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
}
