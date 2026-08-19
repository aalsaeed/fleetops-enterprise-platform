package com.aalsaeed.fleetops.audit.web;

import com.aalsaeed.fleetops.audit.application.port.in.RecordAuditEventCommand;
import com.aalsaeed.fleetops.audit.domain.AuditOutcome;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpAuditRecorderTest {

    @Test
    void auditPersistenceFailureDoesNotReplaceBusinessOutcome() {
        Tracer tracer = mock(Tracer.class);
        HttpAuditRecorder recorder = new HttpAuditRecorder(command -> {
            throw new IllegalStateException("audit database unavailable");
        }, tracer);

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
    void enrichesAuditMetadataWithCurrentTraceIdentifiers() {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("4bf92f3577b34da6a3ce929d0e0e4736");
        when(traceContext.spanId()).thenReturn("00f067aa0ba902b7");

        AtomicReference<RecordAuditEventCommand> captured = new AtomicReference<>();
        HttpAuditRecorder recorder = new HttpAuditRecorder(command -> {
            captured.set(command);
            return null;
        }, tracer);

        recorder.record(
                AuditRouteRegistry.AuditRoute.created("TRIP_CREATE", "TRIP"),
                "trip-1",
                AuditOutcome.SUCCESS,
                "corr-2",
                Map.of("status", "201"));

        assertThat(captured.get().metadata())
                .containsEntry("status", "201")
                .containsEntry("traceId", "4bf92f3577b34da6a3ce929d0e0e4736")
                .containsEntry("spanId", "00f067aa0ba902b7");
    }

    @Test
    void acceptsSafeCorrelationIdAndReplacesUnsafeInput() {
        assertThat(AuditCorrelationFilter.resolveCorrelationId("request-123:abc"))
                .isEqualTo("request-123:abc");

        assertThat(AuditCorrelationFilter.resolveCorrelationId("bad value\r\nheader"))
                .matches("[0-9a-f-]{36}");
    }
}
