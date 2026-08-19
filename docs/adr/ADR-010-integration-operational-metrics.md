# ADR-010: Integration Operational Metrics

- Status: Accepted
- Date: 2026-08-19

## Context

FleetOps already exposes an operational reconciliation snapshot for the ERP integration pipeline. It includes transactional Outbox state, Inbox processing state, stale work detection, failed-message samples, and RabbitMQ dead-letter queue depth.

Prometheus now provides the platform metrics surface, but directly exporting identifiers or failure samples as metric labels would create unbounded cardinality and duplicate existing operational APIs.

## Decision

Expose FleetOps integration reliability as Micrometer gauges at the bootstrap/observability boundary while reusing `GetIntegrationOperationsUseCase` as the source of truth.

The metric families are:

- `fleetops.integration.outbox.messages{state=...}`
  - `pending`
  - `publishing`
  - `published`
  - `failed`
  - `stale_publishing`
- `fleetops.integration.inbox.messages{state=...}`
  - `pending`
  - `processing`
  - `processed`
  - `failed`
  - `stale_processing`
- `fleetops.integration.dlq.messages`
- `fleetops.integration.dlq.available`
- `fleetops.integration.snapshot.available`

Only fixed, low-cardinality state labels are allowed. Message IDs, aggregate IDs, shipment references, user subjects, correlation IDs, exception messages, and other request-specific values must not become metric labels.

The observability adapter caches the reconciliation snapshot for a short, configurable interval. This avoids performing the PostgreSQL and RabbitMQ snapshot work independently for every gauge during a single Prometheus scrape. The default TTL is five seconds and can be changed with `OBSERVABILITY_INTEGRATION_SNAPSHOT_TTL_MS`.

If a snapshot refresh fails, the adapter retains the last successful values for continuity and sets `fleetops.integration.snapshot.available` to `0` until a later refresh succeeds. RabbitMQ DLQ availability remains separately visible through `fleetops.integration.dlq.available`.

## Consequences

- Domain and application modules remain independent of Micrometer.
- Metrics reuse the same operational interpretation used by the reconciliation API.
- Prometheus gets stable backlog/failure/staleness signals without exposing business identifiers.
- Scraping does not multiply database/RabbitMQ reconciliation work by the number of gauges.
- Operators can alert on stale work, failed work, DLQ depth, and snapshot availability.
- Detailed failed-message investigation remains in the authenticated operations API rather than metric labels.
