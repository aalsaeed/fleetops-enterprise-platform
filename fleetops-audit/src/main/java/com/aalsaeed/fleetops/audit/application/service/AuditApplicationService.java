package com.aalsaeed.fleetops.audit.application.service;

import com.aalsaeed.fleetops.audit.application.port.in.RecordAuditEventCommand;
import com.aalsaeed.fleetops.audit.application.port.in.RecordAuditEventUseCase;
import com.aalsaeed.fleetops.audit.application.port.out.AuditEventStore;
import com.aalsaeed.fleetops.audit.domain.AuditEvent;
import com.aalsaeed.fleetops.audit.domain.AuditEventId;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class AuditApplicationService implements RecordAuditEventUseCase {

    private final AuditEventStore store;
    private final Clock clock;

    public AuditApplicationService(AuditEventStore store, Clock clock) {
        this.store = Objects.requireNonNull(store, "Audit store cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    @Override
    public AuditEvent record(RecordAuditEventCommand command) {
        Objects.requireNonNull(command, "Audit command cannot be null");

        AuditEvent event = new AuditEvent(
                AuditEventId.newId(),
                Instant.now(clock),
                command.actorSubject(),
                command.actorDisplayName(),
                command.actorAuthorities(),
                command.action(),
                command.resourceType(),
                command.resourceId(),
                command.outcome(),
                command.correlationId(),
                command.metadata());

        store.append(event);
        return event;
    }
}
