package com.aalsaeed.fleetops.integration.infrastructure.persistence.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataIntegrationOutboxRepository extends JpaRepository<IntegrationOutboxJpaEntity, UUID> {
}
