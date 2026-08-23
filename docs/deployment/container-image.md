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

FleetOps continues to expose its existing Actuator health and observability endpoints. Liveness/readiness probe support remains enabled in Spring configuration. The image itself does not embed infrastructure-specific HTTP tooling; orchestration-specific probe wiring remains external to the application image.

The Compose application service uses a lightweight TCP container health check, while CI and deployment operators verify the actual Spring Boot readiness endpoint:

```text
GET /actuator/health/readiness
```

## Run FleetOps inside Docker Compose

The `application` profile runs FleetOps from the hardened OCI image together with the existing PostgreSQL, RabbitMQ, Redis, and Keycloak services.

Build the verified jar and image first:

```bash
mvn clean verify
docker build --tag fleetops-enterprise-platform:local .
```

Then start the containerized application stack:

```bash
docker compose --profile application up -d --no-build
```

FleetOps is available at:

```text
http://127.0.0.1:8080
```

Verify readiness:

```bash
curl --fail http://127.0.0.1:8080/actuator/health/readiness
```

Expected status is `UP`.

The Compose service uses internal service DNS for infrastructure connectivity:

- PostgreSQL: `postgres:5432`
- RabbitMQ: `rabbitmq:5672`
- Keycloak JWK endpoint: `keycloak:8080`

The JWT issuer intentionally remains the public/local issuer (`http://127.0.0.1:8180/realms/fleetops` by default), while the application fetches signing keys through Keycloak's internal Compose address. This keeps token issuer validation consistent with tokens obtained from the host-facing Keycloak endpoint.

## Run the complete application + observability stack

After the image exists locally, start both profiles:

```bash
docker compose --profile application --profile observability up -d --no-build
```

This starts FleetOps plus PostgreSQL, RabbitMQ, Redis, Keycloak, Jaeger, Prometheus, and Grafana. Prometheus continues to scrape the host-published FleetOps port through `host.docker.internal`, so the same scrape configuration works whether FleetOps is started from IntelliJ or from the Compose application profile.

Useful local endpoints:

```text
FleetOps readiness: http://127.0.0.1:8080/actuator/health/readiness
Keycloak:           http://127.0.0.1:8180
Prometheus:         http://127.0.0.1:9090
Grafana:            http://127.0.0.1:3000
Jaeger:             http://127.0.0.1:16686
RabbitMQ UI:        http://127.0.0.1:15672
```

OTLP trace export stays disabled by default. When observability is running, enable it through `OTEL_TRACES_EXPORTER_ENABLED=true`; the Compose application service uses `http://jaeger:4318/v1/traces` by default through `FLEETOPS_CONTAINER_OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`.

## Stop the stack

Stop containers while keeping persistent volumes:

```bash
docker compose --profile application --profile observability down
```

For a complete local reset including PostgreSQL, Redis, Prometheus, and Grafana volumes:

```bash
docker compose --profile application --profile observability down -v
```

The Compose topology is intended for development, demonstrations, and CI smoke validation. Production environments should run the same OCI image under an orchestrator with externally managed secrets, durable infrastructure, and platform-native liveness/readiness probes.
