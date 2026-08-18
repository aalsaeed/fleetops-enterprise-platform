package com.aalsaeed.fleetops.audit.application.port.out;

import com.aalsaeed.fleetops.audit.domain.AuditEvent;

public interface AuditEventStore {

    void append(AuditEvent event);
}
