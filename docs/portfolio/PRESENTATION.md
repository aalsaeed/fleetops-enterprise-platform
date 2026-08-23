# FleetOps Portfolio Presentation Narrative

This narrative mirrors the seven-slide portfolio deck and provides a text-first version that can be reviewed directly in GitHub.

## Slide 1 — FleetOps Enterprise Platform

**Message:** FleetOps is a production-oriented fleet operations and ERP integration reference platform.

**Positioning points:**

- modular monolith;
- reliable asynchronous messaging;
- centralized security and RBAC;
- auditability;
- observability;
- reproducible deployment and CI.

The opening should establish that the project is about enterprise backend engineering, not UI breadth.

## Slide 2 — Why FleetOps Is Not a CRUD Demo

**Message:** The hard part of a fleet platform is keeping business state consistent across system boundaries and failure modes.

The platform focuses on problems such as:

- trip lifecycle correctness;
- ERP handoff failures;
- duplicate or redelivered messages;
- authorization boundaries;
- audit requirements;
- concurrent updates;
- production diagnostics.

**Interview point:** explain that architecture was driven by failure modes rather than by adding screens or endpoints.

## Slide 3 — Architecture: One Deployable, Explicit Boundaries

**Message:** FleetOps deliberately uses a modular monolith.

The deployable application contains explicit Driver, Vehicle, Trip, Integration, Audit, Common, and Bootstrap boundaries. PostgreSQL, RabbitMQ, Redis, Keycloak, Prometheus, Grafana, and Jaeger provide infrastructure around the application.

**Tradeoff:** deployment remains operationally simple while module boundaries are kept strong enough to support later extraction when scaling, ownership, or availability requirements justify it.

## Slide 4 — Reliable ERP Integration

**Message:** FleetOps avoids fragile dual writes.

A business transaction records the integration intent in a transactional outbox. Publishing is retried independently. On the receiving side, inbox/idempotency records protect against duplicate processing. RabbitMQ provides transport, with retry, recovery, and dead-letter behavior around failures.

**Key sentence:** database commit and broker availability do not have to succeed at the same instant for the workflow to remain recoverable.

## Slide 5 — Security, Audit, and Concurrency Are Server-Side Concerns

**Message:** correctness and governance are enforced in the backend rather than delegated to clients.

- Keycloak issues JWT access tokens.
- Spring Security maps realm roles to API policies.
- `FLEETOPS_USER`, `FLEETOPS_OPERATOR`, and `FLEETOPS_ADMIN` have intentionally different capabilities.
- HTTP and domain activity can be correlated through immutable audit records.
- optimistic concurrency rejects stale updates instead of silently accepting last-writer-wins behavior.

**Interview point:** distinguish authentication, authorization, auditability, and concurrency protection as separate concerns.

## Slide 6 — Operational Evidence Stack

**Message:** the portfolio should prove the system at runtime, not just describe it.

Show a small set of focused evidence:

1. Docker Compose services healthy.
2. Grafana operations dashboard.
3. Jaeger trace for an HTTP or message path.
4. Keycloak realm / client / role configuration.
5. RabbitMQ queue or topology evidence.
6. GitHub Actions CI success including verify, image hardening, Compose validation, and readiness smoke test.

Do not fill this slide with decorative screenshots. Every image should answer a technical question.

## Slide 7 — Portfolio Value

**Message:** the repository now tells a coherent engineering story before a reviewer opens individual source files.

The intended sequence is:

- README explains what exists;
- Architecture documentation explains how it fits together;
- Case Study explains why the choices were made;
- Demo Runbook proves runtime behavior;
- ADRs preserve architectural reasoning;
- CI proves repeatability.

**Closing sentence:** FleetOps demonstrates how to design, integrate, secure, observe, package, and verify an enterprise backend system—not merely how to expose CRUD APIs.

## Suggested Presentation Length

- Recruiter / hiring manager: **3–5 minutes** using slides 1, 2, 3, 6, and 7.
- Technical interview: **8–12 minutes** using all seven slides with emphasis on slides 3–6.
- Deep architecture discussion: use the slides as navigation, then move into the Case Study, ADRs, source code, and runtime evidence.
