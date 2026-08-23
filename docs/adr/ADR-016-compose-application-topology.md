# ADR-016: Docker Compose Application Deployment Topology

- Status: Accepted
- Date: 2026-08-24

## Context

FleetOps already provides a hardened OCI runtime image and a local Compose stack for PostgreSQL, RabbitMQ, Redis, Keycloak, Prometheus, Grafana, and Jaeger. Until this increment, however, the application process itself still had to be started from IntelliJ or a host Maven command when exercising the local platform.

A portfolio-grade enterprise reference should demonstrate that the same application artifact can run as an independently deployable container while preserving the security, integration, persistence, and observability boundaries already established by the platform.

## Decision

Add an `application` Docker Compose profile containing a `fleetops` service built from the hardened OCI image.

The container uses Compose-internal DNS for infrastructure connectivity:

- PostgreSQL through `postgres:5432`
- RabbitMQ through `rabbitmq:5672`
- Keycloak signing keys through `keycloak:8080`

The JWT issuer remains the host-facing Keycloak issuer (`http://127.0.0.1:8180/realms/fleetops` by default). FleetOps validates the token `iss` claim against that public issuer while retrieving JWK signing keys through the internal Keycloak service URL. This avoids changing token semantics simply because the application process moved inside Docker.

The application host port is configurable through `FLEETOPS_PORT`, defaulting to `8080`. The image remains non-root and no credentials are baked into it.

OTLP export remains optional. When enabled in the Compose topology, FleetOps uses the internal Jaeger OTLP HTTP endpoint by default. Prometheus keeps its existing host-published scrape path through `host.docker.internal`, allowing the same local Prometheus configuration to scrape FleetOps whether it runs from IntelliJ or from the Compose application profile.

Docker-level health checks only verify that the application TCP port is accepting connections. The authoritative application readiness signal remains Spring Boot's `/actuator/health/readiness` endpoint and is exercised explicitly in CI.

## CI Validation

The normal CI pipeline continues to run the full Maven verification and hardened image build. It additionally:

1. validates the `application` Compose profile;
2. starts the containerized application stack using the already-built CI image;
3. polls `/actuator/health/readiness` until it reports `UP`;
4. emits service diagnostics on failure; and
5. tears down containers and volumes after the smoke test.

## Consequences

- FleetOps can be demonstrated without starting the application from an IDE.
- The same OCI image is used for local Compose, CI smoke validation, and future OCI-compatible production orchestrators.
- Runtime topology remains outside domain and application modules.
- Security issuer semantics stay stable across host and container execution.
- The Compose stack remains a local/demo topology rather than a production orchestrator.
- Production deployments still require platform-managed secrets, durable infrastructure, resource policies, and orchestrator-native health probes.
