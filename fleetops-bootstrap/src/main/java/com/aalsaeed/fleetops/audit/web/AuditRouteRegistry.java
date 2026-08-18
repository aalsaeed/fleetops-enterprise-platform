package com.aalsaeed.fleetops.audit.web;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
final class AuditRouteRegistry {

    private final Map<RouteKey, AuditRoute> routes = Map.ofEntries(
            Map.entry(key("POST", "/api/v1/drivers"), AuditRoute.created("DRIVER_CREATE", "DRIVER")),
            Map.entry(key("PATCH", "/api/v1/drivers/{id}/status"), AuditRoute.path("DRIVER_STATUS_CHANGE", "DRIVER", "id")),
            Map.entry(key("POST", "/api/v1/vehicles"), AuditRoute.created("VEHICLE_CREATE", "VEHICLE")),
            Map.entry(key("PATCH", "/api/v1/vehicles/{id}/status"), AuditRoute.path("VEHICLE_STATUS_CHANGE", "VEHICLE", "id")),
            Map.entry(key("POST", "/api/v1/trips"), AuditRoute.created("TRIP_CREATE", "TRIP")),
            Map.entry(key("PUT", "/api/v1/trips/{id}/assignment"), AuditRoute.path("TRIP_ASSIGN_RESOURCES", "TRIP", "id")),
            Map.entry(key("POST", "/api/v1/trips/{id}/start"), AuditRoute.path("TRIP_START", "TRIP", "id")),
            Map.entry(key("POST", "/api/v1/trips/{id}/complete"), AuditRoute.path("TRIP_COMPLETE", "TRIP", "id")),
            Map.entry(key("POST", "/api/v1/trips/{id}/cancel"), AuditRoute.path("TRIP_CANCEL", "TRIP", "id")),
            Map.entry(
                    key("POST", "/api/v1/integration/operations/outbox/{messageId}/requeue"),
                    AuditRoute.path("INTEGRATION_OUTBOX_REQUEUE", "INTEGRATION_OUTBOX", "messageId")),
            Map.entry(
                    key("POST", "/api/v1/integration/operations/inbox/{messageId}/requeue"),
                    AuditRoute.path("INTEGRATION_INBOX_REQUEUE", "INTEGRATION_INBOX", "messageId")));

    Optional<AuditRoute> resolve(String method, String routePattern) {
        if (method == null || routePattern == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(routes.get(key(method, routePattern)));
    }

    private static RouteKey key(String method, String routePattern) {
        return new RouteKey(method.toUpperCase(), routePattern);
    }

    record AuditRoute(
            String action,
            String resourceType,
            String resourcePathVariable,
            boolean resourceFromLocation) {

        static AuditRoute created(String action, String resourceType) {
            return new AuditRoute(action, resourceType, null, true);
        }

        static AuditRoute path(String action, String resourceType, String resourcePathVariable) {
            return new AuditRoute(action, resourceType, resourcePathVariable, false);
        }
    }

    private record RouteKey(String method, String routePattern) {
    }
}
