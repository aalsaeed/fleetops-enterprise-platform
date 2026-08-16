# ADR-001: Start with a Modular Monolith

## Status

Accepted

## Context

FleetOps contains several business capabilities, but the first release is owned and operated as one product. Introducing network boundaries between every capability would increase deployment, testing, observability, and consistency complexity before independent scaling is required.

## Decision

Implement the first version as a modular monolith with explicit Maven modules and controlled dependencies.

## Consequences

### Positive

- Lower operational complexity
- Straightforward local development
- Strong transactional consistency where required
- Easier end-to-end testing
- Business boundaries remain visible in code

### Negative

- Modules share one deployment unit
- Independent scaling is not initially available
- Boundary discipline must be enforced in the codebase

## Revisit When

A module requires independent scaling, deployment cadence, availability, ownership, or infrastructure characteristics that justify a network boundary.
