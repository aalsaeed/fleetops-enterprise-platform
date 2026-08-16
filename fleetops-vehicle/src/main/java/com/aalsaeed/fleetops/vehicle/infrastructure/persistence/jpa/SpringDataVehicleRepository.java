package com.aalsaeed.fleetops.vehicle.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataVehicleRepository extends JpaRepository<VehicleJpaEntity, UUID> {

    Optional<VehicleJpaEntity> findByExternalReference(String externalReference);

    boolean existsByExternalReference(String externalReference);
}
