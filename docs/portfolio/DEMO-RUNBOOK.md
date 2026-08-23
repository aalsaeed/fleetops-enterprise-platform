# FleetOps Portfolio Demo Runbook

This runbook defines a short, repeatable demonstration of the engineering capabilities implemented in FleetOps.

The goal is to show evidence, not to click through every endpoint.

## 1. Start the complete stack

From the repository root:

```bash
cp .env.example .env
docker compose --profile application --profile observability up --build -d
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
docker compose --profile application --profile observability up --build -d
```

Confirm the services:

```bash
docker compose --profile application --profile observability ps
```

Expected demonstration components:

- FleetOps API
- PostgreSQL
- RabbitMQ
- Redis
- Keycloak
- Prometheus
- Grafana
- Jaeger

## 2. Prove application readiness

```bash
curl http://localhost:8080/actuator/health/readiness
```

Expected result: readiness reports `UP`.

This is the same operational signal used by the CI container smoke test.

## 3. Prove the security boundary

### Unauthenticated request

Call a protected API without a bearer token:

```bash
curl -i http://localhost:8080/api/v1/integration/operations
```

Expected result: HTTP `401` with an `application/problem+json` response and `AUTHENTICATION_REQUIRED` code.

### Request an operator token

```bash
curl -s -X POST "http://localhost:8180/realms/fleetops/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=fleetops-operator-client" \
  -d "client_secret=fleetops-local-operator-secret"
```

PowerShell:

```powershell
$operatorTokenResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8180/realms/fleetops/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grant_type   = "client_credentials"
    client_id    = "fleetops-operator-client"
    client_secret = "fleetops-local-operator-secret"
  }

$operatorToken = $operatorTokenResponse.access_token
```

### Read integration operations as operator

Bash, after storing the access token in `TOKEN`:

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/integration/operations
```

PowerShell:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/v1/integration/operations" `
  -Headers @{ Authorization = "Bearer $operatorToken" }
```

Expected result: the operational snapshot is accessible because GET operations allow `FLEETOPS_OPERATOR` or `FLEETOPS_ADMIN`.

## 4. Prove differentiated RBAC

Integration recovery is deliberately stricter than operational read access.

An operator can read the operations snapshot but cannot execute:

```text
POST /api/v1/integration/operations/outbox/{messageId}/requeue
POST /api/v1/integration/operations/inbox/{messageId}/requeue
```

Those routes require `FLEETOPS_ADMIN`.

Request an administrator token using the local admin client:

```text
client_id=fleetops-admin-client
client_secret=fleetops-local-admin-secret
```

The recovery endpoint should only be called with a real failed message ID from the operational snapshot. Do not manufacture a production-looking recovery result for portfolio screenshots.

## 5. Prove protected observability

### Prometheus target

Open:

```text
http://localhost:9090/targets
```

The FleetOps target should be `UP`.

Prometheus authenticates to the protected `/actuator/prometheus` endpoint using the local Keycloak operator client.

### Grafana

Open:

```text
http://localhost:3000
```

Local credentials:

```text
username: admin
password: fleetops_local_admin
```

Open:

```text
FleetOps -> FleetOps Operations
```

Useful dashboard evidence includes:

- integration snapshot availability;
- ERP shipment DLQ depth;
- outbox state counts;
- inbox state counts;
- HTTP request rate;
- HTTP 5xx rate;
- JVM heap usage;
- JVM live threads.

The strongest screenshot is one that shows several of these panels together while the FleetOps Prometheus target is healthy.

## 6. Prove distributed tracing

Trace export must be enabled for the application runtime:

```text
OTEL_TRACES_EXPORTER_ENABLED=true
OTEL_TRACES_SAMPLING_PROBABILITY=1.0
```

For the containerized application, the configured Compose endpoint sends OTLP traces to Jaeger.

Open:

```text
http://localhost:16686
```

Select service:

```text
fleetops-enterprise-platform
```

Generate traffic by calling authenticated FleetOps APIs, then search for traces.

For the strongest demonstration, use an ERP/integration flow so the trace shows HTTP and RabbitMQ activity rather than only a single REST span.

## 7. Show RabbitMQ topology

Open:

```text
http://localhost:15672
```

Use the local RabbitMQ credentials from `.env.example`.

Show the exchange/queue topology used by the ERP integration and, where available, the dead-letter path.

The purpose of this screen is to support the code-level outbox/inbox story with runtime infrastructure evidence.

## 8. Show Keycloak roles and clients

Open Keycloak:

```text
http://localhost:8180
```

Useful evidence:

- `fleetops` realm;
- `fleetops-user-client`;
- `fleetops-operator-client`;
- `fleetops-admin-client`;
- corresponding realm roles.

Do not expose real secrets in screenshots. The repository values are intentionally local demonstration credentials, but portfolio images should still focus on architecture rather than secret values.

## 9. Show CI as executable architecture evidence

Open the repository Actions page and select a successful `CI` run.

The important steps to show are:

1. Maven `Verify`;
2. `Build OCI image`;
3. `Verify non-root runtime user`;
4. `Validate Compose application profile`;
5. `Smoke test containerized application`.

This is stronger evidence than a generic green badge because it demonstrates that architectural claims are part of the automated delivery pipeline.

## Recommended Screenshot Set

Keep the final portfolio set small. Five or six strong images are enough.

| File name | Capture | What it proves |
| --- | --- | --- |
| `01-compose-stack.png` | `docker compose ... ps` with healthy FleetOps and supporting services | Reproducible runtime topology |
| `02-grafana-operations.png` | FleetOps Operations dashboard | Operational metrics and dashboard provisioning |
| `03-jaeger-trace.png` | FleetOps trace with multiple spans | OpenTelemetry/Jaeger tracing |
| `04-keycloak-rbac.png` | FleetOps realm roles/clients | Centralized identity and role model |
| `05-rabbitmq-topology.png` | Integration queues/exchanges/DLQ | Async messaging architecture |
| `06-github-ci.png` | Successful CI job with container checks | Automated verification and delivery discipline |

Do not add screenshots merely to increase image count. Each image should prove a different engineering capability.

## Suggested Demo Narrative

A concise presentation can follow this order:

1. **Business boundary:** drivers, vehicles, and trips are separate domain modules.
2. **Reliability:** trip/ERP handoff does not depend on a fragile database-plus-message dual write.
3. **Recovery:** failed asynchronous work is observable and deliberately recoverable.
4. **Security:** read, operator, admin, recovery, metrics, and audit access are controlled by server-side RBAC.
5. **Diagnostics:** metrics, traces, logs/correlation, and audit data serve different operational purposes.
6. **Delivery:** the entire topology is containerized and the CI pipeline verifies more than compilation/tests.

That sequence explains the engineering decisions before showing the tools that implement them.