package com.aalsaeed.fleetops.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "fleetops.security.test-permit-all=false")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class KeycloakSecurityIntegrationTest {

    private static final String USER_CLIENT = "fleetops-user-client";
    private static final String USER_SECRET = "fleetops-local-user-secret";
    private static final String OPERATOR_CLIENT = "fleetops-operator-client";
    private static final String OPERATOR_SECRET = "fleetops-local-operator-secret";
    private static final String ADMIN_CLIENT = "fleetops-admin-client";
    private static final String ADMIN_SECRET = "fleetops-local-admin-secret";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_keycloak_security_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Container
    static final GenericContainer<?> KEYCLOAK = new GenericContainer<>(
            DockerImageName.parse("quay.io/keycloak/keycloak:26.7.0"))
            .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
            .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "fleetops-test-admin")
            .withEnv("FLEETOPS_USER_CLIENT_SECRET", USER_SECRET)
            .withEnv("FLEETOPS_OPERATOR_CLIENT_SECRET", OPERATOR_SECRET)
            .withEnv("FLEETOPS_ADMIN_CLIENT_SECRET", ADMIN_SECRET)
            .withCopyFileToContainer(
                    MountableFile.forHostPath(resolveRealmFile().toString()),
                    "/opt/keycloak/data/import/fleetops-realm.json")
            .withCommand("start-dev", "--import-realm")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/realms/fleetops/.well-known/openid-configuration")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private JsonMapper jsonMapper;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloakBaseUrl() + "/realms/fleetops");
        registry.add(
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> keycloakBaseUrl() + "/realms/fleetops/protocol/openid-connect/certs");
    }

    @Test
    void realKeycloakTokensEnforceFleetOpsRoles() throws Exception {
        String userToken = token(USER_CLIENT, USER_SECRET);
        String operatorToken = token(OPERATOR_CLIENT, OPERATOR_SECRET);
        String adminToken = token(ADMIN_CLIENT, ADMIN_SECRET);

        HttpResponse<String> userRead = api(
                "GET",
                "/api/v1/drivers/" + UUID.randomUUID(),
                userToken,
                null);
        assertEquals(404, userRead.statusCode());

        HttpResponse<String> userWrite = api(
                "POST",
                "/api/v1/drivers",
                userToken,
                driverPayload("DRV-KEYCLOAK-USER-1001"));
        assertEquals(403, userWrite.statusCode());
        assertTrue(userWrite.body().contains("ACCESS_DENIED"));

        HttpResponse<String> operatorWrite = api(
                "POST",
                "/api/v1/drivers",
                operatorToken,
                driverPayload("DRV-KEYCLOAK-OPERATOR-1001"));
        assertEquals(201, operatorWrite.statusCode());
        assertTrue(operatorWrite.body().contains("DRV-KEYCLOAK-OPERATOR-1001"));

        HttpResponse<String> operatorRecovery = api(
                "POST",
                "/api/v1/integration/operations/outbox/" + UUID.randomUUID() + "/requeue",
                operatorToken,
                null);
        assertEquals(403, operatorRecovery.statusCode());

        HttpResponse<String> adminRecovery = api(
                "POST",
                "/api/v1/integration/operations/outbox/" + UUID.randomUUID() + "/requeue",
                adminToken,
                null);
        assertEquals(409, adminRecovery.statusCode());
        assertTrue(adminRecovery.body().contains("INTEGRATION_RECOVERY_NOT_AVAILABLE"));
    }

    private String token(String clientId, String clientSecret) throws Exception {
        String body = form("grant_type", "client_credentials")
                + "&" + form("client_id", clientId)
                + "&" + form("client_secret", clientSecret);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(keycloakBaseUrl()
                        + "/realms/fleetops/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());

        @SuppressWarnings("unchecked")
        Map<String, Object> tokenResponse = jsonMapper.readValue(response.body(), Map.class);
        String accessToken = (String) tokenResponse.get("access_token");
        assertNotNull(accessToken);
        return accessToken;
    }

    private HttpResponse<String> api(
            String method,
            String path,
            String accessToken,
            String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken);

        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }

        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String driverPayload(String externalReference) {
        return """
                {
                  "externalReference":"%s",
                  "firstName":"Keycloak",
                  "lastName":"Integration",
                  "phoneNumber":"+966500008099"
                }
                """.formatted(externalReference);
    }

    private static String form(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8)
                + "="
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String keycloakBaseUrl() {
        return "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080);
    }

    private static Path resolveRealmFile() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int depth = 0; depth < 4 && current != null; depth++) {
            Path candidate = current.resolve("infra/keycloak/fleetops-realm.json");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate infra/keycloak/fleetops-realm.json");
    }
}
