# ADR-007: Capture Audit Events at the HTTP Boundary

## Status

Accepted

## Context

FleetOps now has an immutable append-only audit store. The next requirement is to capture authenticated business write operations without introducing Spring Security, HTTP, or audit dependencies into Driver, Vehicle, Trip, or Integration application/domain code.

Audit persistence must also not replace an already-determined business HTTP result. If audit storage is temporarily unavailable, the business operation remains authoritative and the audit failure is surfaced operationally through server logs.

## Decision

FleetOps captures auditable write outcomes in the bootstrap HTTP adapter layer using a Spring MVC `HandlerInterceptor`.

A route registry maps explicit write endpoints to stable audit actions and resource types. Resource identifiers are resolved from URI template variables, or from the `Location` response header for create operations.

A high-precedence correlation filter accepts a constrained `X-Correlation-ID` value or generates a UUID when the supplied value is absent or unsafe. The same correlation identifier is returned to the caller and stored with the audit event.

The audit recorder snapshots the authenticated JWT subject, an optional display identity, and the current granted authorities. It never stores bearer tokens, client secrets, request bodies, passwords, or raw ERP payloads.

Audit outcome is derived from the final HTTP status: 2xx is `SUCCESS`; all other handled write outcomes are `FAILURE`.

## Transaction Semantics

The controller/business operation completes before the MVC interceptor appends the audit event. The append therefore uses its own persistence transaction through the audit store.

If audit persistence fails, the exception is logged and suppressed by the audit HTTP adapter. It does not convert a successful business response into an error, nor does it mask an original business failure. This trade-off makes audit-storage degradation visible while preserving the externally observed business result.

## Audited Operations

- Driver create and status change
- Vehicle create and status change
- Trip create, assignment, start, complete, and cancel
- Integration Outbox and Inbox recovery/requeue operations

Read-only endpoints are not recorded by this capture layer.

## Consequences

Business/domain modules remain independent of security and audit infrastructure. Audit action names are centrally reviewable and the correlation ID provides an operational join key across HTTP responses, logs, and durable audit records.

Authentication failures and authorization denials that are rejected before Spring MVC are outside this interceptor and may be covered later by a dedicated security-event audit adapter.
