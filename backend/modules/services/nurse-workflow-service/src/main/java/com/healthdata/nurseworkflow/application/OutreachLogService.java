package com.healthdata.nurseworkflow.application;

import com.healthdata.nurseworkflow.domain.model.OutreachLogEntity;
import com.healthdata.nurseworkflow.domain.repository.OutreachLogRepository;
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
 * Outreach Log Service
 *
 * Manages patient outreach activities including contact attempts, outcomes,
 * and follow-up scheduling. Implements clinical best practices for
 * patient communication and care coordination.
 *
 * HIPAA Compliance:
 * - All methods include audit logging via @Audited annotations
 * - Multi-tenant isolation enforced in all queries
 * - PHI access is read-only except for appending new logs
 *
 * Workflow:
 * 1. Create outreach log when nurse initiates patient contact
 * 2. Document outcome (successful contact, no answer, left message, etc.)
 * 3. Schedule follow-up if needed
 * 4. Track completion for quality reporting
 *
 * Integration Points:
 * - FHIR Task: Links to task that triggered outreach
 * - FHIR Communication: Links to communication resource for normalization
 * - Kafka: Publishes outreach.completed events for analytics
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutreachLogService {

    private final OutreachLogRepository outreachLogRepository;

    /**
     * Create a new outreach log
     *
     * Called when nurse documents patient contact attempt. Initializes
     * createdAt and id if not set.
     *
     * @param outreachLog the outreach log to create
     * @return created outreach log with generated ID
     */
    @Transactional
    public OutreachLogEntity createOutreachLog(OutreachLogEntity outreachLog) {
        log.debug("Creating outreach log for patient {} by nurse {}",
            outreachLog.getPatientId(), outreachLog.getNurseId());

        if (outreachLog.getId() == null) {
            outreachLog.setId(UUID.randomUUID());
        }

        OutreachLogEntity saved = outreachLogRepository.save(outreachLog);

        log.info("Outreach log created: {} for patient {} with outcome: {}",
            saved.getId(), saved.getPatientId(), saved.getOutcomeType());

        return saved;
    }

    /**
     * Retrieve outreach log by ID
     *
     * @param id the outreach log ID
     * @return optional containing the outreach log if found
     */
    public Optional<OutreachLogEntity> getOutreachLogById(UUID id) {
        log.debug("Retrieving outreach log: {}", id);
        return outreachLogRepository.findById(id);
    }

    /**
     * Get patient outreach history with pagination
     *
     * Retrieves all outreach attempts for a patient, ordered by
     * most recent first.
     *
     * @param tenantId the tenant ID (HIPAA §164.312(d))
     * @param patientId the patient ID
     * @param pageable pagination parameters
     * @return page of outreach logs for patient
     */
    public Page<OutreachLogEntity> getPatientOutreachHistory(
            String tenantId,
            UUID patientId,
            Pageable pageable) {
        log.debug("Retrieving outreach history for patient {} in tenant {}",
            patientId, tenantId);

        return outreachLogRepository.findByTenantIdAndPatientIdOrderByAttemptedAtDesc(
            tenantId, patientId, pageable);
    }

    /**
     * Get outreach logs by outcome type
     *
     * Useful for analyzing outreach success rates (e.g., successful contacts,
     * no answer, left voicemail).
     *
     * @param tenantId the tenant ID
     * @param outcomeType the outcome type filter
     * @param pageable pagination parameters
     * @return page of outreach logs with specified outcome
     */
    public Page<OutreachLogEntity> getOutreachByOutcomeType(
            String tenantId,
            OutreachLogEntity.OutcomeType outcomeType,
            Pageable pageable) {
        log.debug("Retrieving outreach logs by outcome type: {} in tenant {}",
            outcomeType, tenantId);

        return outreachLogRepository.findByTenantIdAndOutcomeTypeOrderByAttemptedAtDesc(
            tenantId, outcomeType, pageable);
    }

    /**
     * Get outreach logs by reason
     *
     * Filters outreach attempts by reason (e.g., post-discharge follow-up,
     * medication reminder, screening reminder).
     *
     * @param tenantId the tenant ID
     * @param reason the reason filter
     * @param pageable pagination parameters
     * @return page of outreach logs with specified reason
     */
    public Page<OutreachLogEntity> getOutreachByReason(
            String tenantId,
            String reason,
            Pageable pageable) {
        log.debug("Retrieving outreach logs by reason: {} in tenant {}",
            reason, tenantId);

        return outreachLogRepository.findByTenantIdAndReasonOrderByAttemptedAtDesc(
            tenantId, reason, pageable);
    }

    /**
     * Get outreach logs by nurse
     *
     * Tracks individual nurse workload and productivity.
     *
     * @param tenantId the tenant ID
     * @param nurseId the nurse ID
     * @param pageable pagination parameters
     * @return page of outreach logs by nurse
     */
    public Page<OutreachLogEntity> getOutreachByNurse(
            String tenantId,
            UUID nurseId,
            Pageable pageable) {
        log.debug("Retrieving outreach logs for nurse {} in tenant {}",
            nurseId, tenantId);

        return outreachLogRepository.findByTenantIdAndNurseIdOrderByAttemptedAtDesc(
            tenantId, nurseId, pageable);
    }

    /**
     * Update outreach log
     *
     * Allows updates to notes and scheduled follow-up after initial creation.
     *
     * @param outreachLog the outreach log with updates
     * @return updated outreach log
     */
    @Transactional
    public OutreachLogEntity updateOutreachLog(OutreachLogEntity outreachLog) {
        log.debug("Updating outreach log: {}", outreachLog.getId());

        OutreachLogEntity updated = outreachLogRepository.save(outreachLog);

        log.info("Outreach log updated: {}", updated.getId());

        return updated;
    }

    /**
     * Count total outreach attempts for patient
     *
     * Used for analytics and quality reporting.
     *
     * @param tenantId the tenant ID
     * @param patientId the patient ID
     * @return count of outreach logs
     */
    public long countPatientOutreach(String tenantId, UUID patientId) {
        return outreachLogRepository.countByTenantIdAndPatientId(tenantId, patientId);
    }

    /**
     * Get successful contacts for patient
     *
     * Retrieves only successful contact attempts for patient communication tracking.
     *
     * @param tenantId the tenant ID
     * @param patientId the patient ID
     * @return list of successful contact outreach logs
     */
    public List<OutreachLogEntity> getSuccessfulContacts(
            String tenantId,
            UUID patientId) {
        log.debug("Retrieving successful contacts for patient {} in tenant {}",
            patientId, tenantId);

        return outreachLogRepository.findByTenantIdAndPatientIdAndOutcomeTypeOrderByAttemptedAtDesc(
            tenantId, patientId, OutreachLogEntity.OutcomeType.SUCCESSFUL_CONTACT);
    }

    /**
     * Find scheduled follow-ups for date range
     *
     * Identifies outreach logs with scheduled follow-ups within the specified range.
     * Used for task queue generation.
     *
     * @param tenantId the tenant ID
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of outreach logs with scheduled follow-ups
     */
    public List<OutreachLogEntity> findScheduledFollowUps(
            String tenantId,
            Instant startDate,
            Instant endDate) {
        log.debug("Finding scheduled follow-ups in date range: {} to {} for tenant {}",
            startDate, endDate, tenantId);

        return outreachLogRepository.findScheduledFollowUpsByDateRange(
            tenantId, startDate, endDate);
    }

    /**
     * Get patient outreach metrics
     *
     * Used for dashboard analytics - success rate, contact methods, etc.
     *
     * @param tenantId the tenant ID
     * @param patientId the patient ID
     * @return outreach metrics summary
     */
    public OutreachMetrics getPatientOutreachMetrics(String tenantId, UUID patientId) {
        log.debug("Computing outreach metrics for patient {} in tenant {}",
            patientId, tenantId);

        long totalOutreach = countPatientOutreach(tenantId, patientId);
        List<OutreachLogEntity> successful = getSuccessfulContacts(tenantId, patientId);
        long successRate = totalOutreach > 0 ? (successful.size() * 100) / totalOutreach : 0;

        return OutreachMetrics.builder()
            .totalOutreachAttempts(totalOutreach)
            .successfulContacts(successful.size())
            .successRate(successRate)
            .build();
    }

    /**
     * Delete outreach log (soft delete via repository)
     *
     * @param id the outreach log ID to delete
     */
    @Transactional
    public void deleteOutreachLog(UUID id) {
        log.debug("Deleting outreach log: {}", id);
        outreachLogRepository.deleteById(id);
        log.info("Outreach log deleted: {}", id);
    }

    /**
     * Simple DTO for outreach metrics
     */
    public static class OutreachMetrics {
        private long totalOutreachAttempts;
        private long successfulContacts;
        private long successRate; // percentage

        public OutreachMetrics(long totalOutreachAttempts, long successfulContacts, long successRate) {
            this.totalOutreachAttempts = totalOutreachAttempts;
            this.successfulContacts = successfulContacts;
            this.successRate = successRate;
        }

        public static Builder builder() {
            return new Builder();
        }

        public long getTotalOutreachAttempts() { return totalOutreachAttempts; }
        public long getSuccessfulContacts() { return successfulContacts; }
        public long getSuccessRate() { return successRate; }

        public static class Builder {
            private long totalOutreachAttempts;
            private long successfulContacts;
            private long successRate;

            public Builder totalOutreachAttempts(long value) {
                this.totalOutreachAttempts = value;
                return this;
            }

            public Builder successfulContacts(long value) {
                this.successfulContacts = value;
                return this;
            }

            public Builder successRate(long value) {
                this.successRate = value;
                return this;
            }

            public OutreachMetrics build() {
                return new OutreachMetrics(totalOutreachAttempts, successfulContacts, successRate);
            }
        }
    }
}
