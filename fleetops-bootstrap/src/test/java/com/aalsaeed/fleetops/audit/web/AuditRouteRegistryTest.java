package com.aalsaeed.fleetops.audit.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRouteRegistryTest {

    private final AuditRouteRegistry registry = new AuditRouteRegistry();

    @Test
    void mapsAllAuditedWriteRoutes() {
        assertRoute("POST", "/api/v1/drivers", "DRIVER_CREATE", "DRIVER");
        assertRoute("PATCH", "/api/v1/drivers/{id}/status", "DRIVER_STATUS_CHANGE", "DRIVER");
        assertRoute("POST", "/api/v1/vehicles", "VEHICLE_CREATE", "VEHICLE");
        assertRoute("PATCH", "/api/v1/vehicles/{id}/status", "VEHICLE_STATUS_CHANGE", "VEHICLE");
        assertRoute("POST", "/api/v1/trips", "TRIP_CREATE", "TRIP");
        assertRoute("PUT", "/api/v1/trips/{id}/assignment", "TRIP_ASSIGN_RESOURCES", "TRIP");
        assertRoute("POST", "/api/v1/trips/{id}/start", "TRIP_START", "TRIP");
        assertRoute("POST", "/api/v1/trips/{id}/complete", "TRIP_COMPLETE", "TRIP");
        assertRoute("POST", "/api/v1/trips/{id}/cancel", "TRIP_CANCEL", "TRIP");
        assertRoute(
                "POST",
                "/api/v1/integration/operations/outbox/{messageId}/requeue",
                "INTEGRATION_OUTBOX_REQUEUE",
                "INTEGRATION_OUTBOX");
        assertRoute(
                "POST",
                "/api/v1/integration/operations/inbox/{messageId}/requeue",
                "INTEGRATION_INBOX_REQUEUE",
                "INTEGRATION_INBOX");
    }

    @Test
    void ignoresReadAndUnknownRoutes() {
        assertThat(registry.resolve("GET", "/api/v1/drivers/{id}")).isEmpty();
        assertThat(registry.resolve("POST", "/api/v1/unknown")).isEmpty();
    }

    private void assertRoute(String method, String pattern, String action, String resourceType) {
        AuditRouteRegistry.AuditRoute route = registry.resolve(method, pattern).orElseThrow();
        assertThat(route.action()).isEqualTo(action);
        assertThat(route.resourceType()).isEqualTo(resourceType);
    }
}
