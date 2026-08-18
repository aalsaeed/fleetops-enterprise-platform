# ADR-006: Immutable Enterprise Audit Trail

## Status

Accepted

## Context

FleetOps now protects its HTTP boundary with OAuth2/JWT and role-based authorization. Security-sensitive recovery operations and normal business writes require a durable record of who performed an action, what resource was affected, the outcome, and the request correlation identity.

Audit data must not leak identity-provider credentials, bearer tokens, ERP payloads, or other secrets. Domain aggregates should also remain unaware of HTTP and JWT infrastructure.

## Decision

Introduce a dedicated `fleetops-audit` module with a canonical, framework-free audit event and an application port for recording events.

Audit persistence is append-only:

- the application exposes an `append` store operation only;
- PostgreSQL stores the event, authority snapshot, and safe metadata separately;
- database triggers reject `UPDATE` and `DELETE` mutations on audit tables;
- audit identity is a UUID and timestamps are recorded in UTC;
- actor authorities are captured as a snapshot rather than resolved later;
- correlation ID is mandatory for traceability.

The first increment establishes the model and immutable persistence. HTTP/JWT capture adapters and the administrator query API are separate follow-up increments.

## Consequences

- Business/domain modules remain independent of Spring Security and JWT types.
- Audit history is resistant to accidental application mutation.
- Operational retention must use an explicit future archival/partitioning strategy rather than ordinary row deletion.
- Audit capture failures require deliberate transaction semantics in the next increment instead of being silently ignored.
