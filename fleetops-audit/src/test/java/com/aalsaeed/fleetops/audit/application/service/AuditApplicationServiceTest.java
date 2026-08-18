package com.aalsaeed.fleetops.audit.application.service;

import com.aalsaeed.fleetops.audit.application.port.in.RecordAuditEventCommand;
import com.aalsaeed.fleetops.audit.application.port.out.AuditEventStore;
import com.aalsaeed.fleetops.audit.domain.AuditEvent;
import com.aalsaeed.fleetops.audit.domain.AuditOutcome;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuditApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-19T00:00:00Z");

    @Test
    void createsCanonicalAuditEventAndAppendsIt() {
        CapturingStore store = new CapturingStore();
        AuditApplicationService service = new AuditApplicationService(
                store,
                Clock.fixed(NOW, ZoneOffset.UTC));

        AuditEvent event = service.record(new RecordAuditEventCommand(
                "subject-123",
                "Fleet Operator",
                Set.of("FLEETOPS_OPERATOR"),
                "DRIVER_CREATE",
                "DRIVER",
                "driver-123",
                AuditOutcome.SUCCESS,
                "corr-123",
                Map.of("externalReference", "DRV-1001")));

        assertNotNull(event.id());
        assertEquals(NOW, event.occurredAt());
        assertEquals("subject-123", event.actorSubject());
        assertEquals(Set.of("FLEETOPS_OPERATOR"), event.actorAuthorities());
        assertEquals("DRIVER_CREATE", event.action());
        assertEquals("DRIVER", event.resourceType());
        assertEquals("driver-123", event.resourceId());
        assertEquals(AuditOutcome.SUCCESS, event.outcome());
        assertEquals("corr-123", event.correlationId());
        assertEquals("DRV-1001", event.metadata().get("externalReference"));
        assertEquals(event, store.appended);
    }

    private static final class CapturingStore implements AuditEventStore {
        private AuditEvent appended;

        @Override
        public void append(AuditEvent event) {
            this.appended = event;
        }
    }
}
