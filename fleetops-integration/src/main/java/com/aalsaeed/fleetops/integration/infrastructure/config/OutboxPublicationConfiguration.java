package com.aalsaeed.fleetops.integration.infrastructure.config;

import com.aalsaeed.fleetops.integration.application.port.in.PublishPendingOutboxUseCase;
import com.aalsaeed.fleetops.integration.application.port.out.OutboxMessagePublisher;
import com.aalsaeed.fleetops.integration.application.port.out.OutboxPublicationStore;
import com.aalsaeed.fleetops.integration.application.service.OutboxPublicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class OutboxPublicationConfiguration {

    @Bean
    PublishPendingOutboxUseCase publishPendingOutboxUseCase(
            OutboxPublicationStore publicationStore,
            OutboxMessagePublisher messagePublisher) {
        return new OutboxPublicationService(publicationStore, messagePublisher, Clock.systemUTC());
    }
}
