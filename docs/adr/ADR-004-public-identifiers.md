# ADR-004: Use Opaque Public Identifiers

## Status

Accepted

## Context

API consumers should not depend on sequential database identifiers or infer record counts and ordering from identifiers.

## Decision

Use UUID-based opaque identifiers for resources exposed through public APIs. Internal persistence details may evolve independently where justified.

## Consequences

- API identifiers do not reveal simple database sequences.
- Records can be created safely across future distributed boundaries.
- Identifier generation does not require a central sequence for public identity.
