package com.healthdata.patientevent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.authentication.context.UserContext;
import com.healthdata.authentication.context.UserContextHolder;
import com.healthdata.eventsourcing.event.EventUserContext;
import com.healthdata.eventsourcing.kafka.UserContextKafkaInterceptor;
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
     * Converts REST request to PatientCreatedEvent and processes through handler.
     *
     * HIPAA Compliance:
     * - 45 CFR 164.312(b): Includes user context for audit trail
     * - 45 CFR 164.312(d): Captures authenticated user identity
     */
    public PatientEventResponse createPatientEvent(CreatePatientRequest request, String tenantId) {
        log.info("Creating patient event for tenant: {}, firstName: {}", tenantId, request.getFirstName());

        // Generate unique patient ID
        String patientId = "PATIENT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Build HIPAA-compliant user context for audit trail
        EventUserContext userContext = buildEventUserContext(tenantId);

        // Set user context for Kafka interceptor to propagate via headers
        setKafkaUserContext();

        // Create domain event with user context
        PatientCreatedEvent event = new PatientCreatedEvent(
            patientId,
            tenantId,
            request.getFirstName(),
            request.getLastName(),
            request.getDateOfBirth(),
            userContext
        );

        // Delegate to Phase 4 event handler for business logic
        patientEventHandler.handle(event);

        // Publish to Kafka for other services (UserContextKafkaInterceptor adds headers)
        kafkaTemplate.send(PATIENT_EVENTS_TOPIC, patientId, event);

        log.info("Patient event created: patientId={}, tenantId={}, user={}",
            patientId, tenantId, userContext != null ? userContext.getUsername() : "system");

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
     * Processes enrollment status change.
     *
     * HIPAA Compliance:
     * - 45 CFR 164.312(b): Audit trail via Kafka headers
     */
    public PatientEventResponse enrollPatientEvent(String enrollmentRequest, String tenantId) {
        log.info("Enrolling patient for tenant: {}", tenantId);

        // Set user context for Kafka interceptor
        setKafkaUserContext();

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

            // Publish to Kafka (UserContextKafkaInterceptor adds headers)
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
     * Processes demographic changes.
     *
     * HIPAA Compliance:
     * - 45 CFR 164.312(b): Audit trail via Kafka headers
     */
    public PatientEventResponse updateDemographicsEvent(String demographicsRequest, String tenantId) {
        log.info("Updating demographics for tenant: {}", tenantId);

        // Set user context for Kafka interceptor
        setKafkaUserContext();

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

            // Publish to Kafka (UserContextKafkaInterceptor adds headers)
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

    // ===== Helper methods for HIPAA-compliant user context =====

    /**
     * Build EventUserContext from the current authenticated user.
     *
     * HIPAA Compliance:
     * - Captures WHO is performing the action
     * - Records WHY (purpose of use)
     * - Includes tenant context for multi-tenant audit segregation
     */
    private EventUserContext buildEventUserContext(String tenantId) {
        UserContext context = UserContextHolder.getContext();

        if (context != null) {
            return EventUserContext.builder()
                .userId(context.userIdAsString())
                .username(context.username())
                .activeTenantId(tenantId)
                .roles(context.roles() != null ? String.join(",", context.roles()) : null)
                .ipAddress(context.ipAddress())
                .purposeOfUse("TREATMENT")  // Default for patient operations
                .initiatedAt(Instant.now().toString())
                .tokenId(context.tokenId())
                .userAgent(context.userAgent())
                .build();
        }

        // System context for operations without user (e.g., scheduled jobs)
        return EventUserContext.system(tenantId, "OPERATIONS");
    }

    /**
     * Set user context for Kafka interceptor to propagate via message headers.
     * This ensures audit context flows through event-driven processing.
     */
    private void setKafkaUserContext() {
        UserContext context = UserContextHolder.getContext();
        if (context != null) {
            UserContextKafkaInterceptor.setUserContext(context);
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
