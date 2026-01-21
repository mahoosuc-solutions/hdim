package com.healthdata.eventsourcing.handler.patient;

import com.healthdata.eventsourcing.command.patient.PatientCreatedEvent;
import com.healthdata.eventsourcing.projection.patient.PatientProjection;
import com.healthdata.eventsourcing.projection.patient.PatientProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Patient Event Handler - CQRS Read Model Update Handler
 *
 * Consumes PatientCreatedEvent from Kafka topic and updates PatientProjection
 * in the read model for efficient patient lookup and search queries.
 *
 * Responsibility:
 * - Listen for PatientCreatedEvent on Kafka
 * - Deserialize event message
 * - Transform event to projection
 * - Save projection to read model database
 *
 * Multi-tenant: Yes - enforces tenant ID in projection
 * Idempotency: Delegated to event store (no duplicate events)
 * Error handling: Exception propagation allows Kafka retry
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientEventHandler {

    private final PatientProjectionService projectionService;

    /**
     * Handles PatientCreatedEvent from Kafka.
     *
     * Event Flow:
     * 1. Receive event from Kafka
     * 2. Deserialize to PatientCreatedEvent
     * 3. Transform to PatientProjection (read model)
     * 4. Save to patient_projections table
     *
     * @param event PatientCreatedEvent from Kafka topic
     * @throws RuntimeException if persistence fails (triggers Kafka retry)
     */
    @KafkaListener(
        topics = "patient-events",
        groupId = "patient-projection-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePatientCreatedEvent(PatientCreatedEvent event) {
        log.debug("Handling PatientCreatedEvent for patient: {} in tenant: {}",
            event.getPatientId(), event.getTenantId());

        try {
            // Transform event to projection
            PatientProjection projection = transformToProjection(event);

            // Save to read model
            projectionService.saveProjection(projection);

            log.info("Successfully created patient projection for patient: {} in tenant: {}",
                event.getPatientId(), event.getTenantId());

        } catch (Exception e) {
            log.error("Failed to handle PatientCreatedEvent for patient: {} in tenant: {}",
                event.getPatientId(), event.getTenantId(), e);
            throw e;
        }
    }

    /**
     * Transforms PatientCreatedEvent to PatientProjection.
     *
     * Maps all event fields to projection entity fields for read model.
     * Preserves tenant context for multi-tenant isolation.
     *
     * @param event PatientCreatedEvent from write model
     * @return PatientProjection ready for persistence
     */
    private PatientProjection transformToProjection(PatientCreatedEvent event) {
        return PatientProjection.builder()
            .patientId(event.getPatientId())
            .tenantId(event.getTenantId())
            .mrn(event.getMrn())
            .firstName(event.getFirstName())
            .lastName(event.getLastName())
            .dateOfBirth(event.getDateOfBirth())
            .gender(event.getGender())
            .insuranceMemberId(event.getInsuranceMemberId())
            .build();
    }
}
