package com.aalsaeed.fleetops.audit.web;

import com.aalsaeed.fleetops.audit.application.port.in.RecordAuditEventCommand;
import com.aalsaeed.fleetops.audit.application.port.in.RecordAuditEventUseCase;
import com.aalsaeed.fleetops.audit.domain.AuditOutcome;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
final class HttpAuditRecorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpAuditRecorder.class);
    private static final String ANONYMOUS_SUBJECT = "anonymous";

    private final RecordAuditEventUseCase recordAuditEventUseCase;
    private final Tracer tracer;

    HttpAuditRecorder(RecordAuditEventUseCase recordAuditEventUseCase, Tracer tracer) {
        this.recordAuditEventUseCase = Objects.requireNonNull(recordAuditEventUseCase);
        this.tracer = Objects.requireNonNull(tracer);
    }

    void record(
            AuditRouteRegistry.AuditRoute route,
            String resourceId,
            AuditOutcome outcome,
            String correlationId,
            Map<String, String> metadata) {

        AuditActor actor = currentActor();
        Map<String, String> enrichedMetadata = withTraceContext(metadata);
        try {
            recordAuditEventUseCase.record(new RecordAuditEventCommand(
                    actor.subject(),
                    actor.displayName(),
                    actor.authorities(),
                    route.action(),
                    route.resourceType(),
                    resourceId,
                    outcome,
                    correlationId,
                    enrichedMetadata));
        } catch (RuntimeException exception) {
            // Audit persistence is deliberately isolated from the already-completed business operation.
            // The failure is surfaced to operations through logs instead of replacing the HTTP result.
            LOGGER.error(
                    "Failed to persist audit event action={} correlationId={}",
                    route.action(),
                    correlationId,
                    exception);
        }
    }

    private Map<String, String> withTraceContext(Map<String, String> metadata) {
        LinkedHashMap<String, String> enriched = new LinkedHashMap<>(Objects.requireNonNull(metadata));
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return Map.copyOf(enriched);
        }

        String traceId = currentSpan.context().traceId();
        String spanId = currentSpan.context().spanId();
        if (traceId != null && !traceId.isBlank()) {
            enriched.putIfAbsent("traceId", traceId);
        }
        if (spanId != null && !spanId.isBlank()) {
            enriched.putIfAbsent("spanId", spanId);
        }
        return Map.copyOf(enriched);
    }

    private static AuditActor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new AuditActor(ANONYMOUS_SUBJECT, null, Set.of());
        }

        String subject = authentication.getName();
        String displayName = null;
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            if (jwtAuthentication.getToken().getSubject() != null
                    && !jwtAuthentication.getToken().getSubject().isBlank()) {
                subject = jwtAuthentication.getToken().getSubject();
            }
            displayName = firstNonBlank(
                    jwtAuthentication.getToken().getClaimAsString("preferred_username"),
                    jwtAuthentication.getToken().getClaimAsString("email"),
                    jwtAuthentication.getToken().getClaimAsString("client_id"),
                    jwtAuthentication.getToken().getClaimAsString("azp"));
        }

        if (subject == null || subject.isBlank()) {
            subject = ANONYMOUS_SUBJECT;
        }

        LinkedHashSet<String> authorities = new LinkedHashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority != null && authority.getAuthority() != null && !authority.getAuthority().isBlank()) {
                authorities.add(authority.getAuthority());
            }
        }
        return new AuditActor(subject, displayName, Set.copyOf(authorities));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private record AuditActor(String subject, String displayName, Set<String> authorities) {
    }
}
