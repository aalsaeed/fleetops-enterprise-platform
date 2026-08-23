ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:21-jre-jammy
FROM ${JAVA_RUNTIME_IMAGE}

LABEL org.opencontainers.image.title="FleetOps Enterprise Platform" \
      org.opencontainers.image.description="Enterprise fleet, logistics, and ERP integration reference platform" \
      org.opencontainers.image.source="https://github.com/aalsaeed/fleetops-enterprise-platform"

RUN groupadd --system --gid 10001 fleetops \
    && useradd --system --uid 10001 --gid fleetops --home-dir /app --create-home --shell /usr/sbin/nologin fleetops

WORKDIR /app

COPY --chown=10001:10001 fleetops-bootstrap/target/fleetops-bootstrap-*.jar /app/fleetops.jar

USER 10001:10001

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0 -XX:+ExitOnOutOfMemoryError -Djava.io.tmpdir=/tmp"

ENTRYPOINT ["java", "-jar", "/app/fleetops.jar"]
