package com.aalsaeed.fleetops.vehicle.infrastructure.persistence.jpa;

import com.aalsaeed.fleetops.common.concurrency.OptimisticConcurrencyConflictException;
import com.aalsaeed.fleetops.vehicle.application.port.out.VehicleRepository;
import com.aalsaeed.fleetops.vehicle.domain.Vehicle;
import com.aalsaeed.fleetops.vehicle.domain.VehicleId;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
public class JpaVehicleRepositoryAdapter implements VehicleRepository {

    private final SpringDataVehicleRepository repository;

    public JpaVehicleRepositoryAdapter(SpringDataVehicleRepository repository) {
        this.repository = repository;
    }

    @Override
    public Vehicle save(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Vehicle cannot be null");
        try {
            return repository.save(VehicleJpaEntity.fromDomain(vehicle)).toDomain();
        } catch (OptimisticLockingFailureException exception) {
            throw new OptimisticConcurrencyConflictException(
                    "Vehicle",
                    vehicle.id().value().toString(),
                    exception);
        }
    }

    @Override
    public Optional<Vehicle> findById(VehicleId id) {
        Objects.requireNonNull(id, "Vehicle ID cannot be null");
        return repository.findById(id.value()).map(VehicleJpaEntity::toDomain);
    }

    @Override
    public Optional<Vehicle> findByExternalReference(String externalReference) {
        Objects.requireNonNull(externalReference, "External reference cannot be null");
        return repository.findByExternalReference(externalReference.trim())
                .map(VehicleJpaEntity::toDomain);
    }

    @Override
    public boolean existsByExternalReference(String externalReference) {
        Objects.requireNonNull(externalReference, "External reference cannot be null");
        return repository.existsByExternalReference(externalReference.trim());
    }
}
