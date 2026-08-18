package com.aalsaeed.fleetops.audit.infrastructure.persistence.jpa;

import com.aalsaeed.fleetops.audit.application.port.in.AuditSearchPage;
import com.aalsaeed.fleetops.audit.application.port.in.AuditSearchQuery;
import com.aalsaeed.fleetops.audit.application.port.out.AuditEventQueryStore;
import com.aalsaeed.fleetops.audit.domain.AuditEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class JpaAuditEventQueryStore implements AuditEventQueryStore {

    private final EntityManager entityManager;

    public JpaAuditEventQueryStore(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager cannot be null");
    }

    @Override
    @Transactional(readOnly = true)
    public AuditSearchPage search(AuditSearchQuery query) {
        Objects.requireNonNull(query, "Audit search query cannot be null");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        long total = count(criteriaBuilder, query);
        if (total == 0) {
            return new AuditSearchPage(List.of(), 0, query.offset(), query.limit());
        }

        CriteriaQuery<AuditJpaEntity> criteria = criteriaBuilder.createQuery(AuditJpaEntity.class);
        Root<AuditJpaEntity> root = criteria.from(AuditJpaEntity.class);
        List<Predicate> predicates = predicates(criteriaBuilder, root, query);

        criteria.select(root)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(
                        criteriaBuilder.desc(root.<Instant>get("occurredAt")),
                        criteriaBuilder.desc(root.<UUID>get("id")));

        List<AuditEvent> items = entityManager.createQuery(criteria)
                .setFirstResult(query.offset())
                .setMaxResults(query.limit())
                .getResultList()
                .stream()
                .map(AuditJpaEntity::toDomain)
                .toList();

        return new AuditSearchPage(items, total, query.offset(), query.limit());
    }

    private long count(CriteriaBuilder criteriaBuilder, AuditSearchQuery query) {
        CriteriaQuery<Long> criteria = criteriaBuilder.createQuery(Long.class);
        Root<AuditJpaEntity> root = criteria.from(AuditJpaEntity.class);
        List<Predicate> predicates = predicates(criteriaBuilder, root, query);

        criteria.select(criteriaBuilder.count(root))
                .where(predicates.toArray(Predicate[]::new));

        return entityManager.createQuery(criteria).getSingleResult();
    }

    private static List<Predicate> predicates(
            CriteriaBuilder criteriaBuilder,
            Root<AuditJpaEntity> root,
            AuditSearchQuery query) {

        List<Predicate> predicates = new ArrayList<>();

        if (query.from() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.<Instant>get("occurredAt"),
                    query.from()));
        }
        if (query.to() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.<Instant>get("occurredAt"),
                    query.to()));
        }
        if (query.actorSubject() != null) {
            predicates.add(criteriaBuilder.equal(root.get("actorSubject"), query.actorSubject()));
        }
        if (query.action() != null) {
            predicates.add(criteriaBuilder.equal(root.get("action"), query.action()));
        }
        if (query.resourceType() != null) {
            predicates.add(criteriaBuilder.equal(root.get("resourceType"), query.resourceType()));
        }
        if (query.resourceId() != null) {
            predicates.add(criteriaBuilder.equal(root.get("resourceId"), query.resourceId()));
        }
        if (query.outcome() != null) {
            predicates.add(criteriaBuilder.equal(root.get("outcome"), query.outcome()));
        }
        if (query.correlationId() != null) {
            predicates.add(criteriaBuilder.equal(root.get("correlationId"), query.correlationId()));
        }

        return predicates;
    }
}
