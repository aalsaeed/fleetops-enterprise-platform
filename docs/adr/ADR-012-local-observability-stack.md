# ADR-012: Local Prometheus and Grafana Operations Stack

- Status: Accepted
- Date: 2026-08-20

## Context

FleetOps already exposes a protected Prometheus endpoint, publishes bounded integration pipeline gauges, and exports distributed traces through OTLP when enabled. The portfolio still needs a reproducible local operations view that demonstrates how those signals are consumed without weakening endpoint security or coupling domain code to monitoring infrastructure.

## Decision

Add Prometheus and Grafana to the local Docker Compose stack under the `observability` profile.

Prometheus scrapes the protected FleetOps `/actuator/prometheus` endpoint using the existing local Keycloak operator service account through OAuth 2.0 client credentials. The scrape target is `host.docker.internal:8080` because the application is normally started from the developer IDE while infrastructure runs in Docker.

Grafana is provisioned automatically with:

- a Prometheus datasource;
- a `FleetOps Operations` dashboard;
- integration Outbox/Inbox state panels;
- DLQ and integration snapshot availability panels;
- HTTP request/error-rate panels;
- JVM memory and thread panels.

Prometheus keeps a named local data volume. Grafana also uses a named volume while its datasource and dashboard definitions remain version-controlled and read-only inside the container.

The local Prometheus configuration intentionally uses the repository's documented non-production operator client secret. Real deployment secrets must not be committed and production monitoring should source credentials from a secret manager or workload identity mechanism.

## Consequences

- The complete metrics path can be demonstrated locally without making `/actuator/prometheus` public.
- The dashboard emphasizes operationally useful and bounded metrics rather than request-specific identifiers.
- Local observability infrastructure remains optional through the Compose profile.
- The application remains executable from IntelliJ/Maven without requiring containerization.
- Developers who change the local operator client secret must keep the local Prometheus scrape credential aligned.
- Production Grafana/Prometheus deployment topology remains outside the scope of this reference implementation.
