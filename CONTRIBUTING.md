# Contributing

FleetOps is currently maintained as a focused engineering portfolio, but issues and pull requests are welcome when they improve correctness, documentation, testing, or architecture clarity.

## Development Principles

- Keep business rules inside the relevant business module.
- Prefer small, reviewable changes.
- Add or update tests for behavior changes.
- Document significant architecture decisions with an ADR.
- Never commit credentials, tokens, company data, customer data, or proprietary ERP assets.

## Commit Style

The repository uses concise Conventional Commit-style messages, for example:

```text
feat(trip): add trip assignment rules
fix(driver): prevent duplicate external reference
test(trip): cover invalid lifecycle transitions
docs(architecture): document outbox decision
```

## Pull Requests

A useful pull request should explain:

1. What changed
2. Why the change is required
3. Important design decisions
4. How the change was tested
5. Risks or follow-up work
