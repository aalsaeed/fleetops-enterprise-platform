package com.aalsaeed.fleetops.audit.application.port.in;

public interface SearchAuditEventsUseCase {

    AuditSearchPage search(AuditSearchQuery query);
}
