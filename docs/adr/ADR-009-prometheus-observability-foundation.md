# ADR-009: Prometheus observability foundation

## Status

Accepted

## Context

FleetOps already exposes Spring Boot Actuator health and info endpoints, but it does not export a monitoring-system scrape format. The platform needs production-shaped operational visibility before adding FleetOps-specific integration metrics and distributed tracing.

Prometheus is a suitable first monitoring boundary because Spring Boot Actuator and Micrometer provide native registry integration and a dedicated `/actuator/prometheus` endpoint.

Operational metrics can reveal JVM, process, HTTP, database-pool, and later FleetOps integration behavior. Unlike health probes, the full metrics stream should not be anonymously accessible from the application layer.

## Decision

- Add the Micrometer Prometheus registry to the bootstrap module.
- Expose `/actuator/prometheus` through Actuator.
- Keep `/actuator/health`, health probes, and `/actuator/info` publicly accessible as already designed.
- Require `FLEETOPS_OPERATOR` or `FLEETOPS_ADMIN` authority for `/actuator/prometheus`.
- Keep Prometheus configuration in infrastructure/bootstrap code only; domain and application modules remain independent of Micrometer.
- Avoid high-cardinality metric tags such as trip IDs, driver IDs, vehicle IDs, shipment references, message IDs, correlation IDs, or user subjects.

## Consequences

### Positive

- Prometheus can scrape standard JVM, process, HTTP, connection-pool, and other auto-configured metrics.
- The metrics surface follows the existing OAuth2/RBAC model instead of becoming an anonymous operational endpoint.
- The foundation can be extended with bounded FleetOps-specific integration metrics without changing domain code.

### Trade-offs

- A Prometheus deployment scraping the application must authenticate with an operator/admin-capable service identity or be fronted by trusted infrastructure that provides the required bearer token.
- Additional custom metrics must be reviewed for tag cardinality and query cost before registration.

## Follow-up

Subsequent increments under the observability epic will add FleetOps integration pipeline metrics, OpenTelemetry tracing/OTLP export, and local monitoring/visualization configuration.
