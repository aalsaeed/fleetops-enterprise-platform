package com.aalsaeed.fleetops.audit.application.port.in;

import com.aalsaeed.fleetops.audit.domain.AuditEvent;

public interface RecordAuditEventUseCase {

    AuditEvent record(RecordAuditEventCommand command);
}
