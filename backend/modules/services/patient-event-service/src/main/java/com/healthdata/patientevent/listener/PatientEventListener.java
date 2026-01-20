package com.healthdata.patientevent.listener;

import com.healthdata.patientevent.projection.PatientProjection;
import com.healthdata.patientevent.repository.PatientProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Patient Event Listener (CQRS Event Handler)
 *
 * Consumes domain events from Kafka and updates the patient projection.
 *
 * Events consumed:
 * - patient.created: New patient in write model
 * - patient.updated: Patient demographics changed
 * - patient.status.changed: Patient status changed (ACTIVE/INACTIVE/DECEASED)
 * - care-gap.identified: New care gap for patient
 * - care-gap.closed: Care gap closed
 * - risk-assessment.updated: Patient risk level changed
 * - mental-health.updated: Mental health assessment completed
 * - clinical-alert.triggered: New clinical alert for patient
 * - clinical-alert.resolved: Clinical alert resolved
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientEventListener {

    private final PatientProjectionRepository patientProjectionRepository;

    /**
     * Handle patient.created event
     * Creates a new projection entry when patient is created
     */
    @KafkaListener(
        topics = "patient.created",
        groupId = "patient-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onPatientCreated(String tenantId, UUID patientId, String firstName, String lastName) {
        log.debug("Processing patient.created event for patient {} in tenant {}", patientId, tenantId);

        PatientProjection projection = PatientProjection.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .firstName(firstName)
            .lastName(lastName)
            .status("ACTIVE")
            .riskScore(0.0)
            .riskLevel("LOW")
            .openCareGapsCount(0)
            .urgentCareGapsCount(0)
            .activeAlertsCount(0)
            .hasCriticalAlert(false)
            .mentalHealthFlag(false)
            .createdAt(Instant.now())
            .lastUpdatedAt(Instant.now())
            .eventVersion(1L)
            .build();

        patientProjectionRepository.save(projection);
        log.info("Created patient projection for patient {} in tenant {}", patientId, tenantId);
    }

    /**
     * Handle patient.updated event
     * Updates patient demographics in projection
     */
    @KafkaListener(
        topics = "patient.updated",
        groupId = "patient-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onPatientUpdated(String tenantId, UUID patientId, String firstName, String lastName,
                                 String email, String phoneNumber) {
        log.debug("Processing patient.updated event for patient {}", patientId);

        patientProjectionRepository.findByTenantIdAndPatientId(tenantId, patientId)
            .ifPresentOrElse(
                projection -> {
                    projection.setFirstName(firstName);
                    projection.setLastName(lastName);
                    projection.setEmail(email);
                    projection.setPhoneNumber(phoneNumber);
                    projection.setLastUpdatedAt(Instant.now());
                    patientProjectionRepository.save(projection);
                    log.info("Updated patient projection for patient {}", patientId);
                },
                () -> {
                    log.warn("Patient projection not found for patient {} in tenant {}. Creating new one.", patientId, tenantId);
                    onPatientCreated(tenantId, patientId, firstName, lastName);
                }
            );
    }

    /**
     * Handle patient.status.changed event
     * Updates patient status (ACTIVE, INACTIVE, DECEASED)
     */
    @KafkaListener(
        topics = "patient.status.changed",
        groupId = "patient-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onPatientStatusChanged(String tenantId, UUID patientId, String status) {
        log.debug("Processing patient.status.changed event for patient {}: status={}", patientId, status);

        getOrCreateProjection(tenantId, patientId, patientId.toString())
            .ifPresent(projection -> {
                projection.setStatus(status);
                projection.setLastUpdatedAt(Instant.now());
                patientProjectionRepository.save(projection);
                log.info("Updated patient status for patient {}: {}", patientId, status);
            });
    }

    /**
     * Handle care-gap.identified event
     * Increments care gap counters
     */
    @KafkaListener(
        topics = "care-gap.identified",
        groupId = "patient-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onCareGapIdentified(String tenantId, UUID patientId, String priority) {
        log.debug("Processing care-gap.identified event for patient {}: priority={}", patientId, priority);

        getOrCreateProjection(tenantId, patientId, patientId.toString())
            .ifPresent(projection -> {
                projection.setOpenCareGapsCount(projection.getOpenCareGapsCount() + 1);
                if ("urgent".equalsIgnoreCase(priority)) {
                    projection.setUrgentCareGapsCount(projection.getUrgentCareGapsCount() + 1);
                }
                projection.setLastUpdatedAt(Instant.now());
                patientProjectionRepository.save(projection);
                log.debug("Updated care gap count for patient {}", patientId);
            });
    }

    /**
     * Handle care-gap.closed event
     * Decrements care gap counters
     */
    @KafkaListener(
        topics = "care-gap.closed",
        groupId = "patient-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onCareGapClosed(String tenantId, UUID patientId, String priority) {
        log.debug("Processing care-gap.closed event for patient {}: priority={}", patientId, priority);

        getOrCreateProjection(tenantId, patientId, patientId.toString())
            .ifPresent(projection -> {
                projection.setOpenCareGapsCount(Math.max(0, projection.getOpenCareGapsCount() - 1));
                if ("urgent".equalsIgnoreCase(priority)) {
                    projection.setUrgentCareGapsCount(Math.max(0, projection.getUrgentCareGapsCount() - 1));
                }
                projection.setLastUpdatedAt(Instant.now());
                patientProjectionRepository.save(projection);
                log.debug("Updated care gap count for patient {}", patientId);
            });
    }

    /**
     * Handle risk-assessment.updated event
     * Updates patient risk score and level
     */
    @KafkaListener(
        topics = "risk-assessment.updated",
        groupId = "patient-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onRiskAssessmentUpdated(String tenantId, UUID patientId, Double riskScore, String riskLevel) {
        log.debug("Processing risk-assessment.updated for patient {}: score={}, level={}",
            patientId, riskScore, riskLevel);

        getOrCreateProjection(tenantId, patientId, patientId.toString())
            .ifPresent(projection -> {
                projection.setRiskScore(riskScore);
                projection.setRiskLevel(riskLevel);
                projection.setLastUpdatedAt(Instant.now());
                patientProjectionRepository.save(projection);
                log.info("Updated risk assessment for patient {}", patientId);
            });
    }

    /**
     * Handle mental-health.updated event
     * Updates patient mental health flag and score
     */
    @KafkaListener(
        topics = "mental-health.updated",
        groupId = "patient-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onMentalHealthUpdated(String tenantId, UUID patientId, Integer score) {
        log.debug("Processing mental-health.updated for patient {}: score={}", patientId, score);

        getOrCreateProjection(tenantId, patientId, patientId.toString())
            .ifPresent(projection -> {
                projection.setMentalHealthFlag(true);
                projection.setMentalHealthScore(score);
                projection.setLastUpdatedAt(Instant.now());
                patientProjectionRepository.save(projection);
                log.info("Updated mental health assessment for patient {}", patientId);
            });
    }

    /**
     * Handle clinical-alert.triggered event
     * Increments alert counters
     */
    @KafkaListener(
        topics = "clinical-alert.triggered",
        groupId = "patient-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onClinicalAlertTriggered(String tenantId, UUID patientId, String severity) {
        log.debug("Processing clinical-alert.triggered for patient {}: severity={}", patientId, severity);

        getOrCreateProjection(tenantId, patientId, patientId.toString())
            .ifPresent(projection -> {
                projection.setActiveAlertsCount(projection.getActiveAlertsCount() + 1);
                if ("CRITICAL".equalsIgnoreCase(severity)) {
                    projection.setHasCriticalAlert(true);
                }
                projection.setLastUpdatedAt(Instant.now());
                patientProjectionRepository.save(projection);
                log.debug("Updated alert count for patient {}", patientId);
            });
    }

    /**
     * Handle clinical-alert.resolved event
     * Decrements alert counters
     */
    @KafkaListener(
        topics = "clinical-alert.resolved",
        groupId = "patient-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onClinicalAlertResolved(String tenantId, UUID patientId) {
        log.debug("Processing clinical-alert.resolved for patient {}", patientId);

        getOrCreateProjection(tenantId, patientId, patientId.toString())
            .ifPresent(projection -> {
                projection.setActiveAlertsCount(Math.max(0, projection.getActiveAlertsCount() - 1));
                if (projection.getActiveAlertsCount() == 0) {
                    projection.setHasCriticalAlert(false);
                }
                projection.setLastUpdatedAt(Instant.now());
                patientProjectionRepository.save(projection);
                log.debug("Updated alert count for patient {}", patientId);
            });
    }

    /**
     * Get or create patient projection
     */
    private java.util.Optional<PatientProjection> getOrCreateProjection(String tenantId, UUID patientId, String fhirId) {
        return patientProjectionRepository.findByTenantIdAndPatientId(tenantId, patientId)
            .or(() -> {
                log.warn("Patient projection not found for patient {} in tenant {}. Creating placeholder.", patientId, tenantId);
                PatientProjection projection = PatientProjection.builder()
                    .tenantId(tenantId)
                    .patientId(patientId)
                    .fhirId(fhirId)
                    .status("ACTIVE")
                    .riskScore(0.0)
                    .riskLevel("LOW")
                    .openCareGapsCount(0)
                    .urgentCareGapsCount(0)
                    .activeAlertsCount(0)
                    .hasCriticalAlert(false)
                    .mentalHealthFlag(false)
                    .createdAt(Instant.now())
                    .lastUpdatedAt(Instant.now())
                    .eventVersion(1L)
                    .build();
                return java.util.Optional.of(patientProjectionRepository.save(projection));
            });
    }
}
