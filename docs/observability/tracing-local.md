# Local OpenTelemetry Tracing

FleetOps uses Micrometer Tracing with OpenTelemetry and exports traces through OTLP. The local Docker Compose stack includes Jaeger for development and demonstration.

## Start the trace backend

```bash
docker compose up -d jaeger
```

Jaeger exposes:

- UI: `http://127.0.0.1:16686`
- OTLP gRPC: `127.0.0.1:4317`
- OTLP HTTP: `http://127.0.0.1:4318/v1/traces`

The ports can be changed with `JAEGER_UI_PORT`, `OTEL_GRPC_PORT`, and `OTEL_HTTP_PORT`.

## Enable trace export for the application

External trace export is intentionally disabled by default. Enable it when running the local backend:

```text
OTEL_TRACES_EXPORTER_ENABLED=true
OTEL_TRACES_SAMPLING_PROBABILITY=1.0
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://127.0.0.1:4318/v1/traces
OTEL_SERVICE_NAME=fleetops-enterprise-platform
```

A production deployment should normally use a lower sampling probability and a deployment-specific OTLP collector endpoint.

## What is traced

Spring Boot automatically instruments inbound HTTP requests. Spring AMQP observation is enabled for RabbitTemplate sends and Rabbit listener containers so trace context can cross RabbitMQ message boundaries.

Spring Boot also places the active `traceId` and `spanId` into the logging MDC. FleetOps audit capture stores those identifiers as audit metadata when a trace is active while preserving the existing `X-Correlation-ID` as the independent request/business correlation identifier.

## Verify locally

1. Start Jaeger.
2. Run FleetOps with OTLP export enabled.
3. Call one or more FleetOps APIs.
4. Open the Jaeger UI.
5. Select service `fleetops-enterprise-platform` and search for traces.

For message-driven flows, publish or process an ERP shipment event and inspect the HTTP/Rabbit spans belonging to the same trace where propagation applies.

## Safety and cardinality

Trace IDs and span IDs are useful for investigation but are high-cardinality identifiers. They must not be added to Prometheus metric tags. They are permitted in logs, trace data, and immutable audit metadata.

The local Jaeger container uses transient storage and is not a production persistence recommendation.
