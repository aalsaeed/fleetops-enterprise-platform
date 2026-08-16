# FleetOps Enterprise Platform

FleetOps is an enterprise-grade reference platform for fleet, trip, and ERP-integrated logistics operations.

The project is being built as a public engineering portfolio focused on:

- Java backend engineering
- Spring Boot
- Enterprise integration patterns
- Reliable asynchronous messaging
- Security and auditability
- Automated testing
- Production observability
- Architecture decision records

## Project Status

**Foundation phase — active development**

The first milestone establishes the modular architecture, local development environment, core domain boundaries, database migrations, automated tests, and CI pipeline.

## Planned Business Capabilities

- Driver management
- Vehicle management
- Trip lifecycle and assignment
- Location ingestion
- ERP integration simulation
- Reliable messaging and reconciliation
- Audit trails
- Security and role-based access control
- Health, metrics, logging, and tracing

## Engineering Approach

The first production-shaped version is intentionally designed as a **modular monolith**. The goal is to keep operational complexity low while enforcing clear business boundaries and leaving room for selective service extraction when independent scaling or deployment is justified.

## Technology Direction

- Java 21 LTS
- Spring Boot 4.x
- Maven
- PostgreSQL
- Flyway
- RabbitMQ
- Redis
- Docker Compose
- JUnit 5
- Testcontainers
- OpenAPI
- GitHub Actions
- Prometheus / Grafana

## Repository Roadmap

- `v0.1` — foundation, drivers, vehicles, trips, REST API, persistence, testing
- `v0.2` — ERP simulator, messaging, outbox/inbox, idempotency, reconciliation
- `v0.3` — authentication, authorization, audit, observability
- `v1.0` — documented reference architecture with production-oriented runbooks and case studies

## Documentation

Architecture decisions and design documents will live under `docs/` and evolve with the implementation.

## License

A license will be selected before the first public release. Until then, all rights are reserved.
