package com.healthdata.qualitymeasure.config;

import com.healthdata.qualitymeasure.eventhandler.QualityMeasureEventHandler;
import com.healthdata.qualitymeasure.eventhandler.QualityMeasureEventHandler.EventStore;
import com.healthdata.qualitymeasure.eventhandler.QualityMeasureEventHandler.MeasureProjectionStore;
import com.healthdata.qualitymeasure.projection.MeasureEvaluationProjection;
import com.healthdata.qualitymeasure.projection.CohortMeasureRateProjection;
import com.healthdata.qualitymeasure.persistence.MeasureEvaluationRepository;
import com.healthdata.qualitymeasure.persistence.CohortMeasureRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Event Handler Configuration for Quality Measure Service
 *
 * Instantiates the Phase 4 QualityMeasureEventHandler as a Spring bean
 * Provides adapters for EventStore and MeasureProjectionStore interfaces
 */
@Configuration
@RequiredArgsConstructor
public class EventHandlerConfig {

    private final MeasureEvaluationRepository evaluationRepository;
    private final CohortMeasureRateRepository cohortRepository;

    /**
     * Create QualityMeasureEventHandler bean
     * Integrates Phase 4 event handler library with Spring dependency injection
     */
    @Bean
    public QualityMeasureEventHandler qualityMeasureEventHandler() {
        // Create in-memory event store adapter
        EventStore eventStore = new InMemoryEventStore();

        // Create projection store adapter backed by Spring Data JPA repositories
        MeasureProjectionStore projectionStore = new RepositoryBackedProjectionStore(evaluationRepository, cohortRepository);

        return new QualityMeasureEventHandler(projectionStore, eventStore);
    }

    /**
     * In-memory event store for append-only event log
     * In production, this would be replaced with a database-backed implementation
     */
    private static class InMemoryEventStore implements EventStore {
        @Override
        public void storeEvent(Object event) {
            // Append event to in-memory log or database
        }
    }

    /**
     * Projection store backed by Spring Data JPA repositories
     * Persists measure evaluation and cohort aggregation data for fast queries
     */
    private static class RepositoryBackedProjectionStore implements MeasureProjectionStore {
        private final MeasureEvaluationRepository evaluationRepository;
        private final CohortMeasureRateRepository cohortRepository;

        public RepositoryBackedProjectionStore(MeasureEvaluationRepository evaluationRepository,
                                              CohortMeasureRateRepository cohortRepository) {
            this.evaluationRepository = evaluationRepository;
            this.cohortRepository = cohortRepository;
        }

        @Override
        public void saveMeasureProjection(MeasureEvaluationProjection projection) {
            evaluationRepository.save(projection);
        }

        @Override
        public void saveCohortProjection(CohortMeasureRateProjection projection) {
            cohortRepository.save(projection);
        }

        @Override
        public MeasureEvaluationProjection getMeasureProjection(String patientId, String tenantId) {
            return evaluationRepository.findByPatientIdAndTenant(patientId, tenantId).orElse(null);
        }

        @Override
        public CohortMeasureRateProjection getCohortProjection(String measureCode, String tenantId) {
            return cohortRepository.findByMeasureCodeAndTenant(measureCode, tenantId).orElse(null);
        }
    }
}
