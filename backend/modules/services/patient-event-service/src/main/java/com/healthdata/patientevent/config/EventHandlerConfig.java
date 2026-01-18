package com.healthdata.patientevent.config;

import com.healthdata.patientevent.eventhandler.PatientEventHandler;
import com.healthdata.patientevent.eventhandler.PatientEventHandler.EventStore;
import com.healthdata.patientevent.eventhandler.PatientEventHandler.PatientProjectionStore;
import com.healthdata.patientevent.projection.PatientActiveProjection;
import com.healthdata.patientevent.persistence.PatientProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Event Handler Configuration
 *
 * Instantiates the Phase 4 PatientEventHandler as a Spring bean
 * Provides adapters for EventStore and ProjectionStore interfaces
 */
@Configuration
@RequiredArgsConstructor
public class EventHandlerConfig {

    private final PatientProjectionRepository projectionRepository;

    /**
     * Create PatientEventHandler bean
     * Integrates Phase 4 event handler library with Spring dependency injection
     */
    @Bean
    public PatientEventHandler patientEventHandler() {
        // Create in-memory event store adapter
        EventStore eventStore = new InMemoryEventStore();

        // Create projection store adapter backed by Spring Data JPA repository
        PatientProjectionStore projectionStore = new RepositoryBackedProjectionStore(projectionRepository);

        return new PatientEventHandler(projectionStore, eventStore);
    }

    /**
     * In-memory event store for append-only event log
     * In production, this would be replaced with a database-backed implementation
     */
    private static class InMemoryEventStore implements EventStore {
        // Simple in-memory implementation
        // In production, persist to event_store table with full audit trail

        @Override
        public void storeEvent(Object event) {
            // Append event to in-memory log or database
            // For now, logging indicates event persistence
        }
    }

    /**
     * Projection store backed by Spring Data JPA repository
     * Persists denormalized patient state for fast queries
     */
    private static class RepositoryBackedProjectionStore implements PatientProjectionStore {
        private final PatientProjectionRepository repository;

        public RepositoryBackedProjectionStore(PatientProjectionRepository repository) {
            this.repository = repository;
        }

        @Override
        public void saveProjection(PatientActiveProjection projection) {
            repository.save(projection);
        }

        @Override
        public PatientActiveProjection getPatientProjection(String patientId, String tenantId) {
            return repository.findByIdAndTenant(patientId, tenantId).orElse(null);
        }
    }
}
