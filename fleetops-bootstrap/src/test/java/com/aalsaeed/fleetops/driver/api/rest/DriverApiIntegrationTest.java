package com.aalsaeed.fleetops.driver.api.rest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DriverApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_api_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void driverLifecycleWorksThroughHttpAndPostgreSql() throws Exception {
        String requestBody = """
                {
                  "externalReference": "DRV-E2E-1001",
                  "firstName": "Ahmed",
                  "lastName": "Saleh",
                  "phoneNumber": "+966500001001"
                }
                """;

        HttpResponse<String> created = send("POST", "/api/v1/drivers", requestBody);
        assertEquals(201, created.statusCode());
        assertTrue(created.body().contains("\"externalReference\":\"DRV-E2E-1001\""));
        assertTrue(created.body().contains("\"status\":\"ACTIVE\""));

        String location = created.headers().firstValue("Location").orElseThrow();
        String driverPath = URI.create(location).getPath();

        HttpResponse<String> fetched = send("GET", driverPath, null);
        assertEquals(200, fetched.statusCode());
        assertTrue(fetched.body().contains("\"externalReference\":\"DRV-E2E-1001\""));

        HttpResponse<String> suspended = send(
                "PATCH",
                driverPath + "/status",
                "{\"status\":\"SUSPENDED\"}");
        assertEquals(200, suspended.statusCode());
        assertTrue(suspended.body().contains("\"status\":\"SUSPENDED\""));

        HttpResponse<String> byExternalReference = send(
                "GET",
                "/api/v1/drivers?externalReference=DRV-E2E-1001",
                null);
        assertEquals(200, byExternalReference.statusCode());
        assertTrue(byExternalReference.body().contains("\"status\":\"SUSPENDED\""));

        HttpResponse<String> duplicate = send("POST", "/api/v1/drivers", requestBody);
        assertEquals(409, duplicate.statusCode());
        assertTrue(duplicate.body().contains("DRIVER_ALREADY_EXISTS"));
    }

    @Test
    void invalidDomainInputReturnsBadRequest() throws Exception {
        HttpResponse<String> response = send(
                "POST",
                "/api/v1/drivers",
                """
                        {
                          "externalReference": "DRV-E2E-BAD",
                          "firstName": "Ahmed",
                          "lastName": "Saleh",
                          "phoneNumber": "0500000000"
                        }
                        """);

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("INVALID_REQUEST"));
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Accept", "application/json");

        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
