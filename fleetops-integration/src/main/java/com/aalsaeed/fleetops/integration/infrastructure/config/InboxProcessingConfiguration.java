package com.aalsaeed.fleetops.integration.infrastructure.config;

import com.aalsaeed.fleetops.integration.application.port.in.ProcessPendingInboxUseCase;
import com.aalsaeed.fleetops.integration.application.port.in.RecoverStaleInboxUseCase;
import com.aalsaeed.fleetops.integration.application.port.in.RequeueFailedInboxUseCase;
import com.aalsaeed.fleetops.integration.application.port.out.ErpShipmentPayloadDeserializer;
import com.aalsaeed.fleetops.integration.application.port.out.ErpShipmentTripHandoffPort;
import com.aalsaeed.fleetops.integration.application.port.out.InboxProcessingStore;
import com.aalsaeed.fleetops.integration.application.service.InboxProcessingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class InboxProcessingConfiguration {

    @Bean
    InboxProcessingService inboxProcessingService(
            InboxProcessingStore processingStore,
            ErpShipmentPayloadDeserializer payloadDeserializer,
            ErpShipmentTripHandoffPort tripHandoffPort) {
        return new InboxProcessingService(
                processingStore,
                payloadDeserializer,
                tripHandoffPort,
                Clock.systemUTC());
    }

    @Bean
    ProcessPendingInboxUseCase processPendingInboxUseCase(InboxProcessingService service) {
        return service;
    }

    @Bean
    RecoverStaleInboxUseCase recoverStaleInboxUseCase(InboxProcessingService service) {
        return service;
    }

    @Bean
    RequeueFailedInboxUseCase requeueFailedInboxUseCase(InboxProcessingService service) {
        return service;
    }
}
