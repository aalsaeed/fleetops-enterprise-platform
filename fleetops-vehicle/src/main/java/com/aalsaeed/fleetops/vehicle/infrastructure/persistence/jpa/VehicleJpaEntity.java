package com.aalsaeed.fleetops.vehicle.infrastructure.persistence.jpa;

import com.aalsaeed.fleetops.vehicle.domain.Vehicle;
import com.aalsaeed.fleetops.vehicle.domain.VehicleId;
import com.aalsaeed.fleetops.vehicle.domain.VehicleStatus;
import com.aalsaeed.fleetops.vehicle.domain.VehicleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "vehicles")
class VehicleJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "external_reference", nullable = false, unique = true, length = 100)
    private String externalReference;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private VehicleType type;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VehicleStatus status;

    protected VehicleJpaEntity() {
    }

    private VehicleJpaEntity(
            UUID id,
            String externalReference,
            String description,
            VehicleType type,
            String serialNumber,
            VehicleStatus status) {
        this.id = id;
        this.externalReference = externalReference;
        this.description = description;
        this.type = type;
        this.serialNumber = serialNumber;
        this.status = status;
    }

    static VehicleJpaEntity fromDomain(Vehicle vehicle) {
        return new VehicleJpaEntity(
                vehicle.id().value(),
                vehicle.externalReference(),
                vehicle.description(),
                vehicle.type(),
                vehicle.serialNumber(),
                vehicle.status());
    }

    Vehicle toDomain() {
        return Vehicle.restore(
                new VehicleId(id),
                externalReference,
                description,
                type,
                serialNumber,
                status);
    }
}
