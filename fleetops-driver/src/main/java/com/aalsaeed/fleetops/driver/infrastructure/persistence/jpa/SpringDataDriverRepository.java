package com.aalsaeed.fleetops.driver.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataDriverRepository extends JpaRepository<DriverJpaEntity, UUID> {

    Optional<DriverJpaEntity> findByExternalReference(String externalReference);

    boolean existsByExternalReference(String externalReference);
}
