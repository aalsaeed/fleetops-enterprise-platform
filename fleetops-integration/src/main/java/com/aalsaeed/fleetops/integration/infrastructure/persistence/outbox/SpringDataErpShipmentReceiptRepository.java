package com.aalsaeed.fleetops.integration.infrastructure.persistence.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataErpShipmentReceiptRepository extends JpaRepository<ErpShipmentReceiptJpaEntity, UUID> {

    Optional<ErpShipmentReceiptJpaEntity> findByIdempotencyKey(String idempotencyKey);
}
