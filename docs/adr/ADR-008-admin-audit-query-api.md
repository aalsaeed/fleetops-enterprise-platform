# ADR-008: Administrator Audit Query API

## Status

Accepted

## Context

FleetOps now records immutable audit events for authenticated business writes and integration recovery operations. Operations and security teams need a bounded way to investigate those events without direct database access.

The query capability must not weaken the append-only audit model, expose unbounded scans through the HTTP API, or make audit data visible to normal users and operators.

## Decision

Expose a read-only administrator endpoint at:

`GET /api/v1/audit/events`

The endpoint supports exact-match filters for actor subject, action, resource type, resource identifier, outcome, and correlation identifier, together with inclusive `from` / `to` timestamp bounds.

Pagination uses `offset` and `limit`. The default limit is 50 and the hard maximum is 200 records per request. Results are ordered by audit timestamp descending and UUID descending to provide deterministic newest-first retrieval.

The HTTP adapter delegates to a framework-free audit search use case. Persistence filtering remains behind an audit query-store port. Audit domain objects and append-only write semantics are unchanged.

Spring Security applies an explicit rule before the generic `/api/v1/**` read rule: only `FLEETOPS_ADMIN` may access `/api/v1/audit/**`.

The response includes the immutable audit event, authority snapshot, safe metadata, total matching record count, current offset/limit, and whether more results exist.

No endpoint is provided to update or delete audit events.

## Consequences

- Administrators can investigate security and operational activity without database access.
- Query load is bounded at the API boundary.
- Existing PostgreSQL indexes on timestamp, actor, action, resource, and correlation ID support the principal investigation paths.
- Exact-match filters avoid ambiguous search semantics and uncontrolled wildcard scans.
- Database-level immutability triggers remain authoritative for stored audit records.
- Audit query access itself is intentionally read-only and is not recorded as a business-write audit event.
