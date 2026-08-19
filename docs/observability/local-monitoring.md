# Local Prometheus and Grafana Monitoring

FleetOps includes an optional local monitoring profile for portfolio demonstration and operational development.

## Components

- Prometheus: `http://127.0.0.1:9090`
- Grafana: `http://127.0.0.1:3000`
- Jaeger: `http://127.0.0.1:16686`
- FleetOps metrics endpoint: `http://127.0.0.1:8080/actuator/prometheus`

The metrics endpoint remains protected by FleetOps RBAC. Prometheus authenticates with the local Keycloak `fleetops-operator-client` through OAuth 2.0 client credentials before every scrape as required by its token lifecycle.

## Start

1. Start the normal FleetOps dependencies and monitoring services:

```bash
docker compose --profile observability up -d
```

2. Run FleetOps locally on port `8080` from IntelliJ or Maven.

3. Verify the Prometheus target at:

```text
http://127.0.0.1:9090/targets
```

The `fleetops` target should become `UP` after Keycloak and the application are ready.

4. Open Grafana:

```text
http://127.0.0.1:3000
```

Default local login:

```text
username: admin
password: fleetops_local_admin
```

Open folder `FleetOps` and dashboard `FleetOps Operations`.

## Dashboard Signals

The provisioned dashboard focuses on:

- integration snapshot availability;
- ERP shipment DLQ depth;
- Outbox state counts;
- Inbox state counts;
- HTTP request rate;
- HTTP 5xx rate;
- JVM heap usage;
- JVM live threads.

The custom integration metrics use bounded state tags only. Trip IDs, driver IDs, vehicle IDs, shipment references, correlation IDs, trace IDs, span IDs, and user subjects are intentionally excluded from Prometheus labels.

## Local Authentication Detail

`infra/observability/prometheus/prometheus.yml` uses the documented local-only Keycloak operator client credentials. The repository credentials are demonstration defaults and are not production secrets.

If `FLEETOPS_OPERATOR_CLIENT_SECRET` is changed for local development, keep the Prometheus `client_secret` aligned before starting the monitoring profile.

A production deployment should inject monitoring credentials from a secret manager or use the platform's workload identity mechanism instead of committing secrets in configuration.

## Application Port

Prometheus reaches the locally running application through:

```text
host.docker.internal:8080
```

Docker Compose also adds the host-gateway mapping for platforms that support it. If FleetOps is run on a different host port, update the local Prometheus target accordingly.

## Tracing

The same observability profile starts Jaeger. To send traces to it, enable:

```text
OTEL_TRACES_EXPORTER_ENABLED=true
OTEL_TRACES_SAMPLING_PROBABILITY=1.0
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://127.0.0.1:4318/v1/traces
```

Prometheus remains the metrics backend; OTLP metrics export is intentionally disabled.

## Stop

```bash
docker compose --profile observability down
```

Named Prometheus and Grafana volumes preserve local data between container restarts. Use `docker compose down -v` only when intentionally clearing local monitoring data.
