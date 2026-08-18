# Local Keycloak Runbook

FleetOps includes a local Keycloak 26.7.0 container for development and portfolio demonstrations. It imports the `fleetops` realm from `infra/keycloak/fleetops-realm.json` on first startup.

The committed client credentials are deliberately local demonstration defaults only. They are not production secrets and must never be reused outside a local development environment.

## Start the local identity provider

```bash
docker compose up -d keycloak
```

Keycloak is exposed at `http://127.0.0.1:8180` by default. The FleetOps application is already configured to validate tokens issued by:

```text
http://127.0.0.1:8180/realms/fleetops
```

## Local machine clients

| Client | Realm role | Default local secret |
| --- | --- | --- |
| `fleetops-user-client` | `FLEETOPS_USER` | `fleetops-local-user-secret` |
| `fleetops-operator-client` | `FLEETOPS_OPERATOR` | `fleetops-local-operator-secret` |
| `fleetops-admin-client` | `FLEETOPS_ADMIN` | `fleetops-local-admin-secret` |

Each client uses the OAuth2 `client_credentials` grant and a Keycloak service account. Direct Access Grants and browser login flows are disabled for these local machine clients.

## Request an operator token

```bash
curl -s -X POST "http://127.0.0.1:8180/realms/fleetops/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=fleetops-operator-client" \
  -d "client_secret=fleetops-local-operator-secret"
```

The returned access token contains the `FLEETOPS_OPERATOR` realm role. FleetOps maps supported realm roles to internal Spring Security authorities through `FleetOpsJwtAuthoritiesConverter`.

## Windows PowerShell example

```powershell
$tokenResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "http://127.0.0.1:8180/realms/fleetops/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grant_type    = "client_credentials"
    client_id     = "fleetops-operator-client"
    client_secret = "fleetops-local-operator-secret"
  }

$token = $tokenResponse.access_token
Invoke-RestMethod `
  -Method Get `
  -Uri "http://127.0.0.1:8080/api/v1/drivers?externalReference=DRV-DEMO" `
  -Headers @{ Authorization = "Bearer $token" }
```

## Reset the imported realm

Keycloak startup import skips a realm that already exists. For a clean local realm during development:

```bash
docker compose rm -sf keycloak
docker compose up -d keycloak
```

The current local Keycloak service does not persist a separate Keycloak database volume, so recreating the container re-imports the committed realm definition.

## Production note

The local container is a development/demo identity provider. Production deployment should use a managed or production-configured OpenID Connect provider, externalized secrets, TLS, durable Keycloak storage if Keycloak is operated directly, and environment-specific issuer/JWK configuration.
