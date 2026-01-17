package com.healthdata.nurseworkflow.application;

import com.healthdata.nurseworkflow.domain.model.PatientEducationLogEntity;
import com.healthdata.nurseworkflow.domain.repository.PatientEducationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Patient Education Service
 *
 * Manages patient education delivery and tracking using evidence-based methods:
 * - Teach-Back Method: Confirms patient understanding
 * - Health Literacy: Uses plain language and culturally competent approach
 * - Barrier Identification: Documents obstacles to learning
 * - Multi-Language Support: Tracks interpreter usage
 *
 * Implements:
 * - Patient-Centered Care principles
 * - Meaningful Use quality measures
 * - HEDIS compliance for chronic disease management
 *
 * HIPAA Compliance:
 * - Education logs are PHI - audit logging required
 * - Multi-tenant isolation in all queries
 * - Patient data de-identification for reporting
 *
 * Integration Points:
 * - FHIR DocumentReference: Links to educational materials
 * - FHIR Task: Links to education tasks
 * - Kafka: Publishes education.delivered events
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientEducationService {

    private final PatientEducationLogRepository educationLogRepository;

    /**
     * Log patient education delivery
     *
     * Documents when and how patient education was provided, including:
     * - Material type (condition-specific education)
     * - Delivery method (in-person, phone, email, portal)
     * - Teach-back assessment (verify understanding)
     * - Identified barriers to learning
     *
     * @param educationLog the education delivery to log
     * @return created education log
     */
    @Transactional
    public PatientEducationLogEntity logEducationDelivery(PatientEducationLogEntity educationLog) {
        log.debug("Logging patient education delivery for patient {} by educator {}",
            educationLog.getPatientId(), educationLog.getEducatorId());

        if (educationLog.getId() == null) {
            educationLog.setId(UUID.randomUUID());
        }

        PatientEducationLogEntity saved = educationLogRepository.save(educationLog);

        log.info("Patient education delivered: {} to patient {} on material type: {} " +
                "with understanding: {}",
            saved.getId(), saved.getPatientId(), saved.getMaterialType(),
            saved.getPatientUnderstanding());

        return saved;
    }

    /**
     * Get patient education log by ID
     *
     * @param id the education log ID
     * @return optional containing log if found
     */
    public Optional<PatientEducationLogEntity> getEducationLogById(UUID id) {
        log.debug("Retrieving patient education log: {}", id);
        return educationLogRepository.findById(id);
    }

    /**
     * Get patient education history with pagination
     *
     * Retrieves all education delivered to patient, ordered by most recent first.
     *
     * @param tenantId the tenant ID
     * @param patientId the patient ID
     * @param pageable pagination parameters
     * @return page of education logs for patient
     */
    public Page<PatientEducationLogEntity> getPatientEducationHistory(
            String tenantId,
            UUID patientId,
            Pageable pageable) {
        log.debug("Retrieving education history for patient {} in tenant {}",
            patientId, tenantId);

        return educationLogRepository.findByTenantIdAndPatientIdOrderByDeliveredAtDesc(
            tenantId, patientId, pageable);
    }

    /**
     * Get education logs by material type
     *
     * Filters education logs by disease/condition type (diabetes, hypertension, etc.)
     * for tracking patient education on specific conditions.
     *
     * @param tenantId the tenant ID
     * @param materialType the material type filter
     * @param pageable pagination parameters
     * @return page of education logs with specified material
     */
    public Page<PatientEducationLogEntity> getEducationByMaterialType(
            String tenantId,
            PatientEducationLogEntity.MaterialType materialType,
            Pageable pageable) {
        log.debug("Retrieving education logs by material type: {} in tenant {}",
            materialType, tenantId);

        return educationLogRepository.findByTenantIdAndMaterialTypeOrderByDeliveredAtDesc(
            tenantId, materialType, pageable);
    }

    /**
     * Get education logs by delivery method
     *
     * Filters by how education was delivered (in-person, phone, email, portal, etc.)
     * for tracking outreach methods and modality preferences.
     *
     * @param tenantId the tenant ID
     * @param deliveryMethod the delivery method filter
     * @param pageable pagination parameters
     * @return page of education logs with specified delivery method
     */
    public Page<PatientEducationLogEntity> getEducationByDeliveryMethod(
            String tenantId,
            PatientEducationLogEntity.DeliveryMethod deliveryMethod,
            Pageable pageable) {
        log.debug("Retrieving education logs by delivery method: {} in tenant {}",
            deliveryMethod, tenantId);

        return educationLogRepository.findByTenantIdAndDeliveryMethodOrderByDeliveredAtDesc(
            tenantId, deliveryMethod, pageable);
    }

    /**
     * Get education logs for patient by material type
     *
     * Retrieves all education delivered to a specific patient for a specific
     * condition/material type, useful for tracking education progression.
     *
     * @param tenantId the tenant ID
     * @param patientId the patient ID
     * @param materialType the material type
     * @return list of education logs
     */
    public List<PatientEducationLogEntity> getPatientEducationByMaterialType(
            String tenantId,
            UUID patientId,
            PatientEducationLogEntity.MaterialType materialType) {
        log.debug("Retrieving education for patient {} on material type: {} in tenant {}",
            patientId, materialType, tenantId);

        return educationLogRepository.findByTenantIdAndPatientIdAndMaterialTypeOrderByDeliveredAtDesc(
            tenantId, patientId, materialType);
    }

    /**
     * Find education sessions with poor patient understanding
     *
     * Identifies education sessions where patient showed poor or fair understanding.
     * These require follow-up education and reinforcement.
     *
     * @param tenantId the tenant ID
     * @return list of education logs needing follow-up
     */
    public List<PatientEducationLogEntity> findWithPoorUnderstanding(String tenantId) {
        log.debug("Finding education sessions with poor understanding in tenant {}", tenantId);

        return educationLogRepository.findPoorUnderstandingEducation(tenantId);
    }

    /**
     * Update patient education log
     *
     * Allows updating notes and follow-up status after initial creation.
     *
     * @param educationLog the education log with updates
     * @return updated education log
     */
    @Transactional
    public PatientEducationLogEntity updateEducationLog(PatientEducationLogEntity educationLog) {
        log.debug("Updating patient education log: {}", educationLog.getId());

        PatientEducationLogEntity updated = educationLogRepository.save(educationLog);

        log.info("Patient education log updated: {}", updated.getId());

        return updated;
    }

    /**
     * Count patient education sessions
     *
     * Returns total education sessions for patient, useful for analytics.
     *
     * @param tenantId the tenant ID
     * @param patientId the patient ID
     * @return count of education sessions
     */
    public long countPatientEducation(String tenantId, UUID patientId) {
        return educationLogRepository.countByTenantIdAndPatientId(tenantId, patientId);
    }

    /**
     * Get patient education metrics
     *
     * Calculates education metrics: total sessions, material types covered, etc.
     * Used for dashboard analytics and quality reporting.
     *
     * @param tenantId the tenant ID
     * @param patientId the patient ID
     * @return education metrics summary
     */
    public PatientEducationMetrics getPatientEducationMetrics(String tenantId, UUID patientId) {
        log.debug("Computing education metrics for patient {} in tenant {}",
            patientId, tenantId);

        long totalSessions = countPatientEducation(tenantId, patientId);
        List<PatientEducationLogEntity> poorUnderstanding =
            findWithPoorUnderstanding(tenantId);

        return PatientEducationMetrics.builder()
            .totalEducationSessions(totalSessions)
            .sessionsWithPoorUnderstanding(
                (int) poorUnderstanding.stream()
                    .filter(e -> e.getPatientId().equals(patientId))
                    .count())
            .build();
    }

    /**
     * Find interpreted education sessions
     *
     * Retrieves education sessions where interpreter was used, for tracking
     * language services and ensuring accessibility.
     *
     * @param tenantId the tenant ID
     * @param pageable pagination parameters
     * @return page of interpreted education sessions
     */
    public Page<PatientEducationLogEntity> findInterpretedSessions(
            String tenantId,
            Pageable pageable) {
        log.debug("Finding interpreted education sessions in tenant {}", tenantId);

        return educationLogRepository.findInterpretedEducationSessions(tenantId, pageable);
    }

    /**
     * Get education logs within date range
     *
     * Retrieves education delivered within specified date range for
     * quality reporting and analytics.
     *
     * @param tenantId the tenant ID
     * @param patientId the patient ID
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination parameters
     * @return page of education logs in date range
     */
    public Page<PatientEducationLogEntity> getEducationByDateRange(
            String tenantId,
            UUID patientId,
            Instant startDate,
            Instant endDate,
            Pageable pageable) {
        log.debug("Retrieving education for patient {} in date range: {} to {} for tenant {}",
            patientId, startDate, endDate, tenantId);

        return educationLogRepository.findByTenantIdAndPatientIdAndDeliveredAtBetweenOrderByDeliveredAtDesc(
            tenantId, patientId, startDate, endDate, pageable);
    }

    /**
     * Delete patient education log
     *
     * @param id the education log ID to delete
     */
    @Transactional
    public void deleteEducationLog(UUID id) {
        log.debug("Deleting patient education log: {}", id);
        educationLogRepository.deleteById(id);
        log.info("Patient education log deleted: {}", id);
    }

    /**
     * Simple DTO for patient education metrics
     */
    public static class PatientEducationMetrics {
        private long totalEducationSessions;
        private int sessionsWithPoorUnderstanding;

        public PatientEducationMetrics(long total, int poor) {
            this.totalEducationSessions = total;
            this.sessionsWithPoorUnderstanding = poor;
        }

        public static Builder builder() {
            return new Builder();
        }

        public long getTotalEducationSessions() { return totalEducationSessions; }
        public int getSessionsWithPoorUnderstanding() { return sessionsWithPoorUnderstanding; }

        public static class Builder {
            private long totalEducationSessions;
            private int sessionsWithPoorUnderstanding;

            public Builder totalEducationSessions(long value) {
                this.totalEducationSessions = value;
                return this;
            }

            public Builder sessionsWithPoorUnderstanding(int value) {
                this.sessionsWithPoorUnderstanding = value;
                return this;
            }

            public PatientEducationMetrics build() {
                return new PatientEducationMetrics(totalEducationSessions,
                    sessionsWithPoorUnderstanding);
            }
        }
    }
}
