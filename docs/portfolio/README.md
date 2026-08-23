# FleetOps Portfolio Pack

This directory is the shortest path for evaluating FleetOps as an engineering portfolio.

The goal is not to duplicate the technical documentation. It is to connect the implementation, architecture decisions, runtime proof, and presentation material into one review path.

## Recommended Review Path

| Time | Review | What it demonstrates |
| --- | --- | --- |
| 2 minutes | Root `README.md` | Platform positioning, architecture at a glance, implemented capabilities, and runnable stack |
| 5–10 minutes | [Engineering Case Study](CASE-STUDY.md) | Engineering problem, architecture decisions, reliability model, security, audit, observability, and tradeoffs |
| 5 minutes | [Architecture Overview](../architecture/README.md) | Module boundaries, integration flow, runtime topology, and observability design |
| 10 minutes | [Demo Runbook](DEMO-RUNBOOK.md) | Repeatable operational proof using Keycloak, RabbitMQ, Grafana, Jaeger, APIs, and CI |
| Interview / presentation | [Presentation Narrative](PRESENTATION.md) | Seven-slide story for explaining the platform clearly without walking through source code file by file |

## Evidence Principle

Every portfolio claim should be supported by at least one of the following:

1. **Implementation evidence** — source code, configuration, migrations, tests, or ADRs.
2. **Runtime evidence** — a healthy service, API response, metric, trace, queue state, security behavior, or recovery operation.
3. **Automation evidence** — CI verification showing that the behavior is reproducible.

Screenshots are useful only when they prove a distinct capability. They should not be used as decoration.

## Evidence Set

The preferred public evidence set is intentionally small:

1. Full Docker Compose stack healthy.
2. Grafana `FleetOps Operations` dashboard.
3. Jaeger trace crossing an HTTP or message-driven path.
4. Keycloak realm / client / role configuration without secrets.
5. RabbitMQ topology or queue state relevant to the ERP integration flow.
6. Successful GitHub Actions CI run showing verify, OCI hardening, Compose validation, and readiness smoke test.

See [Evidence Asset Guide](assets/README.md) for naming and redaction rules.

## What the Portfolio Should Communicate

A reviewer should be able to conclude that FleetOps demonstrates:

- explicit domain boundaries inside a modular monolith;
- reliable asynchronous ERP integration rather than fragile dual writes;
- idempotent processing, retries, recovery, and dead-letter handling;
- OAuth2/JWT authorization with role-based policies;
- immutable auditability and correlation;
- optimistic concurrency protection;
- metrics and distributed tracing designed for production diagnostics;
- hardened, non-root container packaging;
- reproducible local deployment and automated verification.

## Scope Statement

FleetOps is a reference implementation and public engineering portfolio. Its purpose is to demonstrate production-oriented backend and integration engineering decisions, not to present itself as a turnkey commercial fleet-management product.
