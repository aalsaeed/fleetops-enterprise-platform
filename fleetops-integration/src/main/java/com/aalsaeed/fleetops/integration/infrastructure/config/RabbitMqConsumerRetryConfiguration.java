package com.aalsaeed.fleetops.integration.infrastructure.config;

import com.aalsaeed.fleetops.integration.infrastructure.messaging.rabbit.RabbitMqIntegrationTopology;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.StatelessRetryOperationsInterceptor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConsumerRetryConfiguration {

    public static final String LISTENER_CONTAINER_FACTORY = "integrationRabbitListenerContainerFactory";

    @Bean
    StatelessRetryOperationsInterceptor integrationConsumerRetryInterceptor(
            RabbitTemplate rabbitTemplate,
            @Value("${fleetops.integration.inbox.consumer.retry.max-attempts:3}") int maxAttempts,
            @Value("${fleetops.integration.inbox.consumer.retry.initial-interval-ms:250}") long initialIntervalMs,
            @Value("${fleetops.integration.inbox.consumer.retry.multiplier:2.0}") double multiplier,
            @Value("${fleetops.integration.inbox.consumer.retry.max-interval-ms:2000}") long maxIntervalMs) {
        validateRetrySettings(maxAttempts, initialIntervalMs, multiplier, maxIntervalMs);

        RepublishMessageRecoverer recoverer = new RepublishMessageRecoverer(
                rabbitTemplate,
                RabbitMqIntegrationTopology.INTEGRATION_DEAD_LETTER_EXCHANGE,
                RabbitMqIntegrationTopology.ERP_SHIPMENT_DLQ_ROUTING_KEY);

        int maxRetries = maxAttempts - 1;
        return RetryInterceptorBuilder.stateless()
                .maxRetries(maxRetries)
                .backOffOptions(initialIntervalMs, multiplier, maxIntervalMs)
                .recoverer(recoverer)
                .build();
    }

    @Bean(name = LISTENER_CONTAINER_FACTORY)
    SimpleRabbitListenerContainerFactory integrationRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            StatelessRetryOperationsInterceptor integrationConsumerRetryInterceptor) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(integrationConsumerRetryInterceptor);
        return factory;
    }

    private static void validateRetrySettings(
            int maxAttempts,
            long initialIntervalMs,
            double multiplier,
            long maxIntervalMs) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Inbox retry max attempts must be at least 1");
        }
        if (initialIntervalMs < 1) {
            throw new IllegalArgumentException("Inbox retry initial interval must be at least 1 ms");
        }
        if (multiplier < 1.0) {
            throw new IllegalArgumentException("Inbox retry multiplier must be at least 1.0");
        }
        if (maxIntervalMs < initialIntervalMs) {
            throw new IllegalArgumentException("Inbox retry maximum interval cannot be less than initial interval");
        }
    }
}
