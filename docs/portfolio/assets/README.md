# Portfolio Evidence Asset Guide

Use this directory for a small, deliberate set of runtime images that prove specific FleetOps engineering capabilities.

## Preferred Files

Use these names so documentation links remain stable:

| File | What it should prove |
| --- | --- |
| `01-compose-stack.png` | FleetOps application plus required infrastructure running and healthy |
| `02-grafana-operations.png` | Operational metrics visible in the provisioned FleetOps dashboard |
| `03-jaeger-trace.png` | Trace evidence for an HTTP or message-driven flow |
| `04-keycloak-security.png` | FleetOps realm, clients, or roles configured without exposing secrets |
| `05-rabbitmq-integration.png` | Queue/exchange/topology or message-state evidence relevant to ERP integration |
| `06-github-actions-ci.png` | Successful CI with verification, OCI image, non-root runtime, Compose validation, and readiness smoke test |

## Capture Rules

Each image should demonstrate one capability clearly. Prefer a tight crop that keeps the relevant product/application identity visible while removing unrelated desktop clutter.

Before committing an image:

- remove or hide access tokens, passwords, client secrets, session values, cookies, API keys, private hostnames, internal IP addresses, and personal information;
- use demonstration data rather than production records;
- avoid exposing terminal history that may contain credentials;
- make sure the image is readable at normal GitHub width;
- do not use generated illustrations as proof of runtime behavior;
- do not include multiple screenshots that prove the same point.

## Recommended Resolution

A practical target is 1600–1920 pixels wide. PNG is preferred for dashboards and terminal/UI evidence because it keeps text sharp.

## README Usage

Only the strongest two or three images should eventually appear directly in the root README. The complete six-image evidence set can live in the Case Study or Demo Runbook.

The preferred README evidence is:

1. architecture diagram (already rendered by Mermaid);
2. Grafana operations dashboard;
3. Jaeger distributed trace or full Compose stack.

This keeps the landing page technical and fast to scan.
