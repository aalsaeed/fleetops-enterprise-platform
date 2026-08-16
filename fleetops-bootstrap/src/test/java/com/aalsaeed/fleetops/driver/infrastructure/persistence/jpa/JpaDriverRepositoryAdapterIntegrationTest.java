package com.aalsaeed.fleetops.driver.infrastructure.persistence.jpa;

import com.aalsaeed.fleetops.driver.application.port.out.DriverRepository;
import com.aalsaeed.fleetops.driver.domain.Driver;
import com.aalsaeed.fleetops.driver.domain.DriverStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class JpaDriverRepositoryAdapterIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Autowired
    private DriverRepository driverRepository;

    @Test
    void savesAndRestoresDriverDomainState() {
        Driver driver = Driver.create(
                "DRV-TC-1001",
                "Ahmed",
                "Saleh",
                "+966500000001");
        driver.changeStatus(DriverStatus.SUSPENDED);

        Driver saved = driverRepository.save(driver);
        Driver restored = driverRepository.findById(saved.id()).orElseThrow();

        assertThat(restored.id()).isEqualTo(saved.id());
        assertThat(restored.externalReference()).isEqualTo("DRV-TC-1001");
        assertThat(restored.firstName()).isEqualTo("Ahmed");
        assertThat(restored.lastName()).isEqualTo("Saleh");
        assertThat(restored.phoneNumber()).isEqualTo("+966500000001");
        assertThat(restored.status()).isEqualTo(DriverStatus.SUSPENDED);
    }

    @Test
    void findsDriverByNormalizedExternalReference() {
        Driver saved = driverRepository.save(Driver.create(
                "  DRV-TC-1002  ",
                "Mona",
                "Ali",
                "+966500000002"));

        assertThat(driverRepository.existsByExternalReference("  DRV-TC-1002  ")).isTrue();

        Driver restored = driverRepository.findByExternalReference(" DRV-TC-1002 ").orElseThrow();
        assertThat(restored.id()).isEqualTo(saved.id());
        assertThat(restored.externalReference()).isEqualTo("DRV-TC-1002");
    }

    @Test
    void rejectsDuplicateExternalReferenceAtDatabaseBoundary() {
        driverRepository.save(Driver.create(
                "DRV-TC-DUP",
                "First",
                "Driver",
                "+966500000003"));

        assertThatThrownBy(() -> driverRepository.save(Driver.create(
                "DRV-TC-DUP",
                "Second",
                "Driver",
                "+966500000004")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
