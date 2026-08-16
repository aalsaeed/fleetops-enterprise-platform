# ADR-003: Use PostgreSQL with Flyway-managed Schema Changes

## Status

Accepted

## Context

FleetOps needs a relational source of truth for transactional fleet, trip, integration, and audit data. Schema evolution must be reproducible across local, CI, and future deployment environments.

## Decision

Use PostgreSQL as the primary relational database and Flyway for version-controlled schema migrations.

## Consequences

- Database changes are reviewed alongside application code.
- Environments can be recreated deterministically.
- JPA schema auto-creation is not used as a production migration strategy.
- PostgreSQL-specific behavior can be tested directly rather than hidden behind a different development database.
