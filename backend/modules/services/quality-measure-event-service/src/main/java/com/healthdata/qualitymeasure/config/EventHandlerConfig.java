package com.healthdata.qualitymeasure.config;

import com.healthdata.eventstore.client.EventStoreClient;
import com.healthdata.eventstore.client.dto.AppendEventRequest;
import com.healthdata.eventstore.client.dto.EventStoreEntry;
import com.healthdata.eventstore.client.exception.EventStoreException;
import com.healthdata.qualityevent.eventhandler.QualityMeasureEventHandler;
import com.healthdata.qualityevent.eventhandler.QualityMeasureEventHandler.EventStore;
import com.healthdata.qualityevent.eventhandler.QualityMeasureEventHandler.MeasureProjectionStore;
import com.healthdata.qualityevent.projection.MeasureEvaluationProjection;
import com.healthdata.qualityevent.projection.CohortMeasureRateProjection;
import com.healthdata.qualitymeasure.persistence.MeasureEvaluationRepository;
import com.healthdata.qualitymeasure.persistence.CohortMeasureRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Event Handler Configuration for Quality Measure Service
 * Phase 5: Integrated with event-store-service for immutable event log
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class EventHandlerConfig {

    private final EventStoreClient eventStoreClient;

    /**
     * Create QualityMeasureEventHandler bean
     * Integrates Phase 5 event handler library with event-store-service integration
     */
    @Bean
    public QualityMeasureEventHandler qualityMeasureEventHandler() {
        // Create event store adapter backed by event-store-service
        EventStore eventStore = new EventStoreServiceAdapter(eventStoreClient);

        // No-op projection store - event service handles projections via Kafka listeners
        MeasureProjectionStore projectionStore = new NoOpProjectionStore();

        return new QualityMeasureEventHandler(projectionStore, eventStore);
    }

    /**
     * Event store adapter backed by event-store-service
     * Persists all quality measure events to immutable event log
     */
    @Slf4j
    private static class EventStoreServiceAdapter implements EventStore {
        private final EventStoreClient eventStoreClient;

        public EventStoreServiceAdapter(EventStoreClient client) {
            this.eventStoreClient = client;
        }

        @Override
        public void storeEvent(Object event) {
            try {
                // Extract metadata from event using reflection
                String tenantId = extractTenantId(event);
                String patientId = extractPatientId(event);
                String measureCode = extractMeasureCode(event);
                String eventType = event.getClass().getSimpleName();

                // Use patientId as aggregateId
                UUID aggregateId = UUID.fromString(patientId);

                // Build request
                AppendEventRequest request = AppendEventRequest.builder()
                    .aggregateId(aggregateId)
                    .aggregateType("QualityMeasure")
                    .eventType(eventType)
                    .payload(event)
                    .build();

                // Append to event store
                EventStoreEntry storedEvent = eventStoreClient.appendEvent(tenantId, request);

                log.info("Quality measure event persisted: id={}, aggregateId={}, eventType={}, measureCode={}, version={}",
                    storedEvent.getId(), storedEvent.getAggregateId(), storedEvent.getEventType(),
                    measureCode, storedEvent.getEventVersion());

            } catch (Exception e) {
                log.error("Failed to store quality measure event: {}", event.getClass().getSimpleName(), e);
                throw new EventStoreException("Quality measure event persistence failed", e);
            }
        }

        private String extractTenantId(Object event) {
            try {
                Method method = event.getClass().getMethod("getTenantId");
                return (String) method.invoke(event);
            } catch (Exception e) {
                throw new EventStoreException("Failed to extract tenantId from quality measure event", e);
            }
        }

        private String extractPatientId(Object event) {
            try {
                Method method = event.getClass().getMethod("getPatientId");
                return (String) method.invoke(event);
            } catch (Exception e) {
                throw new EventStoreException("Failed to extract patientId from quality measure event", e);
            }
        }

        private String extractMeasureCode(Object event) {
            try {
                Method method = event.getClass().getMethod("getMeasureCode");
                Object value = method.invoke(event);
                return value != null ? value.toString() : "UNKNOWN";
            } catch (Exception e) {
                return "UNKNOWN";
            }
        }
    }

    /**
     * No-op projection store
     *
     * Event service uses Kafka listeners to update projections asynchronously.
     * The handler library's projection store is not used in the event service architecture.
     */
    @Slf4j
    private static class NoOpProjectionStore implements MeasureProjectionStore {

        @Override
        public void saveMeasureProjection(MeasureEvaluationProjection projection) {
            // No-op: Event service projections are updated via Kafka listeners
            log.debug("Measure projection save delegated to Kafka listener");
        }

        @Override
        public void saveCohortRate(CohortMeasureRateProjection projection) {
            // No-op: Event service projections are updated via Kafka listeners
            log.debug("Cohort rate projection save delegated to Kafka listener");
        }

        @Override
        public MeasureEvaluationProjection getMeasureProjection(String patientId, String tenantId, String measureCode) {
            // No-op: Event service queries its own projection tables
            log.debug("Measure projection query delegated to event service repositories");
            return null;
        }

        @Override
        public CohortMeasureRateProjection getCohortRate(String tenantId, String measureCode) {
            // No-op: Event service queries its own projection tables
            log.debug("Cohort rate query delegated to event service repositories");
            return null;
        }

        @Override
        public void saveRiskScoreProjection(com.healthdata.qualityevent.projection.RiskScoreProjection projection) {
            // No-op: Event service projections are updated via Kafka listeners
            log.debug("Risk score projection save delegated to Kafka listener");
        }

        @Override
        public com.healthdata.qualityevent.projection.RiskScoreProjection getRiskScoreProjection(String patientId, String tenantId) {
            // No-op: Event service queries its own projection tables
            log.debug("Risk score projection query delegated to event service repositories");
            return null;
        }
    }
}
