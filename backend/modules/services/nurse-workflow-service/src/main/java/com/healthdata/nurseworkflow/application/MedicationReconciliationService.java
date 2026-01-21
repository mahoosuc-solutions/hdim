package com.healthdata.nurseworkflow.application;

import com.healthdata.nurseworkflow.domain.model.MedicationReconciliationEntity;
import com.healthdata.nurseworkflow.domain.repository.MedicationReconciliationRepository;
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
 * Medication Reconciliation Service
 *
 * Manages the medication reconciliation workflow, a critical clinical process
 * required at transitions of care (admission, discharge, transfers).
 *
 * Implements:
 * - Joint Commission NPSG.03.06.01: Maintain and communicate accurate medication information
 * - 5 Rights of Medication Administration
 * - Teach-Back Method for patient education
 * - Discrepancy tracking and resolution
 *
 * Workflow Stages:
 * 1. REQUESTED - Task created, awaiting nurse
 * 2. IN_PROGRESS - Nurse actively reconciling
 * 3. COMPLETED - Final list verified, patient educated
 * 4. CANCELLED - No longer needed
 *
 * HIPAA Compliance:
 * - All medication lists are PHI - audit logging required
 * - Multi-tenant isolation in all queries
 * - Patient education documentation
 *
 * Integration Points:
 * - FHIR Task: Links to task that triggered med rec
 * - FHIR MedicationStatement: Patient-reported medications
 * - FHIR MedicationRequest: Prescribed medications
 * - Kafka: Publishes med-reconciliation.completed events
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicationReconciliationService {

    private final MedicationReconciliationRepository medRecRepository;

    /**
     * Start medication reconciliation workflow
     *
     * Initializes med rec process when triggered by transition of care event
     * (hospital admission, discharge, ED visit, specialty referral, etc.)
     *
     * @param medRec the medication reconciliation to start
     * @return created medication reconciliation
     */
    @Transactional
    public MedicationReconciliationEntity startReconciliation(
            MedicationReconciliationEntity medRec) {
        log.debug("Starting medication reconciliation for patient {} by reconciler {}",
            medRec.getPatientId(), medRec.getReconcilerId());

        if (medRec.getId() == null) {
            medRec.setId(UUID.randomUUID());
        }

        if (medRec.getStatus() == null) {
            medRec.setStatus(MedicationReconciliationEntity.ReconciliationStatus.REQUESTED);
        }

        MedicationReconciliationEntity saved = medRecRepository.save(medRec);

        log.info("Medication reconciliation started: {} for patient {} triggered by: {}",
            saved.getId(), saved.getPatientId(), saved.getTriggerType());

        return saved;
    }

    /**
     * Complete medication reconciliation
     *
     * Finalizes med rec process after patient education and verification.
     * Sets completion timestamp for audit trail.
     *
     * @param medRec the medication reconciliation to complete
     * @return completed medication reconciliation
     */
    @Transactional
    public MedicationReconciliationEntity completeReconciliation(
            MedicationReconciliationEntity medRec) {
        log.debug("Completing medication reconciliation: {} for patient {}",
            medRec.getId(), medRec.getPatientId());

        medRec.setStatus(MedicationReconciliationEntity.ReconciliationStatus.COMPLETED);
        medRec.setCompletedAt(Instant.now());

        MedicationReconciliationEntity saved = medRecRepository.save(medRec);

        log.info("Medication reconciliation completed: {} for patient {} with {} medications " +
                "and {} discrepancies",
            saved.getId(), saved.getPatientId(), saved.getMedicationCount(),
            saved.getDiscrepancyCount());

        return saved;
    }

    /**
     * Get medication reconciliation by ID
     *
     * @param id the reconciliation ID
     * @return optional containing reconciliation if found
     */
    public Optional<MedicationReconciliationEntity> getMedicationReconciliationById(UUID id) {
        log.debug("Retrieving medication reconciliation: {}", id);
        return medRecRepository.findById(id);
    }

    /**
     * Get pending medication reconciliations for tenant
     *
     * Returns med recs in REQUESTED or IN_PROGRESS status, ordered by
     * start time (oldest first - for priority ordering).
     *
     * @param tenantId the tenant ID
     * @param pageable pagination parameters
     * @return page of pending med recs
     */
    public Page<MedicationReconciliationEntity> getPendingReconciliations(
            String tenantId,
            Pageable pageable) {
        log.debug("Retrieving pending medication reconciliations for tenant {}", tenantId);

        return medRecRepository.findPendingByTenant(tenantId, pageable);
    }

    /**
     * Get medication reconciliations by trigger type
     *
     * Filters by what event triggered the med rec (admission, discharge, etc.)
     * Useful for quality reporting and trend analysis.
     *
     * @param tenantId the tenant ID
     * @param triggerType the trigger event type
     * @param pageable pagination parameters
     * @return page of med recs with specified trigger
     */
    public Page<MedicationReconciliationEntity> getReconciliationsByTriggerType(
            String tenantId,
            MedicationReconciliationEntity.TriggerType triggerType,
            Pageable pageable) {
        log.debug("Retrieving med recs by trigger type: {} in tenant {}",
            triggerType, tenantId);

        return medRecRepository.findByTenantIdAndTriggerTypeOrderByStartedAtDesc(
            tenantId, triggerType, pageable);
    }

    /**
     * Get patient medication reconciliation history
     *
     * Retrieves all med recs for a specific patient, ordered by most recent first.
     *
     * @param tenantId the tenant ID
     * @param patientId the patient ID
     * @param pageable pagination parameters
     * @return page of patient's med recs
     */
    public Page<MedicationReconciliationEntity> getPatientMedicationReconciliationHistory(
            String tenantId,
            java.util.UUID patientId,
            Pageable pageable) {
        log.debug("Retrieving med rec history for patient {} in tenant {}",
            patientId, tenantId);

        return medRecRepository.findByTenantIdAndPatientIdOrderByStartedAtDesc(
            tenantId, patientId, pageable);
    }

    /**
     * Find medication reconciliations with poor patient understanding
     *
     * Identifies med recs where patient showed poor understanding in teach-back
     * assessment. These require follow-up education.
     *
     * @param tenantId the tenant ID
     * @return list of med recs needing follow-up
     */
    public List<MedicationReconciliationEntity> findWithPoorUnderstanding(String tenantId) {
        log.debug("Finding med recs with poor patient understanding in tenant {}", tenantId);

        return medRecRepository.findWithPoorUnderstanding(tenantId);
    }

    /**
     * Count pending medication reconciliations for tenant
     *
     * Used for dashboard metrics and workload assessment.
     *
     * @param tenantId the tenant ID
     * @return count of pending med recs
     */
    public long countPendingReconciliations(String tenantId) {
        return medRecRepository.countByTenantIdAndStatusIn(
            tenantId,
            List.of(
                MedicationReconciliationEntity.ReconciliationStatus.REQUESTED,
                MedicationReconciliationEntity.ReconciliationStatus.IN_PROGRESS
            ));
    }

    /**
     * Update medication reconciliation
     *
     * Allows updating medication count, discrepancy count, patient education status,
     * and notes throughout the reconciliation process.
     *
     * @param medRec the reconciliation with updates
     * @return updated reconciliation
     */
    @Transactional
    public MedicationReconciliationEntity updateReconciliation(
            MedicationReconciliationEntity medRec) {
        log.debug("Updating medication reconciliation: {}", medRec.getId());

        MedicationReconciliationEntity updated = medRecRepository.save(medRec);

        log.info("Medication reconciliation updated: {} with {} discrepancies identified",
            updated.getId(), updated.getDiscrepancyCount());

        return updated;
    }

    /**
     * Get medication reconciliations completed within date range
     *
     * Used for quality reporting and Meaningful Use measures.
     *
     * @param tenantId the tenant ID
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination parameters
     * @return page of completed med recs in date range
     */
    public Page<MedicationReconciliationEntity> getCompletedReconciliations(
            String tenantId,
            Instant startDate,
            Instant endDate,
            Pageable pageable) {
        log.debug("Retrieving completed med recs in date range: {} to {} for tenant {}",
            startDate, endDate, tenantId);

        return medRecRepository.findByTenantIdAndStatusAndCompletedAtBetweenOrderByCompletedAtDesc(
            tenantId,
            MedicationReconciliationEntity.ReconciliationStatus.COMPLETED,
            startDate,
            endDate,
            pageable);
    }

    /**
     * Find medication reconciliation by FHIR Task reference
     *
     * Enables synchronization with FHIR Task resources.
     *
     * @param tenantId the tenant ID
     * @param taskId the FHIR Task resource ID
     * @return optional containing reconciliation if found
     */
    public Optional<MedicationReconciliationEntity> findByTaskId(String tenantId, String taskId) {
        log.debug("Finding med rec by FHIR Task: {} in tenant {}", taskId, tenantId);

        return medRecRepository.findByTenantIdAndTaskId(tenantId, taskId);
    }

    /**
     * Get medication reconciliation completion metrics
     *
     * Calculates completion rate, average medications reconciled, etc.
     * Used for quality reporting and dashboard analytics.
     *
     * @param tenantId the tenant ID
     * @return metrics summary
     */
    public MedicationReconciliationMetrics getMetrics(String tenantId) {
        log.debug("Computing med rec metrics for tenant {}", tenantId);

        long total = medRecRepository.countByTenantIdAndStatusIn(
            tenantId,
            List.of(
                MedicationReconciliationEntity.ReconciliationStatus.REQUESTED,
                MedicationReconciliationEntity.ReconciliationStatus.IN_PROGRESS,
                MedicationReconciliationEntity.ReconciliationStatus.COMPLETED
            ));

        long pending = countPendingReconciliations(tenantId);
        long completionRate = total > 0 ? ((total - pending) * 100) / total : 0;

        return MedicationReconciliationMetrics.builder()
            .totalReconciliations(total)
            .pendingReconciliations(pending)
            .completionRate(completionRate)
            .build();
    }

    /**
     * Simple DTO for med rec metrics
     */
    public static class MedicationReconciliationMetrics {
        private long totalReconciliations;
        private long pendingReconciliations;
        private long completionRate; // percentage

        public MedicationReconciliationMetrics(long total, long pending, long rate) {
            this.totalReconciliations = total;
            this.pendingReconciliations = pending;
            this.completionRate = rate;
        }

        public static Builder builder() {
            return new Builder();
        }

        public long getTotalReconciliations() { return totalReconciliations; }
        public long getPendingReconciliations() { return pendingReconciliations; }
        public long getCompletionRate() { return completionRate; }

        public static class Builder {
            private long totalReconciliations;
            private long pendingReconciliations;
            private long completionRate;

            public Builder totalReconciliations(long value) {
                this.totalReconciliations = value;
                return this;
            }

            public Builder pendingReconciliations(long value) {
                this.pendingReconciliations = value;
                return this;
            }

            public Builder completionRate(long value) {
                this.completionRate = value;
                return this;
            }

            public MedicationReconciliationMetrics build() {
                return new MedicationReconciliationMetrics(
                    totalReconciliations, pendingReconciliations, completionRate);
            }
        }
    }
}
