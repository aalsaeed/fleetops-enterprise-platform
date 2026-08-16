package com.aalsaeed.fleetops.trip.infrastructure.persistence.jpa;

import com.aalsaeed.fleetops.trip.domain.DriverReference;
import com.aalsaeed.fleetops.trip.domain.Trip;
import com.aalsaeed.fleetops.trip.domain.TripId;
import com.aalsaeed.fleetops.trip.domain.TripStatus;
import com.aalsaeed.fleetops.trip.domain.VehicleReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "trips")
class TripJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "external_reference", nullable = false, unique = true, length = 100)
    private String externalReference;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "primary_vehicle_id")
    private UUID primaryVehicleId;

    @Column(name = "attachment_vehicle_id")
    private UUID attachmentVehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TripStatus status;

    protected TripJpaEntity() {
    }

    private TripJpaEntity(
            UUID id,
            String externalReference,
            UUID driverId,
            UUID primaryVehicleId,
            UUID attachmentVehicleId,
            TripStatus status) {
        this.id = id;
        this.externalReference = externalReference;
        this.driverId = driverId;
        this.primaryVehicleId = primaryVehicleId;
        this.attachmentVehicleId = attachmentVehicleId;
        this.status = status;
    }

    static TripJpaEntity fromDomain(Trip trip) {
        return new TripJpaEntity(
                trip.id().value(),
                trip.externalReference(),
                trip.driverReference() == null ? null : trip.driverReference().value(),
                trip.primaryVehicleReference() == null ? null : trip.primaryVehicleReference().value(),
                trip.attachmentVehicleReference() == null ? null : trip.attachmentVehicleReference().value(),
                trip.status());
    }

    Trip toDomain() {
        return Trip.restore(
                TripId.of(id),
                externalReference,
                driverId == null ? null : DriverReference.of(driverId),
                primaryVehicleId == null ? null : VehicleReference.of(primaryVehicleId),
                attachmentVehicleId == null ? null : VehicleReference.of(attachmentVehicleId),
                status);
    }
}
