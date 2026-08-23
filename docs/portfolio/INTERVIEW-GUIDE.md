# FleetOps Technical Interview Guide

This guide turns the implementation into concise architecture answers for technical interviews. The intent is to explain the engineering reasoning, not to memorize scripts.

## 1. Why did you choose a modular monolith instead of microservices?

FleetOps needs clear Driver, Vehicle, Trip, Integration, and Audit boundaries, but those boundaries do not automatically require independent deployment. A modular monolith keeps transactions, local development, testing, and operations simpler while still enforcing separation in code and dependencies.

I would extract a module only when there is a measurable need for independent scaling, deployment cadence, ownership, availability, or technology choice.

## 2. What problem does the transactional outbox solve?

It prevents a business transaction and message publication from becoming an unsafe dual write.

The business state and an outbox record are committed in the same database transaction. Publication to RabbitMQ happens afterward and can be retried independently. This means a temporary broker failure does not lose the integration intent.

## 3. Why do you also need an inbox if RabbitMQ already delivers messages?

At-least-once delivery means duplicates are possible. The inbox provides consumer-side idempotency by recording message identity and processing state.

The broker is responsible for delivery. The application is responsible for making repeated delivery safe.

## 4. How do retries differ from dead-letter handling?

Retries are appropriate for failures that may succeed later, such as transient infrastructure or dependency failures. Retry attempts are bounded and observable.

Dead-letter handling is the terminal path for messages that cannot be processed safely within the retry policy. Those messages remain visible for investigation or controlled recovery instead of disappearing.

## 5. How is manual recovery handled?

FleetOps exposes integration operations that allow failed outbox or inbox items to be requeued when recovery is valid. Recovery endpoints are administrative operations, not normal user actions, and therefore require the strongest application role.

The important design point is that recovery is explicit and observable rather than implemented through ad-hoc database edits.

## 6. How is authorization enforced?

Keycloak provides identity and issues JWT access tokens. Spring Security validates the token and maps supported realm roles into application authorities.

FleetOps separates read, write, operational recovery, metrics, and audit access. For example, normal users can read allowed APIs, operators can perform operational writes, and administrative capabilities are reserved for `FLEETOPS_ADMIN`.

## 7. Why is authentication not enough?

Authentication answers who the caller is. Authorization determines what that caller may do. Auditability records what happened, and correlation helps trace the request across logs and distributed operations.

FleetOps treats these as separate controls instead of assuming that a valid token is sufficient security.

## 8. How do you prevent lost updates?

Mutable aggregates use optimistic concurrency. A client operates on a known revision, and the persistence layer rejects a stale update when another writer has already changed the same aggregate.

This is preferable to silent last-writer-wins behavior because conflicting business updates become explicit and recoverable.

## 9. Why use PostgreSQL and Flyway?

PostgreSQL provides the transactional guarantees needed by the domain and integration patterns. Flyway makes schema evolution repeatable, versioned, and part of the same engineering baseline as the application code.

The database schema is therefore reproducible in local development, CI, and deployment rather than being maintained manually.

## 10. What do Prometheus, Grafana, and Jaeger each provide?

Prometheus stores time-series metrics. Grafana presents operational dashboards. Jaeger stores and visualizes distributed traces exported through OpenTelemetry.

Metrics answer questions such as “is the system healthy and how is it behaving over time?” Traces answer “what happened to this specific request or message path?” They solve different diagnostic problems.

## 11. How do you control observability cardinality?

High-cardinality identifiers such as trip IDs, driver IDs, message IDs, correlation IDs, trace IDs, and user subjects should not become Prometheus labels.

They are appropriate in logs, traces, and audit records where individual-event investigation is expected. Metrics use bounded dimensions suitable for aggregation.

## 12. What makes the container image production-oriented?

The runtime image is built to run as a non-root user and the CI pipeline verifies that property. The project also validates the Compose application profile and performs a readiness smoke test against the containerized application.

The goal is to test packaging and runtime assumptions automatically rather than treating `docker build` success as sufficient evidence.

## 13. What does the CI pipeline prove?

The pipeline verifies more than compilation. It runs Maven verification, builds the OCI image, checks non-root execution, validates Docker Compose configuration, starts the stack, and checks application readiness.

This provides repeatable evidence that source code, packaging, deployment topology, and runtime startup remain compatible.

## 14. Why is Redis present if the core system uses PostgreSQL?

Redis is infrastructure for concerns that benefit from fast ephemeral or cache-oriented storage; it is not the system of record for the core aggregates. Durable business state remains in PostgreSQL.

This keeps persistence responsibilities explicit and avoids treating Redis as an accidental second source of truth.

## 15. What would you change first if FleetOps had to scale significantly?

I would measure the bottleneck before changing the architecture. Likely candidates would be integration throughput, read-heavy operational APIs, or a domain module with independent availability/scaling requirements.

The modular boundaries allow selective extraction, but moving to microservices would be a response to evidence rather than a default modernization step.

## 16. What is the strongest engineering story in the project?

The strongest story is the combination of domain correctness and production boundaries: trip and resource state are not designed in isolation from ERP delivery guarantees, authorization, audit, concurrency, metrics, tracing, packaging, and CI.

That demonstrates how an enterprise backend behaves under failure and operational pressure, not just how its happy-path endpoints work.

## Interview Rule

When answering, use this sequence:

**problem → design choice → failure mode handled → tradeoff → evidence in the repository**.

That structure communicates engineering judgment more clearly than listing technologies.