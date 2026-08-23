package com.aalsaeed.fleetops.driver.infrastructure.persistence.jpa;

import com.aalsaeed.fleetops.common.concurrency.OptimisticConcurrencyConflictException;
import com.aalsaeed.fleetops.driver.application.port.out.DriverRepository;
import com.aalsaeed.fleetops.driver.domain.Driver;
import com.aalsaeed.fleetops.driver.domain.DriverId;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
public class JpaDriverRepositoryAdapter implements DriverRepository {

    private final SpringDataDriverRepository repository;

    public JpaDriverRepositoryAdapter(SpringDataDriverRepository repository) {
        this.repository = repository;
    }

    @Override
    public Driver save(Driver driver) {
        Objects.requireNonNull(driver, "Driver cannot be null");
        try {
            return repository.save(DriverJpaEntity.fromDomain(driver)).toDomain();
        } catch (OptimisticLockingFailureException exception) {
            throw new OptimisticConcurrencyConflictException(
                    "Driver",
                    driver.id().value().toString(),
                    exception);
        }
    }

    @Override
    public Optional<Driver> findById(DriverId id) {
        Objects.requireNonNull(id, "Driver ID cannot be null");
        return repository.findById(id.value()).map(DriverJpaEntity::toDomain);
    }

    @Override
    public Optional<Driver> findByExternalReference(String externalReference) {
        Objects.requireNonNull(externalReference, "External reference cannot be null");
        return repository.findByExternalReference(externalReference.trim())
                .map(DriverJpaEntity::toDomain);
    }

    @Override
    public boolean existsByExternalReference(String externalReference) {
        Objects.requireNonNull(externalReference, "External reference cannot be null");
        return repository.existsByExternalReference(externalReference.trim());
    }
}
