package com.aalsaeed.fleetops.driver.infrastructure.persistence.jpa;

import com.aalsaeed.fleetops.driver.domain.Driver;
import com.aalsaeed.fleetops.driver.domain.DriverId;
import com.aalsaeed.fleetops.driver.domain.DriverStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "drivers")
class DriverJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "external_reference", nullable = false, unique = true, length = 100)
    private String externalReference;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone_number", nullable = false, length = 16)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DriverStatus status;

    protected DriverJpaEntity() {
    }

    private DriverJpaEntity(
            UUID id,
            String externalReference,
            String firstName,
            String lastName,
            String phoneNumber,
            DriverStatus status) {
        this.id = id;
        this.externalReference = externalReference;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.status = status;
    }

    static DriverJpaEntity fromDomain(Driver driver) {
        return new DriverJpaEntity(
                driver.id().value(),
                driver.externalReference(),
                driver.firstName(),
                driver.lastName(),
                driver.phoneNumber(),
                driver.status());
    }

    Driver toDomain() {
        return Driver.restore(
                new DriverId(id),
                externalReference,
                firstName,
                lastName,
                phoneNumber,
                status);
    }
}
