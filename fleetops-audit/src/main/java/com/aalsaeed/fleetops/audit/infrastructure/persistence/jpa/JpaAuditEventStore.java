package com.aalsaeed.fleetops.audit.infrastructure.persistence.jpa;

import com.aalsaeed.fleetops.audit.application.port.out.AuditEventStore;
import com.aalsaeed.fleetops.audit.domain.AuditEvent;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Repository
public class JpaAuditEventStore implements AuditEventStore {

    private final EntityManager entityManager;

    public JpaAuditEventStore(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager cannot be null");
    }

    @Override
    @Transactional
    public void append(AuditEvent event) {
        entityManager.persist(new AuditJpaEntity(Objects.requireNonNull(event, "Audit event cannot be null")));
    }
}
