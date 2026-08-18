package com.aalsaeed.fleetops.audit.application.service;

import com.aalsaeed.fleetops.audit.application.port.in.AuditSearchPage;
import com.aalsaeed.fleetops.audit.application.port.in.AuditSearchQuery;
import com.aalsaeed.fleetops.audit.application.port.in.SearchAuditEventsUseCase;
import com.aalsaeed.fleetops.audit.application.port.out.AuditEventQueryStore;

import java.util.Objects;

public final class AuditQueryService implements SearchAuditEventsUseCase {

    private final AuditEventQueryStore queryStore;

    public AuditQueryService(AuditEventQueryStore queryStore) {
        this.queryStore = Objects.requireNonNull(queryStore, "Audit query store cannot be null");
    }

    @Override
    public AuditSearchPage search(AuditSearchQuery query) {
        return queryStore.search(Objects.requireNonNull(query, "Audit search query cannot be null"));
    }
}
