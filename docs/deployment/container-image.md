# FleetOps OCI Container Image

FleetOps ships a runtime-only OCI image for the executable `fleetops-bootstrap` Spring Boot application.

## Build locally

First produce and verify the executable jar:

```bash
mvn clean verify
```

Then build the runtime image:

```bash
docker build --tag fleetops-enterprise-platform:local .
```

The Docker build intentionally consumes the already packaged `fleetops-bootstrap/target/fleetops-bootstrap-*.jar`; it does not compile source code inside the image.

## Validate the runtime user

The image runs as the dedicated non-root numeric identity `10001:10001`:

```bash
docker image inspect --format '{{.Config.User}}' fleetops-enterprise-platform:local
```

Expected output:

```text
10001:10001
```

You can also verify the Java runtime without starting FleetOps dependencies:

```bash
docker run --rm --entrypoint java fleetops-enterprise-platform:local -version
```

## Runtime configuration

The image contains no environment-specific credentials. Existing Spring Boot environment variables remain authoritative, including:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`
- `SECURITY_JWT_ISSUER_URI`, `SECURITY_JWK_SET_URI`
- `OTEL_TRACES_EXPORTER_ENABLED`, `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`, `OTEL_TRACES_SAMPLING_PROBABILITY`
- `SERVER_PORT`

The JVM receives container-aware defaults through `JAVA_TOOL_OPTIONS`: percentage-based heap sizing, fail-fast behavior on out-of-memory, and `/tmp` as the Java temporary directory. Deployment environments can override `JAVA_TOOL_OPTIONS` when their runtime policy requires different JVM settings.

## Health and observability

FleetOps continues to expose its existing Actuator health and observability endpoints. Liveness/readiness probe support remains enabled in Spring configuration. The image itself does not embed infrastructure-specific health tooling; orchestration-specific probe wiring is handled by the deployment environment.

A following increment will add an optional Docker Compose application profile so FleetOps can join the existing PostgreSQL, RabbitMQ, Redis, Keycloak, Prometheus, Grafana, and Jaeger stack without requiring IntelliJ.
