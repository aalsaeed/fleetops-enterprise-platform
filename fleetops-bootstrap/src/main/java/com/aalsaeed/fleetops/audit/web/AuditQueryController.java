package com.aalsaeed.fleetops.audit.web;

import com.aalsaeed.fleetops.audit.application.port.in.AuditSearchQuery;
import com.aalsaeed.fleetops.audit.application.port.in.SearchAuditEventsUseCase;
import com.aalsaeed.fleetops.audit.domain.AuditOutcome;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit/events")
public class AuditQueryController {

    private final SearchAuditEventsUseCase searchAuditEventsUseCase;

    public AuditQueryController(SearchAuditEventsUseCase searchAuditEventsUseCase) {
        this.searchAuditEventsUseCase = searchAuditEventsUseCase;
    }

    @GetMapping
    public AuditSearchResponse search(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(name = "actor", required = false) String actorSubject,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) AuditOutcome outcome,
            @RequestParam(required = false) String correlationId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {

        validate(from, to, actorSubject, action, resourceType, resourceId, correlationId, offset, limit);

        return AuditSearchResponse.from(searchAuditEventsUseCase.search(new AuditSearchQuery(
                from,
                to,
                actorSubject,
                action,
                resourceType,
                resourceId,
                outcome,
                correlationId,
                offset,
                limit)));
    }

    private static void validate(
            Instant from,
            Instant to,
            String actorSubject,
            String action,
            String resourceType,
            String resourceId,
            String correlationId,
            int offset,
            int limit) {

        if (from != null && to != null && from.isAfter(to)) {
            badRequest("Query parameter 'from' cannot be after 'to'");
        }
        if (offset < 0) {
            badRequest("Query parameter 'offset' cannot be negative");
        }
        if (limit < 1 || limit > AuditSearchQuery.MAX_LIMIT) {
            badRequest("Query parameter 'limit' must be between 1 and " + AuditSearchQuery.MAX_LIMIT);
        }

        maxLength("actor", actorSubject, 255);
        maxLength("action", action, 120);
        maxLength("resourceType", resourceType, 100);
        maxLength("resourceId", resourceId, 255);
        maxLength("correlationId", correlationId, 128);
    }

    private static void maxLength(String name, String value, int maximum) {
        if (value != null && value.length() > maximum) {
            badRequest("Query parameter '" + name + "' exceeds maximum length " + maximum);
        }
    }

    private static void badRequest(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
