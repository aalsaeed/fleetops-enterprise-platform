package com.aalsaeed.fleetops.integration.infrastructure.config;

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
}
