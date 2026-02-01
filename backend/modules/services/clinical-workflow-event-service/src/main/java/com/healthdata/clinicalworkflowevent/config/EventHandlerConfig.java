package com.healthdata.clinicalworkflowevent.config;

import com.healthdata.eventstore.client.EventStoreClient;
import com.healthdata.eventstore.client.dto.AppendEventRequest;
import com.healthdata.eventstore.client.dto.EventStoreEntry;
import com.healthdata.eventstore.client.exception.EventStoreException;
import com.healthdata.workflow.eventhandler.ClinicalWorkflowEventHandler;
import com.healthdata.workflow.eventhandler.ClinicalWorkflowEventHandler.EventStore;
import com.healthdata.workflow.eventhandler.ClinicalWorkflowEventHandler.WorkflowProjectionStore;
import com.healthdata.workflow.projection.WorkflowProjection;
import com.healthdata.clinicalworkflowevent.repository.WorkflowProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Event Handler Configuration for Clinical Workflow Service
 * Phase 5: Integrated with event-store-service for immutable event log
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class EventHandlerConfig {

    private final EventStoreClient eventStoreClient;

    /**
     * Create ClinicalWorkflowEventHandler bean
     * Integrates Phase 5 event handler library with event-store-service integration
     */
    @Bean
    public ClinicalWorkflowEventHandler clinicalWorkflowEventHandler() {
        // Create event store adapter backed by event-store-service
        EventStore eventStore = new EventStoreServiceAdapter(eventStoreClient);

        // No-op projection store - event service handles projections via Kafka listeners
        WorkflowProjectionStore projectionStore = new NoOpProjectionStore();

        return new ClinicalWorkflowEventHandler(projectionStore, eventStore);
    }

    /**
     * Event store adapter backed by event-store-service
     * Persists all clinical workflow events to immutable event log
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
                String workflowType = extractWorkflowType(event);
                String eventType = event.getClass().getSimpleName();

                // Use patientId as aggregateId
                UUID aggregateId = UUID.fromString(patientId);

                // Build request
                AppendEventRequest request = AppendEventRequest.builder()
                    .aggregateId(aggregateId)
                    .aggregateType("ClinicalWorkflow")
                    .eventType(eventType)
                    .payload(event)
                    .build();

                // Append to event store
                EventStoreEntry storedEvent = eventStoreClient.appendEvent(tenantId, request);

                log.info("Clinical workflow event persisted: id={}, aggregateId={}, eventType={}, workflowType={}, version={}",
                    storedEvent.getId(), storedEvent.getAggregateId(), storedEvent.getEventType(),
                    workflowType, storedEvent.getEventVersion());

            } catch (Exception e) {
                log.error("Failed to store clinical workflow event: {}", event.getClass().getSimpleName(), e);
                throw new EventStoreException("Clinical workflow event persistence failed", e);
            }
        }

        private String extractTenantId(Object event) {
            try {
                Method method = event.getClass().getMethod("getTenantId");
                return (String) method.invoke(event);
            } catch (Exception e) {
                throw new EventStoreException("Failed to extract tenantId from clinical workflow event", e);
            }
        }

        private String extractPatientId(Object event) {
            try {
                Method method = event.getClass().getMethod("getPatientId");
                return (String) method.invoke(event);
            } catch (Exception e) {
                throw new EventStoreException("Failed to extract patientId from clinical workflow event", e);
            }
        }

        private String extractWorkflowType(Object event) {
            try {
                Method method = event.getClass().getMethod("getWorkflowType");
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
    private static class NoOpProjectionStore implements WorkflowProjectionStore {

        @Override
        public void saveWorkflowProjection(WorkflowProjection projection) {
            // No-op: Event service projections are updated via Kafka listeners
            log.debug("Workflow projection save delegated to Kafka listener");
        }

        @Override
        public WorkflowProjection getWorkflowProjection(String patientId, String tenantId, String workflowType) {
            // No-op: Event service queries its own projection tables
            log.debug("Workflow projection query delegated to event service repositories");
            return null;
        }
    }
}
