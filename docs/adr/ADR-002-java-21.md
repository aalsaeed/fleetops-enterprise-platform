# ADR-002: Use Java 21 as the Project Baseline

## Status

Accepted

## Context

The portfolio should demonstrate modern Java while remaining representative of enterprise environments that commonly standardize on long-term-support releases.

## Decision

Use Java 21 as the minimum language and runtime baseline for the initial FleetOps releases.

## Consequences

- Modern Java language and runtime features are available.
- The codebase remains suitable for a broad range of current enterprise Java environments.
- A future upgrade to a newer LTS release can be handled as an explicit engineering change rather than an incidental dependency update.
