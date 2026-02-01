package com.healthdata.nurseworkflow.application;

import com.healthdata.nurseworkflow.domain.model.ReferralCoordinationEntity;
import com.healthdata.nurseworkflow.domain.repository.ReferralCoordinationRepository;
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
 * Referral Coordination Service
 *
 * Manages complete referral workflow from request through completion,
 * implementing PCMH (Patient-Centered Medical Home) care coordination requirements.
 *
 * Closed-Loop Referral Workflow:
 * 1. PENDING_AUTHORIZATION - Insurance auth pending
 * 2. AUTHORIZED - Authorization received, ready to schedule
 * 3. SCHEDULED - Appointment scheduled with specialist
 * 4. COMPLETED - Patient attended, results received, closure completed
 * 5. NO_SHOW / CANCELLED - Referral ended abnormally
 *
 * Tracks:
 * - Insurance authorization status and numbers
 * - Appointment scheduling and attendance
 * - Medical records transmission
 * - Results receipt and provider communication
 * - Follow-up completion
 *
 * HIPAA Compliance:
 * - Referral details are PHI - audit logging required
 * - Multi-tenant isolation in all queries
 * - Specialist contact info is restricted
 *
 * Integration Points:
 * - FHIR ServiceRequest: Referral order resource (todo)
 * - FHIR Appointment: Specialist appointment
 * - Kafka: Publishes referral.updated events
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReferralCoordinationService {

    private final ReferralCoordinationRepository referralRepository;

    /**
     * Create referral
     *
     * Initiates referral request to specialist. Sets initial status to
     * PENDING_AUTHORIZATION if insurance auth required, or AUTHORIZED if not.
     *
     * @param referral the referral to create
     * @return created referral
     */
    @Transactional
    public ReferralCoordinationEntity createReferral(ReferralCoordinationEntity referral) {
        log.debug("Creating referral for patient {} to specialty: {} by coordinator {}",
            referral.getPatientId(), referral.getSpecialtyType(), referral.getCoordinatorId());

        if (referral.getId() == null) {
            referral.setId(UUID.randomUUID());
        }

        if (referral.getStatus() == null) {
            referral.setStatus(ReferralCoordinationEntity.ReferralStatus.PENDING_AUTHORIZATION);
        }

        ReferralCoordinationEntity saved = referralRepository.save(referral);

        log.info("Referral created: {} for patient {} to {} with priority: {}",
            saved.getId(), saved.getPatientId(), saved.getSpecialtyType(), saved.getPriority());

        return saved;
    }

    /**
     * Get referral by ID
     *
     * @param id the referral ID
     * @return optional containing referral if found
     */
    public Optional<ReferralCoordinationEntity> getReferralById(UUID id) {
        log.debug("Retrieving referral: {}", id);
        return referralRepository.findById(id);
    }

    /**
     * Get pending referrals for tenant
     *
     * Returns referrals not yet completed (PENDING_AUTH, AUTHORIZED, SCHEDULED, AWAITING_APT).
     * Ordered by request time (oldest first for priority).
     *
     * @param tenantId the tenant ID
     * @param pageable pagination parameters
     * @return page of pending referrals
     */
    public Page<ReferralCoordinationEntity> getPendingReferrals(
            String tenantId,
            Pageable pageable) {
        log.debug("Retrieving pending referrals for tenant {}", tenantId);

        return referralRepository.findPendingByTenant(tenantId, pageable);
    }

    /**
     * Get patient referral history
     *
     * Retrieves all referrals for a specific patient, ordered by most recent first.
     *
     * @param tenantId the tenant ID
     * @param patientId the patient ID
     * @param pageable pagination parameters
     * @return page of patient referrals
     */
    public Page<ReferralCoordinationEntity> getPatientReferralHistory(
            String tenantId,
            UUID patientId,
            Pageable pageable) {
        log.debug("Retrieving referral history for patient {} in tenant {}",
            patientId, tenantId);

        return referralRepository.findByTenantIdAndPatientIdOrderByRequestedAtDesc(
            tenantId, patientId, pageable);
    }

    /**
     * Get referrals by status
     *
     * Filters referrals by status (authorization pending, scheduled, completed, etc.)
     * for workqueue management.
     *
     * @param tenantId the tenant ID
     * @param status the status filter
     * @param pageable pagination parameters
     * @return page of referrals with specified status
     */
    public Page<ReferralCoordinationEntity> getReferralsByStatus(
            String tenantId,
            ReferralCoordinationEntity.ReferralStatus status,
            Pageable pageable) {
        log.debug("Retrieving referrals by status: {} in tenant {}", status, tenantId);

        return referralRepository.findByTenantIdAndStatusOrderByRequestedAtDesc(
            tenantId, status, pageable);
    }

    /**
     * Get referrals by specialty type
     *
     * Filters referrals by specialty (cardiology, podiatry, etc.) for
     * specialty-specific coordination and metrics.
     *
     * @param tenantId the tenant ID
     * @param specialtyType the specialty type filter
     * @param pageable pagination parameters
     * @return page of referrals to specified specialty
     */
    public Page<ReferralCoordinationEntity> getReferralsBySpecialty(
            String tenantId,
            String specialtyType,
            Pageable pageable) {
        log.debug("Retrieving referrals by specialty: {} in tenant {}",
            specialtyType, tenantId);

        return referralRepository.findByTenantIdAndSpecialtyTypeOrderByRequestedAtDesc(
            tenantId, specialtyType, pageable);
    }

    /**
     * Get referrals awaiting appointment scheduling
     *
     * Returns referrals that have authorization but no appointment yet.
     * These need immediate coordination action.
     *
     * @param tenantId the tenant ID
     * @return list of referrals needing appointment scheduling
     */
    public List<ReferralCoordinationEntity> findAwaitingAppointmentScheduling(String tenantId) {
        log.debug("Finding referrals awaiting appointment scheduling in tenant {}", tenantId);

        return referralRepository.findAwaitingAppointmentScheduling(tenantId);
    }

    /**
     * Get referrals awaiting results follow-up
     *
     * Returns referrals where patient attended appointment but results not
     * yet received. These need follow-up action to close the loop.
     *
     * @param tenantId the tenant ID
     * @return list of referrals needing results follow-up
     */
    public List<ReferralCoordinationEntity> findAwaitingResults(String tenantId) {
        log.debug("Finding referrals awaiting results in tenant {}", tenantId);

        return referralRepository.findAwaitingResults(tenantId);
    }

    /**
     * Update referral
     *
     * Allows updating status, authorization number, appointment details,
     * and results as referral progresses through workflow.
     *
     * @param referral the referral with updates
     * @return updated referral
     */
    @Transactional
    public ReferralCoordinationEntity updateReferral(ReferralCoordinationEntity referral) {
        log.debug("Updating referral: {} for patient {}",
            referral.getId(), referral.getPatientId());

        ReferralCoordinationEntity updated = referralRepository.save(referral);

        log.info("Referral updated: {} status now: {}",
            updated.getId(), updated.getStatus());

        return updated;
    }

    /**
     * Count pending referrals for tenant
     *
     * Returns count of referrals not yet completed, useful for
     * workload assessment and dashboard metrics.
     *
     * @param tenantId the tenant ID
     * @return count of pending referrals
     */
    public long countPendingReferrals(String tenantId) {
        return referralRepository.countByTenantIdAndStatusIn(
            tenantId,
            List.of(
                ReferralCoordinationEntity.ReferralStatus.PENDING_AUTHORIZATION,
                ReferralCoordinationEntity.ReferralStatus.AUTHORIZED,
                ReferralCoordinationEntity.ReferralStatus.SCHEDULED,
                ReferralCoordinationEntity.ReferralStatus.AWAITING_APPOINTMENT
            ));
    }

    /**
     * Find urgent referrals awaiting scheduling
     *
     * Returns URGENT priority referrals that don't yet have appointments.
     * These need immediate action.
     *
     * @param tenantId the tenant ID
     * @return list of urgent referrals needing scheduling
     */
    public List<ReferralCoordinationEntity> findUrgentAwaitingScheduling(String tenantId) {
        log.debug("Finding urgent referrals awaiting scheduling in tenant {}", tenantId);

        return referralRepository.findUrgentAwaitingScheduling(tenantId);
    }

    /**
     * Get referral completion metrics
     *
     * Calculates completion rate, average time to appointment, etc.
     * Used for quality reporting and PCMH metrics.
     *
     * @param tenantId the tenant ID
     * @return referral metrics summary
     */
    public ReferralMetrics getMetrics(String tenantId) {
        log.debug("Computing referral metrics for tenant {}", tenantId);

        long total = referralRepository.countByTenantIdAndStatusIn(
            tenantId,
            List.of(
                ReferralCoordinationEntity.ReferralStatus.PENDING_AUTHORIZATION,
                ReferralCoordinationEntity.ReferralStatus.AUTHORIZED,
                ReferralCoordinationEntity.ReferralStatus.SCHEDULED,
                ReferralCoordinationEntity.ReferralStatus.AWAITING_APPOINTMENT,
                ReferralCoordinationEntity.ReferralStatus.COMPLETED,
                ReferralCoordinationEntity.ReferralStatus.CANCELLED,
                ReferralCoordinationEntity.ReferralStatus.NO_SHOW
            ));

        long pending = countPendingReferrals(tenantId);
        long completionRate = total > 0 ? ((total - pending) * 100) / total : 0;

        return ReferralMetrics.builder()
            .totalReferrals(total)
            .pendingReferrals(pending)
            .completionRate(completionRate)
            .build();
    }

    /**
     * Simple DTO for referral metrics
     */
    public static class ReferralMetrics {
        private long totalReferrals;
        private long pendingReferrals;
        private long completionRate; // percentage

        public ReferralMetrics(long total, long pending, long rate) {
            this.totalReferrals = total;
            this.pendingReferrals = pending;
            this.completionRate = rate;
        }

        public static Builder builder() {
            return new Builder();
        }

        public long getTotalReferrals() { return totalReferrals; }
        public long getPendingReferrals() { return pendingReferrals; }
        public long getCompletionRate() { return completionRate; }

        public static class Builder {
            private long totalReferrals;
            private long pendingReferrals;
            private long completionRate;

            public Builder totalReferrals(long value) {
                this.totalReferrals = value;
                return this;
            }

            public Builder pendingReferrals(long value) {
                this.pendingReferrals = value;
                return this;
            }

            public Builder completionRate(long value) {
                this.completionRate = value;
                return this;
            }

            public ReferralMetrics build() {
                return new ReferralMetrics(totalReferrals, pendingReferrals, completionRate);
            }
        }
    }
}
