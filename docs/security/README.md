# FleetOps Security Model

FleetOps HTTP APIs are protected as an OAuth2 Resource Server using JWT bearer tokens.

## Authorities

| Authority | Intended access |
| --- | --- |
| `FLEETOPS_USER` | Read-only access to business APIs |
| `FLEETOPS_OPERATOR` | Business reads and normal write operations |
| `FLEETOPS_ADMIN` | Full administrative access, including integration recovery |

## HTTP Policy

- `GET /api/v1/**` requires `FLEETOPS_USER`, `FLEETOPS_OPERATOR`, or `FLEETOPS_ADMIN`.
- `POST`, `PUT`, and `PATCH /api/v1/**` require `FLEETOPS_OPERATOR` or `FLEETOPS_ADMIN`.
- `DELETE /api/v1/**` requires `FLEETOPS_ADMIN`.
- `GET /api/v1/integration/operations` requires `FLEETOPS_OPERATOR` or `FLEETOPS_ADMIN`.
- `POST /api/v1/integration/operations/**` requires `FLEETOPS_ADMIN`.
- Actuator health and info endpoints remain public.

JWT roles are normalized through a dedicated converter. Unrecognized identity-provider roles are ignored rather than being promoted automatically into application authorities.

## Local Identity Provider

The development stack includes Keycloak with a committed, non-secret demonstration realm. It provisions service-account clients for the three FleetOps authorities and exercises the same JWT validation path used by the application.

See [Local Keycloak Runbook](keycloak-local.md) for startup, token acquisition, and reset instructions.

The local Keycloak configuration is for development and portfolio demonstrations only. Production identity-provider endpoints and credentials remain environment-specific.
