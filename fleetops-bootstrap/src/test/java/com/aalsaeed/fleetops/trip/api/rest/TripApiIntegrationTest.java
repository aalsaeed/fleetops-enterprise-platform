package com.aalsaeed.fleetops.trip.api.rest;

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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TripApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_trip_api_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void completeTripWorkflowUsesRealDriverAndVehicleResources() throws Exception {
        HttpResponse<String> driver = send("POST", "/api/v1/drivers", """
                {
                  "externalReference":"DRV-TRIP-E2E-1001",
                  "firstName":"Ahmed",
                  "lastName":"Saleh",
                  "phoneNumber":"+966500009001"
                }
                """);
        assertEquals(201, driver.statusCode());
        UUID driverId = resourceId(driver);

        HttpResponse<String> tractor = send("POST", "/api/v1/vehicles", """
                {
                  "externalReference":"VEH-TRIP-E2E-TRACTOR-1001",
                  "description":"Trip E2E tractor",
                  "type":"TRACTOR",
                  "serialNumber":"TRIP-TR-1001"
                }
                """);
        assertEquals(201, tractor.statusCode());
        UUID tractorId = resourceId(tractor);

        HttpResponse<String> bulker = send("POST", "/api/v1/vehicles", """
                {
                  "externalReference":"VEH-TRIP-E2E-BULKER-1001",
                  "description":"Trip E2E bulker",
                  "type":"BULKER",
                  "serialNumber":"TRIP-BU-1001"
                }
                """);
        assertEquals(201, bulker.statusCode());
        UUID bulkerId = resourceId(bulker);

        HttpResponse<String> createdTrip = send("POST", "/api/v1/trips", """
                {"externalReference":"TRIP-E2E-1001"}
                """);
        assertEquals(201, createdTrip.statusCode());
        assertTrue(createdTrip.body().contains("\"status\":\"PLANNED\""));
        String tripPath = URI.create(createdTrip.headers().firstValue("Location").orElseThrow()).getPath();

        HttpResponse<String> assigned = send("PUT", tripPath + "/assignment", """
                {
                  "driverId":"%s",
                  "primaryVehicleId":"%s",
                  "attachmentVehicleId":"%s"
                }
                """.formatted(driverId, tractorId, bulkerId));
        assertEquals(200, assigned.statusCode());
        assertTrue(assigned.body().contains("\"status\":\"ASSIGNED\""));
        assertTrue(assigned.body().contains("\"driverId\":\"" + driverId + "\""));
        assertTrue(assigned.body().contains("\"primaryVehicleId\":\"" + tractorId + "\""));
        assertTrue(assigned.body().contains("\"attachmentVehicleId\":\"" + bulkerId + "\""));

        HttpResponse<String> started = send("POST", tripPath + "/start", null);
        assertEquals(200, started.statusCode());
        assertTrue(started.body().contains("\"status\":\"IN_PROGRESS\""));

        HttpResponse<String> completed = send("POST", tripPath + "/complete", null);
        assertEquals(200, completed.statusCode());
        assertTrue(completed.body().contains("\"status\":\"COMPLETED\""));

        HttpResponse<String> fetched = send("GET", "/api/v1/trips?externalReference=TRIP-E2E-1001", null);
        assertEquals(200, fetched.statusCode());
        assertTrue(fetched.body().contains("\"status\":\"COMPLETED\""));
    }

    @Test
    void startRevalidatesDriverOperationalState() throws Exception {
        HttpResponse<String> driver = send("POST", "/api/v1/drivers", """
                {
                  "externalReference":"DRV-TRIP-E2E-1002",
                  "firstName":"Omar",
                  "lastName":"Khaled",
                  "phoneNumber":"+966500009002"
                }
                """);
        assertEquals(201, driver.statusCode());
        UUID driverId = resourceId(driver);
        String driverPath = URI.create(driver.headers().firstValue("Location").orElseThrow()).getPath();

        HttpResponse<String> tractor = send("POST", "/api/v1/vehicles", """
                {
                  "externalReference":"VEH-TRIP-E2E-TRACTOR-1002",
                  "description":"Trip E2E revalidation tractor",
                  "type":"TRACTOR"
                }
                """);
        assertEquals(201, tractor.statusCode());
        UUID tractorId = resourceId(tractor);

        HttpResponse<String> createdTrip = send("POST", "/api/v1/trips", """
                {"externalReference":"TRIP-E2E-1002"}
                """);
        assertEquals(201, createdTrip.statusCode());
        String tripPath = URI.create(createdTrip.headers().firstValue("Location").orElseThrow()).getPath();

        HttpResponse<String> assigned = send("PUT", tripPath + "/assignment", """
                {
                  "driverId":"%s",
                  "primaryVehicleId":"%s"
                }
                """.formatted(driverId, tractorId));
        assertEquals(200, assigned.statusCode());

        HttpResponse<String> suspended = send(
                "PATCH",
                driverPath + "/status",
                "{\"status\":\"SUSPENDED\"}");
        assertEquals(200, suspended.statusCode());

        HttpResponse<String> start = send("POST", tripPath + "/start", null);
        assertEquals(409, start.statusCode());
        assertTrue(start.body().contains("TRIP_RESOURCE_UNAVAILABLE"));
    }

    private UUID resourceId(HttpResponse<String> response) {
        String path = URI.create(response.headers().firstValue("Location").orElseThrow()).getPath();
        return UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
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
