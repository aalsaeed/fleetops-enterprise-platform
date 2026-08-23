# FleetOps Engineering Case Study

## Executive Summary

FleetOps Enterprise Platform is a production-oriented backend reference implementation for fleet and ERP-integrated logistics operations.

The project is intentionally focused on the engineering problems that appear after basic CRUD is no longer enough: domain consistency, asynchronous integration reliability, duplicate delivery, recovery, role-based access control, immutable auditability, observability, concurrent updates, container hardening, and reproducible delivery.

The result is a runnable Java 21 / Spring Boot modular monolith with PostgreSQL, RabbitMQ, Redis, Keycloak, Prometheus, Grafana, OpenTelemetry, Jaeger, Docker Compose, Testcontainers, and GitHub Actions.

## The Engineering Problem

A fleet platform may look simple when reduced to drivers, vehicles, and trips. The difficult part is keeping those concepts correct while they interact with external systems and operational failures.

FleetOps was designed around the following questions:

- How can trip state remain consistent when an ERP handoff fails after the business transaction commits?
- How should duplicate or redelivered messages be handled safely?
- How can failed integration messages be retried without creating an infinite poison-message loop?
- How can operators see the state of the integration pipeline without querying application tables manually?
- How should application roles differ between read, operational write, recovery, and audit access?
- How can an administrator reconstruct who performed an action and correlate it with HTTP requests and traces?
- How should simultaneous updates fail without silently overwriting another user's changes?
- How can the same platform be started locally with the infrastructure needed to demonstrate those behaviors?

## Architectural Direction

FleetOps uses a **modular monolith** rather than starting with microservices.

That choice is deliberate. The application keeps a single deployment unit and transaction boundary while enforcing domain boundaries in the codebase. The modules are separated by responsibility and use explicit ports/adapters instead of sharing persistence internals.

Core modules:

| Module | Responsibility |
| --- | --- |
| `fleetops-driver` | Driver lifecycle and domain rules |
| `fleetops-vehicle` | Vehicle lifecycle and operational state |
| `fleetops-trip` | Trip aggregate, assignment, lifecycle, and invariants |
| `fleetops-integration` | ERP contracts, reliable messaging, retries, recovery, and operations |
| `fleetops-audit` | Immutable audit model and query contracts |
| `fleetops-common` | Small cross-cutting technical contracts |
| `fleetops-bootstrap` | Runtime composition, security, HTTP audit, and observability wiring |

The architecture can be split later if independent deployment or scaling becomes a measured requirement. Distribution is not used as a substitute for modularity.

## Reliable ERP Integration

### Failure being avoided

A naive implementation can update the FleetOps database and then publish a message to an ERP or broker. If the database commit succeeds and the message publication fails, the two systems diverge.

FleetOps avoids that dual-write failure mode with a **transactional outbox**.

Business state and the outbound integration record are persisted first. A publisher then claims pending outbox records and sends them through RabbitMQ. Publication state is persisted so failed attempts can be retried and recovered.

### Consumer safety

Inbound ERP shipment processing uses an **inbox/idempotency boundary**. Messages can be redelivered without executing the business effect repeatedly.

The integration implementation includes:

- transactional outbox persistence;
- inbox/idempotency records;
- RabbitMQ publishing and consumption;
- bounded retry/backoff;
- stale-processing recovery;
- dead-letter handling;
- explicit failed-message requeue operations;
- integration operations snapshots;
- operational metrics for pipeline state and DLQ depth.

### Why this matters

The objective is not to claim exactly-once transport. The objective is to create **effectively-once business behavior** from at-least-once delivery by combining durable state, idempotency, and controlled recovery.

## Security Model

FleetOps is a stateless OAuth2 Resource Server backed by Keycloak.

The local realm defines three service-account roles:

- `FLEETOPS_USER`
- `FLEETOPS_OPERATOR`
- `FLEETOPS_ADMIN`

The authorization boundary is intentionally differentiated:

- normal authenticated roles can read standard `/api/v1/**` resources;
- operators and administrators can perform normal business writes;
- integration recovery operations require administrator authority;
- audit queries require administrator authority;
- Prometheus scraping requires operator or administrator authority;
- health and info endpoints remain accessible for platform health checks.

This demonstrates authorization as an application policy rather than a UI-only concern.

## Immutable Auditability

FleetOps records audit events separately from normal business entities.

HTTP audit capture records security and request context without placing audit logic inside controllers. Correlation identifiers support request-level investigation, while active OpenTelemetry trace/span identifiers can be stored as audit metadata when available.

The design separates three concepts that are often incorrectly mixed:

- **business correlation ID** for workflow/request investigation;
- **trace/span IDs** for distributed telemetry;
- **audit records** for durable accountability.

Audit access itself is protected by the administrator role.

## Concurrency Protection

Driver, vehicle, and trip aggregates carry revision state and use optimistic locking.

When two clients update the same aggregate concurrently, FleetOps rejects the stale write instead of silently allowing the last writer to overwrite the first.

The public API translates persistence-provider conflicts into a provider-neutral application response so clients do not need to understand Hibernate or database-specific exceptions.

## Observability

FleetOps provides three complementary operational views.

### Metrics

Spring Boot Actuator and Micrometer expose Prometheus metrics. The provisioned Grafana dashboard includes signals for:

- integration snapshot availability;
- ERP shipment DLQ depth;
- outbox state counts;
- inbox state counts;
- HTTP request rate;
- HTTP 5xx rate;
- JVM heap usage;
- JVM live threads.

High-cardinality business identifiers are intentionally excluded from metric labels.

### Traces

Micrometer Tracing with OpenTelemetry exports OTLP traces to Jaeger when enabled.

Inbound HTTP requests and RabbitMQ operations participate in tracing so an asynchronous workflow can be investigated across request and message boundaries.

### Logs and correlation

Trace IDs and span IDs are placed into MDC when a trace is active. FleetOps also preserves an independent correlation identifier for business/request investigation.

The separation prevents Prometheus cardinality problems while keeping detailed identifiers where they belong: logs, traces, and audit metadata.

## Runtime and Container Hardening

FleetOps ships as a container image rather than relying on an IDE-specific launch path.

The runtime image is designed to:

- run as a non-root UID/GID (`10001:10001`);
- use an explicit application entry point;
- expose readiness suitable for container smoke testing;
- run in the same Compose topology used by the portfolio demonstration.

The local Compose stack includes the application and supporting PostgreSQL, RabbitMQ, Redis, Keycloak, Prometheus, Grafana, and Jaeger services.

## Automated Verification

The repository contains unit and integration coverage for the areas that carry the most engineering risk, including:

- driver, vehicle, and trip domain behavior;
- REST API behavior;
- JPA persistence adapters;
- Keycloak/JWT authorization;
- HTTP audit capture and audit persistence;
- transactional outbox behavior;
- RabbitMQ publication and consumption;
- inbox processing and dead-letter behavior;
- retry/recovery flows;
- operational integration metrics;
- Prometheus endpoint protection;
- OpenTelemetry tracing;
- optimistic concurrency conflicts.

GitHub Actions goes beyond `mvn verify`. The pipeline also builds the OCI image, verifies the non-root runtime user, validates the Compose application profile, starts the containerized application, and executes a readiness smoke test.

## Key Engineering Tradeoffs

### Modular monolith over microservices

**Chosen:** one deployable application with strict internal boundaries.

**Reason:** lower operational complexity, simpler transactions, and easier local reproducibility while still demonstrating architecture discipline.

**Extraction trigger:** independent scaling, deployment, availability, ownership, or technology requirements—not fashion.

### At-least-once messaging over "exactly once" claims

**Chosen:** durable outbox/inbox plus idempotent processing and recovery.

**Reason:** the design is explicit about transport realities and protects business effects instead of depending on an unrealistic cross-system transaction guarantee.

### Protected metrics endpoint

**Chosen:** Prometheus authenticates through Keycloak using an operator client.

**Reason:** operational data is treated as an authenticated resource rather than being exposed because it is "only metrics."

### Tracing disabled by default

**Chosen:** external OTLP export is opt-in in the local runtime.

**Reason:** the application remains runnable without a tracing backend while the full observability path can be enabled for demonstrations and diagnostics.

## Evidence Map

| Claim | Repository evidence |
| --- | --- |
| Modular architecture | module structure + `docs/architecture/README.md` + ADR-001 |
| Reliable messaging | outbox/inbox migrations, integration services, RabbitMQ adapters, recovery tests |
| Role-based security | `SecurityConfiguration`, Keycloak realm, authorization integration tests |
| Auditability | `fleetops-audit`, HTTP audit capture, admin query tests |
| Metrics | `IntegrationOperationalMetrics`, Prometheus configuration, Grafana dashboard |
| Tracing | OpenTelemetry integration test + Jaeger runbook |
| Concurrency | aggregate revisions + optimistic-locking integration tests + ADR-013/014 |
| Container hardening | Dockerfile + OCI packaging ADR + CI non-root verification |
| Reproducibility | Docker Compose profiles + CI Compose validation and smoke test |
| Architecture governance | ADR series under `docs/adr/` |

## What the Project Demonstrates

FleetOps is not intended to prove that a single developer can create another fleet CRUD application.

It is intended to demonstrate the ability to reason about and implement:

- domain boundaries;
- integration failure modes;
- asynchronous consistency;
- idempotency and recovery;
- application security;
- audit design;
- concurrency control;
- production diagnostics;
- containerized delivery;
- automated architectural verification.

That is the engineering value of the project.