# ADR-011: OpenTelemetry Distributed Tracing

- Status: Accepted
- Date: 2026-08-20

## Context

FleetOps already exposes Prometheus metrics and integration-specific operational gauges. Operators can measure backlog and failure states, but they still need request-level execution context across HTTP, messaging, logs, and audit records.

Tracing is an infrastructure concern. Domain and application modules should not depend directly on OpenTelemetry SDK types or a specific tracing backend.

## Decision

Use Spring Boot's Micrometer Tracing integration with OpenTelemetry and OTLP from the bootstrap boundary.

The implementation uses `spring-boot-starter-opentelemetry` and keeps application code on Micrometer Tracing APIs where explicit trace access is required.

### Propagation

- Incoming and outgoing HTTP tracing uses Spring Boot/Spring Framework observation instrumentation and W3C trace context.
- RabbitMQ template and listener observations are enabled so Spring AMQP can propagate tracing context through message headers.
- Application code should use Spring Boot auto-configured HTTP client builders when outbound HTTP clients are introduced so trace propagation is retained.

### Export

Trace export is disabled by default and enabled explicitly by deployment configuration. This prevents development, tests, and CI from silently depending on an external collector.

The OTLP endpoint and sampling probability are environment-driven:

- `OTEL_TRACES_EXPORTER_ENABLED`
- `OTEL_TRACES_SAMPLING_PROBABILITY`
- `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`
- `OTEL_SERVICE_NAME`

The default sampler remains parent-based trace-id-ratio sampling. Tests use 100% sampling while keeping external export disabled.

### Correlation

Spring Boot's tracing integration places `traceId` and `spanId` in the logging MDC, so application log lines can be correlated with traces.

FleetOps keeps its existing `X-Correlation-ID` as the stable business/request correlation identifier. Audit records additionally persist the active `traceId` and `spanId` as safe metadata when a trace is present. Trace identifiers do not replace the existing correlation ID.

Trace IDs, span IDs, correlation IDs, shipment references, user identifiers, and other request-specific identifiers remain prohibited as metric tags.

### Local Backend

The local Docker Compose stack includes Jaeger v2 with OTLP HTTP/gRPC ingestion and its query UI. Jaeger uses transient local storage in this setup and is intended for development and portfolio demonstration, not as a production persistence design.

## Consequences

- HTTP requests, RabbitMQ messaging, logs, and audit records can be connected through trace context.
- Domain/application modules remain independent of OpenTelemetry infrastructure.
- OTLP remains vendor-neutral; the local Jaeger backend can be replaced without changing domain behavior.
- CI does not require a trace collector.
- Operators retain both business correlation IDs and technical trace IDs for investigation.
- Future outbound HTTP clients must be created from Spring Boot auto-configured builders to retain automatic trace propagation.
