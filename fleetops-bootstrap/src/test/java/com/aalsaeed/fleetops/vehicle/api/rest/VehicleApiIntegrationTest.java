package com.aalsaeed.fleetops.vehicle.api.rest;

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
class VehicleApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_vehicle_api_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void vehicleLifecycleWorksThroughHttpAndPostgreSql() throws Exception {
        String requestBody = """
                {
                  "externalReference": "VEH-E2E-1001",
                  "description": "Integration tractor",
                  "type": "TRACTOR",
                  "serialNumber": "SN-E2E-1001"
                }
                """;

        HttpResponse<String> created = send("POST", "/api/v1/vehicles", requestBody);
        assertEquals(201, created.statusCode());
        assertTrue(created.body().contains("\"externalReference\":\"VEH-E2E-1001\""));
        assertTrue(created.body().contains("\"type\":\"TRACTOR\""));
        assertTrue(created.body().contains("\"status\":\"ACTIVE\""));

        String location = created.headers().firstValue("Location").orElseThrow();
        String vehiclePath = URI.create(location).getPath();

        HttpResponse<String> fetched = send("GET", vehiclePath, null);
        assertEquals(200, fetched.statusCode());
        assertTrue(fetched.body().contains("\"externalReference\":\"VEH-E2E-1001\""));

        HttpResponse<String> maintenance = send(
                "PATCH",
                vehiclePath + "/status",
                "{\"status\":\"MAINTENANCE\"}");
        assertEquals(200, maintenance.statusCode());
        assertTrue(maintenance.body().contains("\"status\":\"MAINTENANCE\""));

        HttpResponse<String> byExternalReference = send(
                "GET",
                "/api/v1/vehicles?externalReference=VEH-E2E-1001",
                null);
        assertEquals(200, byExternalReference.statusCode());
        assertTrue(byExternalReference.body().contains("\"status\":\"MAINTENANCE\""));

        HttpResponse<String> duplicate = send("POST", "/api/v1/vehicles", requestBody);
        assertEquals(409, duplicate.statusCode());
        assertTrue(duplicate.body().contains("VEHICLE_ALREADY_EXISTS"));
    }

    @Test
    void retiredVehicleCannotReturnToActiveService() throws Exception {
        String requestBody = """
                {
                  "externalReference": "VEH-E2E-RETIRED",
                  "description": "Retirement transition test",
                  "type": "TRAILER"
                }
                """;

        HttpResponse<String> created = send("POST", "/api/v1/vehicles", requestBody);
        assertEquals(201, created.statusCode());
        String vehiclePath = URI.create(created.headers().firstValue("Location").orElseThrow()).getPath();

        HttpResponse<String> retired = send(
                "PATCH",
                vehiclePath + "/status",
                "{\"status\":\"RETIRED\"}");
        assertEquals(200, retired.statusCode());

        HttpResponse<String> reactivated = send(
                "PATCH",
                vehiclePath + "/status",
                "{\"status\":\"ACTIVE\"}");
        assertEquals(409, reactivated.statusCode());
        assertTrue(reactivated.body().contains("INVALID_VEHICLE_STATUS_TRANSITION"));
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
