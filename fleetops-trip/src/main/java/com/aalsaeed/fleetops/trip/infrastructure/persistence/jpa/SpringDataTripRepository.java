package com.aalsaeed.fleetops.trip.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataTripRepository extends JpaRepository<TripJpaEntity, UUID> {

    Optional<TripJpaEntity> findByExternalReference(String externalReference);

    boolean existsByExternalReference(String externalReference);
}
