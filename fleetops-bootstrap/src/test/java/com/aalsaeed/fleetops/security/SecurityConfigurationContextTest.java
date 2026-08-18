package com.aalsaeed.fleetops.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.web.SecurityFilterChain;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "fleetops.security.test-permit-all=false")
@Testcontainers
class SecurityConfigurationContextTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_security_context_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void productionSecurityChainStartsWithoutIdentityProviderConnection() {
        assertNotNull(securityFilterChain);
    }
}
