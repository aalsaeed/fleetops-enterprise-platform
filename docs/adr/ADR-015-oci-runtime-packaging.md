# ADR-015: OCI Runtime Packaging

- Status: Accepted
- Date: 2026-08-24

## Context

FleetOps can be built and run from Maven or an IDE, while its supporting PostgreSQL, RabbitMQ, Redis, Keycloak, and observability services already have Docker Compose definitions. For deployment and portfolio demonstration, the application itself also needs a portable runtime artifact that does not depend on an IDE or a host Maven installation.

A production runtime image should contain only what is required to execute the packaged Spring Boot application. Source code, Maven caches, test dependencies, and build tooling should remain outside the runtime image. The container must not run as root, and runtime endpoints and credentials must remain environment-driven.

## Decision

Package `fleetops-bootstrap` through the normal Maven lifecycle and build a separate runtime-only OCI image from the executable Spring Boot jar.

The runtime image:

- uses a Java 21 JRE base image;
- runs as the dedicated numeric user/group `10001:10001`;
- contains only the packaged FleetOps jar plus the Java runtime supplied by the base image;
- exposes port `8080` as documentation only and does not hard-code host networking;
- sets container-aware JVM memory defaults through `JAVA_TOOL_OPTIONS`;
- carries OCI title, description, and source metadata;
- receives database, messaging, OAuth2, tracing, and other configuration only from the existing environment-driven Spring configuration.

The Docker build consumes the already verified Maven artifact rather than compiling source inside the image build. CI therefore performs Maven verification first and then proves that the same artifact can be packaged into the runtime image.

## Consequences

- FleetOps has a portable OCI deployment artifact independent of IntelliJ or host Maven at runtime.
- Runtime containers execute as non-root by default.
- Build tooling and source code are not shipped in the application image.
- Maven verification remains the authoritative compile/test/package step before image creation.
- A later deployment increment can attach this image to a Docker Compose application profile and wire service discovery/readiness without changing domain or application code.
- Base-image patch updates remain an infrastructure maintenance concern and can be advanced independently while retaining Java 21 compatibility.
