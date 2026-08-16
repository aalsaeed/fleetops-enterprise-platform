package com.aalsaeed.fleetops.driver.application.service;

import com.aalsaeed.fleetops.driver.application.exception.DriverAlreadyExistsException;
import com.aalsaeed.fleetops.driver.application.exception.DriverNotFoundException;
import com.aalsaeed.fleetops.driver.application.port.in.CreateDriverCommand;
import com.aalsaeed.fleetops.driver.application.port.out.DriverRepository;
import com.aalsaeed.fleetops.driver.domain.Driver;
import com.aalsaeed.fleetops.driver.domain.DriverId;
import com.aalsaeed.fleetops.driver.domain.DriverStatus;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriverApplicationServiceTest {

    private final InMemoryDriverRepository repository = new InMemoryDriverRepository();
    private final DriverApplicationService service = new DriverApplicationService(repository);

    @Test
    void createsAndPersistsDriver() {
        Driver created = service.createDriver(new CreateDriverCommand(
                "  DRV-APP-1001  ",
                " Ahmed ",
                " Saleh ",
                "+966500000101"));

        assertEquals("DRV-APP-1001", created.externalReference());
        assertEquals("Ahmed", created.firstName());
        assertEquals("Saleh", created.lastName());
        assertEquals(DriverStatus.ACTIVE, created.status());
        assertTrue(repository.findById(created.id()).isPresent());
    }

    @Test
    void rejectsDuplicateExternalReferenceBeforeSaving() {
        service.createDriver(new CreateDriverCommand(
                "DRV-APP-DUP",
                "First",
                "Driver",
                "+966500000102"));

        assertThrows(
                DriverAlreadyExistsException.class,
                () -> service.createDriver(new CreateDriverCommand(
                        "  DRV-APP-DUP  ",
                        "Second",
                        "Driver",
                        "+966500000103")));
    }

    @Test
    void getsDriverByNormalizedExternalReference() {
        Driver created = service.createDriver(new CreateDriverCommand(
                "DRV-APP-1002",
                "Mona",
                "Ali",
                "+966500000104"));

        Driver found = service.getByExternalReference("  DRV-APP-1002  ");

        assertEquals(created.id(), found.id());
    }

    @Test
    void throwsWhenDriverDoesNotExist() {
        DriverId missingId = DriverId.newId();

        assertThrows(DriverNotFoundException.class, () -> service.getById(missingId));
    }

    @Test
    void changesDriverStatusThroughDomainRules() {
        Driver created = service.createDriver(new CreateDriverCommand(
                "DRV-APP-1003",
                "Omar",
                "Hassan",
                "+966500000105"));

        Driver updated = service.changeStatus(created.id(), DriverStatus.SUSPENDED);

        assertEquals(DriverStatus.SUSPENDED, updated.status());
        assertEquals(DriverStatus.SUSPENDED, repository.findById(created.id()).orElseThrow().status());
    }

    private static final class InMemoryDriverRepository implements DriverRepository {

        private final Map<DriverId, Driver> driversById = new HashMap<>();
        private final Map<String, DriverId> idsByExternalReference = new HashMap<>();

        @Override
        public Driver save(Driver driver) {
            driversById.put(driver.id(), driver);
            idsByExternalReference.put(driver.externalReference(), driver.id());
            return driver;
        }

        @Override
        public Optional<Driver> findById(DriverId id) {
            return Optional.ofNullable(driversById.get(id));
        }

        @Override
        public Optional<Driver> findByExternalReference(String externalReference) {
            DriverId id = idsByExternalReference.get(externalReference.trim());
            return id == null ? Optional.empty() : findById(id);
        }

        @Override
        public boolean existsByExternalReference(String externalReference) {
            return idsByExternalReference.containsKey(externalReference.trim());
        }
    }
}
