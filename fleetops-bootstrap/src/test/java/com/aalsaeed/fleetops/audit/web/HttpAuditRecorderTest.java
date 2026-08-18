package com.aalsaeed.fleetops.audit.web;

import com.aalsaeed.fleetops.audit.domain.AuditOutcome;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class HttpAuditRecorderTest {

    @Test
    void auditPersistenceFailureDoesNotReplaceBusinessOutcome() {
        HttpAuditRecorder recorder = new HttpAuditRecorder(command -> {
            throw new IllegalStateException("audit database unavailable");
        });

        AuditRouteRegistry.AuditRoute route = AuditRouteRegistry.AuditRoute.created(
                "DRIVER_CREATE",
                "DRIVER");

        assertThatCode(() -> recorder.record(
                route,
                "driver-1",
                AuditOutcome.SUCCESS,
                "corr-1",
                Map.of("status", "201")))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsSafeCorrelationIdAndReplacesUnsafeInput() {
        assertThat(AuditCorrelationFilter.resolveCorrelationId("request-123:abc"))
                .isEqualTo("request-123:abc");

        assertThat(AuditCorrelationFilter.resolveCorrelationId("bad value\r\nheader"))
                .matches("[0-9a-f-]{36}");
    }
}
