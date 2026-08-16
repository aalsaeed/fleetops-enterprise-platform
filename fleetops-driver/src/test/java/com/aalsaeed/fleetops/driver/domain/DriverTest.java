package com.aalsaeed.fleetops.driver.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DriverTest {

    @Test
    void newDriverStartsActive() {
        Driver driver = Driver.create("DRV-1001", "Ahmed", "Saleh", "+966500000001");

        assertNotNull(driver.id());
        assertEquals(DriverStatus.ACTIVE, driver.status());
    }

    @Test
    void textualValuesAreTrimmed() {
        Driver driver = Driver.create("  DRV-1001  ", "  Ahmed ", " Saleh  ", "+966500000001");

        assertEquals("DRV-1001", driver.externalReference());
        assertEquals("Ahmed", driver.firstName());
        assertEquals("Saleh", driver.lastName());
    }

    @Test
    void blankRequiredValuesAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Driver.create(" ", "Ahmed", "Saleh", "+966500000001"));
    }

    @Test
    void invalidPhoneNumberIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Driver.create("DRV-1001", "Ahmed", "Saleh", "0500000001"));
    }

    @Test
    void activeDriverCanBeSuspended() {
        Driver driver = Driver.create("DRV-1001", "Ahmed", "Saleh", "+966500000001");

        driver.changeStatus(DriverStatus.SUSPENDED);

        assertEquals(DriverStatus.SUSPENDED, driver.status());
    }

    @Test
    void invalidStatusTransitionIsRejected() {
        Driver driver = Driver.restore(
                DriverId.newId(),
                "DRV-1001",
                "Ahmed",
                "Saleh",
                "+966500000001",
                DriverStatus.INACTIVE);

        assertThrows(
                InvalidDriverStatusTransitionException.class,
                () -> driver.changeStatus(DriverStatus.SUSPENDED));
    }
}
