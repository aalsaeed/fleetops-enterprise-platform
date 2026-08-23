# ADR-014: Provider-Neutral Concurrency Conflict Semantics

- Status: Accepted
- Date: 2026-08-23

## Context

ADR-013 introduced JPA `@Version` optimistic locking for Driver, Vehicle, and Trip. Persistence providers and Spring Data surface stale writes through infrastructure-specific exception types. Returning those exceptions directly through the HTTP layer would leak persistence details and make the API contract depend on Hibernate/JPA behavior.

FleetOps already uses RFC 9457-style `ProblemDetail` responses for stable business and security failures. Concurrency conflicts should follow the same boundary.

## Decision

Persistence adapters translate Spring's `OptimisticLockingFailureException` into the framework-independent `OptimisticConcurrencyConflictException` from `fleetops-common`.

The exception carries only stable resource metadata:

- resource type
- resource identifier
- a safe retry message

REST exception handlers map that application-facing conflict to HTTP `409 Conflict` with code `OPTIMISTIC_CONCURRENCY_CONFLICT`. The response includes `resourceType` and `resourceId` but does not expose provider exception names, SQL, entity snapshots, or stack-trace details.

Driver and Vehicle controller advice are explicitly scoped to their controllers, matching the existing Trip advice pattern and avoiding ambiguous handling of the shared concurrency exception.

## Consequences

- API clients receive one stable conflict contract across Driver, Vehicle, and Trip.
- Domain aggregates remain independent of JPA/Hibernate exception classes.
- Persistence diagnostics remain available as the internal exception cause for logging and tests without crossing the HTTP boundary.
- Existing business conflicts such as duplicate resources or invalid status transitions retain their own error codes.
- A client receiving a concurrency conflict must reload current state and re-evaluate the requested mutation before retrying.
- HTTP conditional requests (`ETag` / `If-Match`) remain an optional client-side precondition layer. They may be added later but do not replace database `@Version` protection.
