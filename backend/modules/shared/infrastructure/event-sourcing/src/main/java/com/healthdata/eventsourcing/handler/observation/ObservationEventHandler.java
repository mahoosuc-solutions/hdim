package com.healthdata.eventsourcing.handler.observation;

import com.healthdata.eventsourcing.command.observation.ObservationRecordedEvent;
import com.healthdata.eventsourcing.projection.observation.ObservationProjection;
import com.healthdata.eventsourcing.projection.observation.ObservationProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Observation Event Handler - CQRS Read Model Update Handler
 *
 * Consumes ObservationRecordedEvent from Kafka topic and updates ObservationProjection
 * in the read model for efficient vital signs and time-series queries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObservationEventHandler {

    private final ObservationProjectionService projectionService;

    /**
     * Handles ObservationRecordedEvent from Kafka.
     *
     * @param event ObservationRecordedEvent from Kafka topic
     */
    @KafkaListener(
        topics = "observation-events",
        groupId = "observation-projection-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleObservationRecordedEvent(ObservationRecordedEvent event) {
        log.debug("Handling ObservationRecordedEvent for patient: {} in tenant: {}",
            event.getPatientId(), event.getTenantId());

        try {
            ObservationProjection projection = transformToProjection(event);
            projectionService.saveProjection(projection);

            log.info("Successfully created observation projection for patient: {} in tenant: {}",
                event.getPatientId(), event.getTenantId());

        } catch (Exception e) {
            log.error("Failed to handle ObservationRecordedEvent for patient: {} in tenant: {}",
                event.getPatientId(), event.getTenantId(), e);
            throw e;
        }
    }

    /**
     * Transforms ObservationRecordedEvent to ObservationProjection.
     *
     * @param event ObservationRecordedEvent from write model
     * @return ObservationProjection ready for persistence
     */
    private ObservationProjection transformToProjection(ObservationRecordedEvent event) {
        return ObservationProjection.builder()
            .tenantId(event.getTenantId())
            .patientId(event.getPatientId())
            .loincCode(event.getLoincCode())
            .value(event.getValue())
            .unit(event.getUnit())
            .observationDate(event.getObservationDate())
            .notes(event.getNotes())
            .build();
    }
}
