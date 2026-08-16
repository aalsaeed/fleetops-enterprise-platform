package com.aalsaeed.fleetops.trip.infrastructure.persistence.jpa;

import com.aalsaeed.fleetops.trip.application.port.out.TripRepository;
import com.aalsaeed.fleetops.trip.domain.Trip;
import com.aalsaeed.fleetops.trip.domain.TripId;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
public class JpaTripRepositoryAdapter implements TripRepository {

    private final SpringDataTripRepository repository;

    public JpaTripRepositoryAdapter(SpringDataTripRepository repository) {
        this.repository = repository;
    }

    @Override
    public Trip save(Trip trip) {
        Objects.requireNonNull(trip, "Trip cannot be null");
        return repository.save(TripJpaEntity.fromDomain(trip)).toDomain();
    }

    @Override
    public Optional<Trip> findById(TripId id) {
        Objects.requireNonNull(id, "Trip ID cannot be null");
        return repository.findById(id.value()).map(TripJpaEntity::toDomain);
    }

    @Override
    public Optional<Trip> findByExternalReference(String externalReference) {
        Objects.requireNonNull(externalReference, "External reference cannot be null");
        return repository.findByExternalReference(externalReference.trim())
                .map(TripJpaEntity::toDomain);
    }

    @Override
    public boolean existsByExternalReference(String externalReference) {
        Objects.requireNonNull(externalReference, "External reference cannot be null");
        return repository.existsByExternalReference(externalReference.trim());
    }
}
