package com.aalsaeed.fleetops.audit.application.port.out;

import com.aalsaeed.fleetops.audit.application.port.in.AuditSearchPage;
import com.aalsaeed.fleetops.audit.application.port.in.AuditSearchQuery;

public interface AuditEventQueryStore {

    AuditSearchPage search(AuditSearchQuery query);
}
