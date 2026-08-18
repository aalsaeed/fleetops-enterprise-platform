package com.aalsaeed.fleetops.audit.infrastructure.config;

import com.aalsaeed.fleetops.audit.application.port.in.RecordAuditEventUseCase;
import com.aalsaeed.fleetops.audit.application.port.in.SearchAuditEventsUseCase;
import com.aalsaeed.fleetops.audit.application.port.out.AuditEventQueryStore;
import com.aalsaeed.fleetops.audit.application.port.out.AuditEventStore;
import com.aalsaeed.fleetops.audit.application.service.AuditApplicationService;
import com.aalsaeed.fleetops.audit.application.service.AuditQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class AuditConfiguration {

    @Bean
    RecordAuditEventUseCase recordAuditEventUseCase(AuditEventStore store) {
        return new AuditApplicationService(store, Clock.systemUTC());
    }

    @Bean
    SearchAuditEventsUseCase searchAuditEventsUseCase(AuditEventQueryStore queryStore) {
        return new AuditQueryService(queryStore);
    }
}
