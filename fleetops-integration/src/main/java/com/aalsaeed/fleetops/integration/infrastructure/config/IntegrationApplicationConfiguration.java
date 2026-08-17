package com.aalsaeed.fleetops.integration.infrastructure.config;

import com.aalsaeed.fleetops.integration.application.port.out.ErpShipmentIngestionStore;
import com.aalsaeed.fleetops.integration.application.port.out.ErpShipmentPayloadSerializer;
import com.aalsaeed.fleetops.integration.application.service.ErpShipmentIngestionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IntegrationApplicationConfiguration {

    @Bean
    ErpShipmentIngestionService erpShipmentIngestionService(
            ErpShipmentPayloadSerializer payloadSerializer,
            ErpShipmentIngestionStore ingestionStore) {
        return new ErpShipmentIngestionService(payloadSerializer, ingestionStore);
    }
}
