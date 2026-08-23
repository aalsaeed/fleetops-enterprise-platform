# ADR-013: Optimistic Concurrency Control for Mutable Aggregates

- Status: Accepted
- Date: 2026-08-23

## Context

Driver, Vehicle, and Trip are mutable aggregates. Their application services use a load-modify-save workflow, and persistence adapters reconstruct detached JPA entities from framework-free domain objects before saving them.

Without an explicit concurrency token, two callers can load the same aggregate state, make different valid changes, and save in sequence. The later save can silently overwrite the earlier committed change. This lost-update behavior is unsafe for fleet operations and is difficult to diagnose after the fact.

Broad pessimistic locking would serialize normal updates and hold database locks across business processing. That is unnecessary for the expected contention profile and would couple ordinary aggregate updates more tightly to database transaction timing.

## Decision

Use optimistic concurrency control for Driver, Vehicle, and Trip.

Each aggregate persistence table has a `revision BIGINT NOT NULL` column. The corresponding JPA entity maps that column with Jakarta Persistence `@Version`.

The technical revision is carried through the JPA-to-domain and domain-to-JPA mapping so a detached aggregate retains the version that was originally loaded. Domain classes expose the revision as a nullable `Long` concurrency token but remain independent of JPA/Hibernate APIs and exceptions.

New aggregates use a `null` revision until their first persistence operation. This is intentional because Spring Data JPA treats an entity with a nullable `@Version` property set to `null` as new even when the aggregate already has an application-assigned UUID. After insertion, the persistence provider initializes the revision. Subsequent writes carry the loaded revision and are rejected when that revision is stale.

The initial increment keeps persistence-provider optimistic-lock exceptions at the persistence boundary. A following increment will translate stale-write failures into stable application/API conflict semantics rather than exposing provider-specific behavior over HTTP.

## Consequences

- Concurrent stale writes are rejected instead of silently overwriting newer state.
- Driver, Vehicle, and Trip share one consistent concurrency model.
- UUID identity remains application-assigned.
- Domain behavior stays framework-independent; only the revision value crosses the persistence boundary.
- Every successful aggregate update advances the revision.
- Callers that retry after a conflict must reload the aggregate and re-evaluate the requested business change against the latest state.
- Normal reads and writes do not require pessimistic database locks.
- API-level conditional requests such as ETag/If-Match can be layered on top of the same revision token later without changing the database concurrency mechanism.
