package com.healthdata.patientevent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.patientevent.api.v1.dto.CreatePatientRequest;
import com.healthdata.patientevent.api.v1.dto.PatientEventResponse;
import com.healthdata.patientevent.persistence.PatientProjectionRepository;
import com.healthdata.patientevent.event.PatientCreatedEvent;
import com.healthdata.patientevent.event.PatientEnrollmentChangedEvent;
import com.healthdata.patientevent.event.PatientDemographicsUpdatedEvent;
import com.healthdata.patientevent.eventhandler.PatientEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Patient Event Application Service
 *
 * Orchestrates patient event processing:
 * 1. Receives REST requests from PatientEventController
 * 2. Creates domain events (PatientCreatedEvent, etc.)
 * 3. Delegates to Phase 4 PatientEventHandler for business logic
 * 4. Publishes events to Kafka topic for other services
 * 5. Persists projections to database for query optimization
 *
 * ★ Insight ─────────────────────────────────────
 * This service layer is the bridge between REST and event domain:
 * - Converts HTTP requests to domain events
 * - Delegates to specialized Phase 4 handlers (contains business logic)
 * - Publishes to Kafka for eventual consistency across services
 * - Persists read models (projections) for fast queries
 * ─────────────────────────────────────────────────
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PatientEventApplicationService {

    private final PatientEventHandler patientEventHandler;
    private final PatientProjectionRepository projectionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String PATIENT_EVENTS_TOPIC = "patient.events";

    /**
     * Create patient event
     *
     * Converts REST request to PatientCreatedEvent and processes through handler
     */
    public PatientEventResponse createPatientEvent(CreatePatientRequest request, String tenantId) {
        log.info("Creating patient event for tenant: {}, firstName: {}", tenantId, request.getFirstName());

        // Generate unique patient ID
        String patientId = "PATIENT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Create domain event
        PatientCreatedEvent event = new PatientCreatedEvent(
            patientId,
            tenantId,
            request.getFirstName(),
            request.getLastName(),
            request.getDateOfBirth()
        );

        // Delegate to Phase 4 event handler for business logic
        patientEventHandler.handle(event);

        // Publish to Kafka for other services
        kafkaTemplate.send(PATIENT_EVENTS_TOPIC, patientId, event);

        log.info("Patient event created: patientId={}, tenantId={}", patientId, tenantId);

        // Return response with created patient ID
        return PatientEventResponse.builder()
            .patientId(patientId)
            .status("CREATED")
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Enroll patient event
     *
     * Processes enrollment status change
     */
    public PatientEventResponse enrollPatientEvent(String enrollmentRequest, String tenantId) {
        log.info("Enrolling patient for tenant: {}", tenantId);

        try {
            // Parse enrollment request
            EnrollmentRequest request = objectMapper.readValue(enrollmentRequest, EnrollmentRequest.class);

            // Create domain event
            PatientEnrollmentChangedEvent event = new PatientEnrollmentChangedEvent(
                request.patientId,
                tenantId,
                request.newStatus,
                request.reason
            );

            // Delegate to Phase 4 event handler
            patientEventHandler.handle(event);

            // Publish to Kafka
            kafkaTemplate.send(PATIENT_EVENTS_TOPIC, request.patientId, event);

            log.info("Patient enrolled: patientId={}, status={}", request.patientId, request.newStatus);

            return PatientEventResponse.builder()
                .patientId(request.patientId)
                .status(request.newStatus)
                .timestamp(Instant.now())
                .build();

        } catch (Exception e) {
            log.error("Error processing enrollment event", e);
            throw new RuntimeException("Failed to process enrollment event", e);
        }
    }

    /**
     * Update patient demographics event
     *
     * Processes demographic changes
     */
    public PatientEventResponse updateDemographicsEvent(String demographicsRequest, String tenantId) {
        log.info("Updating demographics for tenant: {}", tenantId);

        try {
            // Parse demographics request
            DemographicsRequest request = objectMapper.readValue(demographicsRequest, DemographicsRequest.class);

            // Create domain event
            PatientDemographicsUpdatedEvent event = new PatientDemographicsUpdatedEvent(
                request.patientId,
                tenantId,
                request.firstName,
                request.lastName,
                request.dateOfBirth
            );

            // Delegate to Phase 4 event handler
            patientEventHandler.handle(event);

            // Publish to Kafka
            kafkaTemplate.send(PATIENT_EVENTS_TOPIC, request.patientId, event);

            log.info("Demographics updated: patientId={}", request.patientId);

            return PatientEventResponse.builder()
                .patientId(request.patientId)
                .status("DEMOGRAPHICS_UPDATED")
                .timestamp(Instant.now())
                .build();

        } catch (Exception e) {
            log.error("Error processing demographics event", e);
            throw new RuntimeException("Failed to process demographics event", e);
        }
    }

    // ===== Helper DTOs for request parsing =====

    static class EnrollmentRequest {
        public String patientId;
        public String newStatus;
        public String reason;
    }

    static class DemographicsRequest {
        public String patientId;
        public String firstName;
        public String lastName;
        public String dateOfBirth;
    }
}
