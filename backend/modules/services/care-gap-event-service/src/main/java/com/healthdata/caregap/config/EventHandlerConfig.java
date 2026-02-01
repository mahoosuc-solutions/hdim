package com.healthdata.caregap.config;

import com.healthdata.caregap.eventhandler.CareGapEventHandler;
import com.healthdata.caregap.eventhandler.CareGapEventHandler.EventStore;
import com.healthdata.caregap.eventhandler.CareGapEventHandler.CareGapProjectionStore;
import com.healthdata.caregap.persistence.CareGapProjectionRepository;
import com.healthdata.caregap.persistence.PopulationHealthRepository;
import com.healthdata.eventstore.client.EventStoreClient;
import com.healthdata.eventstore.client.dto.AppendEventRequest;
import com.healthdata.eventstore.client.dto.EventStoreEntry;
import com.healthdata.eventstore.client.exception.EventStoreException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Event Handler Configuration for Care Gap Service
 * Phase 5: Integrated with event-store-service for immutable event log
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class EventHandlerConfig {

    private final EventStoreClient eventStoreClient;

    @Bean
    public CareGapEventHandler careGapEventHandler(EventStore eventStore) {
        // No-op projection store - event service handles its own projections via Kafka listeners
        CareGapProjectionStore projectionStore = new NoOpProjectionStore();
        return new CareGapEventHandler(projectionStore, eventStore);
    }

    @Bean
    @ConditionalOnProperty(name = "event-store.enabled", havingValue = "true", matchIfMissing = true)
    public EventStore eventStore() {
        return new EventStoreServiceAdapter(eventStoreClient);
    }

    @Bean
    @ConditionalOnProperty(name = "event-store.enabled", havingValue = "false")
    public EventStore disabledEventStore() {
        return new NoOpEventStore();
    }

    /**
     * Event store adapter backed by event-store-service
     * Persists all care gap events to immutable event log
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
                String careGapId = extractCareGapId(event);
                String patientId = extractPatientId(event);
                String eventType = event.getClass().getSimpleName();

                // Use careGapId as aggregateId (or patientId if careGapId not available)
                UUID aggregateId = resolveAggregateId(careGapId, patientId);

                // Build request
                AppendEventRequest request = AppendEventRequest.builder()
                    .aggregateId(aggregateId)
                    .aggregateType("CareGap")
                    .eventType(eventType)
                    .payload(event)
                    .build();

                // Append to event store
                EventStoreEntry storedEvent = eventStoreClient.appendEvent(tenantId, request);

                log.info("Care gap event persisted to event store: id={}, aggregateId={}, eventType={}, version={}",
                    storedEvent.getId(), storedEvent.getAggregateId(), storedEvent.getEventType(),
                    storedEvent.getEventVersion());

            } catch (Exception e) {
                log.error("Failed to store care gap event in event store: {}", event.getClass().getSimpleName(), e);
                throw new EventStoreException("Care gap event persistence failed", e);
            }
        }

        private String extractTenantId(Object event) {
            try {
                Method method = event.getClass().getMethod("getTenantId");
                return (String) method.invoke(event);
            } catch (Exception e) {
                throw new EventStoreException("Failed to extract tenantId from care gap event", e);
            }
        }

        private String extractPatientId(Object event) {
            try {
                Method method = event.getClass().getMethod("getPatientId");
                return (String) method.invoke(event);
            } catch (Exception e) {
                // Some events might not have patientId
                return null;
            }
        }

        private String extractCareGapId(Object event) {
            try {
                Method method = event.getClass().getMethod("getCareGapId");
                Object value = method.invoke(event);
                return value != null ? value.toString() : null;
            } catch (Exception e) {
                // Some events might not have careGapId yet (e.g., creation events)
                return null;
            }
        }

        private UUID resolveAggregateId(String careGapId, String patientId) {
            String rawId = careGapId != null ? careGapId : patientId;
            if (rawId == null || rawId.isBlank()) {
                return UUID.randomUUID();
            }

            try {
                return UUID.fromString(rawId);
            } catch (IllegalArgumentException ex) {
                return UUID.nameUUIDFromBytes(rawId.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * No-op event store for test environments.
     */
    @Slf4j
    private static class NoOpEventStore implements EventStore {
        @Override
        public void storeEvent(Object event) {
            log.debug("Skipping event store persistence for {}", event != null ? event.getClass().getSimpleName() : "null");
        }
    }

    /**
     * No-op projection store
     *
     * Event service uses Kafka listeners to update projections asynchronously.
     * The handler library's projection store is not used in the event service architecture.
     */
    @Slf4j
    private static class NoOpProjectionStore implements CareGapProjectionStore {

        @Override
        public void saveCareGapProjection(com.healthdata.caregap.projection.CareGapProjection projection) {
            // No-op: Event service projections are updated via Kafka listeners
            log.debug("Care gap projection save delegated to Kafka listener");
        }

        @Override
        public com.healthdata.caregap.projection.CareGapProjection getCareGapProjection(String patientId, String tenantId, String gapCode) {
            // No-op: Event service queries its own projection tables
            log.debug("Care gap projection query delegated to event service repositories");
            return null;
        }

        @Override
        public void savePopulationHealth(com.healthdata.caregap.projection.PopulationHealthProjection projection) {
            // No-op: Event service projections are updated via Kafka listeners
            log.debug("Population health projection save delegated to Kafka listener");
        }

        @Override
        public com.healthdata.caregap.projection.PopulationHealthProjection getPopulationHealth(String tenantId) {
            // No-op: Event service queries its own projection tables
            log.debug("Population health projection query delegated to event service repositories");
            return null;
        }
    }
}
