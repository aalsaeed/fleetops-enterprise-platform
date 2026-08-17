package com.aalsaeed.fleetops.integration.infrastructure.config;

import com.aalsaeed.fleetops.integration.application.port.in.AcceptErpShipmentDeliveryUseCase;
import com.aalsaeed.fleetops.integration.application.port.out.IntegrationInboxStore;
import com.aalsaeed.fleetops.integration.application.service.ErpShipmentInboxService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InboxConsumerConfiguration {

    @Bean
    AcceptErpShipmentDeliveryUseCase acceptErpShipmentDeliveryUseCase(IntegrationInboxStore inboxStore) {
        return new ErpShipmentInboxService(inboxStore);
    }
}
