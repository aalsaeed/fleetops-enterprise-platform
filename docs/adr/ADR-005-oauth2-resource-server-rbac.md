# ADR-005: OAuth2 Resource Server and HTTP RBAC

## Status

Accepted

## Context

FleetOps exposes operational and business APIs that must be protected without coupling authentication or authorization concerns to domain aggregates or application use cases. The platform also needs to remain identity-provider neutral while supporting a local OpenID Connect provider for development and demos.

## Decision

FleetOps acts as an OAuth2 Resource Server and accepts signed JWT bearer tokens from an external OpenID Connect provider.

HTTP authorization is enforced in the bootstrap/infrastructure layer with three internal authorities:

- `FLEETOPS_USER` — read access to business APIs.
- `FLEETOPS_OPERATOR` — read and normal business write access.
- `FLEETOPS_ADMIN` — administrative access, including terminal integration recovery operations.

JWT role claims are translated through a dedicated adapter. The adapter accepts a vendor-neutral top-level `roles` claim and the common Keycloak `realm_access.roles` structure, but only recognized FleetOps authorities are retained.

Issuer and JWK Set locations are environment-driven. Both are configured so JWT issuer validation remains enabled while application startup does not depend on the identity provider being available.

Security remains stateless. Form login, HTTP Basic authentication, server-side sessions, and CSRF protection for bearer-token APIs are not used.

## Consequences

- Domain and application modules remain unaware of Spring Security and JWT structures.
- Authorization policy is explicit and reviewable at the HTTP boundary.
- Alternative OIDC providers can be introduced without changing business code.
- Local Keycloak configuration can be added independently as deployment infrastructure.
- Recovery endpoints have a stricter administrative boundary than normal business writes.
