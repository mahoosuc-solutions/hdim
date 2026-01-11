package com.healthdata.quality.service;

import com.healthdata.quality.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Measure Override Service
 * Manages patient-specific measure parameter overrides with clinical justification.
 *
 * Features:
 * - Override creation with required clinical justification (HIPAA compliance)
 * - Multi-level override resolution (patient override > profile > base measure)
 * - Approval workflow support
 * - Periodic review scheduling
 * - Override conflict detection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeasureOverrideService {

    private final PatientMeasureOverrideRepository overrideRepository;
    private final MeasureConfigProfileRepository profileRepository;
    private final PatientProfileAssignmentRepository profileAssignmentRepository;
    private final PatientMeasureEligibilityCacheRepository cacheRepository;

    /**
     * Get active overrides for a patient and measure
     */
    public List<PatientMeasureOverrideEntity> getActiveOverrides(
            String tenantId,
            UUID patientId,
            UUID measureId) {
        log.debug("Fetching active overrides for patient {} and measure {}", patientId, measureId);
        return overrideRepository.findActiveByPatientAndMeasure(tenantId, patientId, measureId);
    }

    /**
     * Get effective overrides for a patient and measure on a specific date
     */
    public List<PatientMeasureOverrideEntity> getEffectiveOverrides(
            String tenantId,
            UUID patientId,
            UUID measureId,
            LocalDate date) {
        log.debug("Fetching effective overrides for patient {} and measure {} on {}", patientId, measureId, date);
        return overrideRepository.findEffectiveOverrides(tenantId, patientId, measureId, date);
    }

    /**
     * Create a new patient measure override
     * HIPAA Compliance: Requires clinical justification
     */
    @Transactional
    public PatientMeasureOverrideEntity createOverride(
            String tenantId,
            UUID patientId,
            UUID measureId,
            String overrideType,
            String overrideField,
            String originalValue,
            String overrideValue,
            String valueType,
            String clinicalReason,
            Map<String, Object> supportingEvidence,
            UUID createdBy,
            LocalDate effectiveFrom,
            LocalDate effectiveUntil,
            Boolean requiresApproval) {

        // Validate clinical reason is provided (HIPAA requirement)
        if (clinicalReason == null || clinicalReason.isBlank()) {
            throw new IllegalArgumentException("Clinical reason is required for all patient overrides (HIPAA compliance)");
        }

        // Check for conflicts with existing overrides
        List<PatientMeasureOverrideEntity> existing = overrideRepository.findEffectiveOverrides(
                tenantId, patientId, measureId, effectiveFrom != null ? effectiveFrom : LocalDate.now());

        boolean hasConflict = existing.stream()
                .anyMatch(o -> o.getOverrideField().equals(overrideField));

        if (hasConflict) {
            log.warn("Override conflict detected for patient {} measure {} field {}", patientId, measureId, overrideField);
            throw new IllegalStateException(
                    "Conflicting override already exists for field: " + overrideField +
                    ". Deactivate existing override first.");
        }

        PatientMeasureOverrideEntity override = PatientMeasureOverrideEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId(measureId)
                .overrideType(overrideType)
                .overrideField(overrideField)
                .originalValue(originalValue)
                .overrideValue(overrideValue)
                .valueType(valueType != null ? valueType : "TEXT")
                .clinicalReason(clinicalReason)
                .supportingEvidence(supportingEvidence)
                .active(true)
                .effectiveFrom(effectiveFrom != null ? effectiveFrom : LocalDate.now())
                .effectiveUntil(effectiveUntil)
                .requiresPeriodicReview(true)
                .reviewFrequencyDays(90) // Default 90-day review
                .createdBy(createdBy)
                .build();

        // Set approval status if required
        if (requiresApproval != null && !requiresApproval) {
            override.setApprovedBy(createdBy);
            override.setApprovedAt(OffsetDateTime.now());
        }

        override = overrideRepository.save(override);

        log.info("Created override {} for patient {} measure {} field {} in tenant {}",
                override.getId(), patientId, measureId, overrideField, tenantId);

        // Invalidate eligibility cache
        cacheRepository.invalidateByPatientAndMeasure(tenantId, patientId, measureId);

        return override;
    }

    /**
     * Approve a pending override
     */
    @Transactional
    public PatientMeasureOverrideEntity approveOverride(
            String tenantId,
            UUID overrideId,
            UUID approvedBy,
            String approvalNotes) {

        PatientMeasureOverrideEntity override = overrideRepository
                .findByIdAndTenantId(overrideId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Override not found: " + overrideId));

        if (override.getApprovedBy() != null) {
            log.warn("Override {} is already approved", overrideId);
            return override;
        }

        override.setApprovedBy(approvedBy);
        override.setApprovedAt(OffsetDateTime.now());
        override.setUpdatedAt(OffsetDateTime.now());

        override = overrideRepository.save(override);

        log.info("Override {} approved by user {} in tenant {}", overrideId, approvedBy, tenantId);

        return override;
    }

    /**
     * Mark override as reviewed
     */
    @Transactional
    public PatientMeasureOverrideEntity markReviewed(
            String tenantId,
            UUID overrideId,
            UUID reviewedBy) {

        PatientMeasureOverrideEntity override = overrideRepository
                .findByIdAndTenantId(overrideId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Override not found: " + overrideId));

        override.setLastReviewedAt(OffsetDateTime.now());
        override.setLastReviewedBy(reviewedBy);
        // next_review_date will be auto-calculated by database trigger

        override = overrideRepository.save(override);

        log.info("Override {} reviewed by user {}", overrideId, reviewedBy);

        return override;
    }

    /**
     * Get overrides due for review
     */
    public List<PatientMeasureOverrideEntity> getOverridesDueForReview(
            String tenantId,
            LocalDate asOfDate) {
        return overrideRepository.findOverridesDueForReview(
                tenantId, asOfDate != null ? asOfDate : LocalDate.now());
    }

    /**
     * Deactivate an override
     */
    @Transactional
    public PatientMeasureOverrideEntity deactivateOverride(
            String tenantId,
            UUID overrideId,
            UUID deactivatedBy) {

        PatientMeasureOverrideEntity override = overrideRepository
                .findByIdAndTenantId(overrideId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Override not found: " + overrideId));

        override.setActive(false);
        override.setEffectiveUntil(LocalDate.now());
        override.setUpdatedAt(OffsetDateTime.now());

        override = overrideRepository.save(override);

        log.info("Deactivated override {} for patient {} by user {}",
                overrideId, override.getPatientId(), deactivatedBy);

        // Invalidate cache
        cacheRepository.invalidateByPatientAndMeasure(
                tenantId, override.getPatientId(), override.getMeasureId());

        return override;
    }

    /**
     * Resolve all applicable overrides for a patient and measure
     * Resolution order: Patient Override > Profile > Base Measure (handled by caller)
     *
     * @return Map of field names to resolved values
     */
    public Map<String, Object> resolveOverrides(
            String tenantId,
            UUID patientId,
            UUID measureId,
            LocalDate evaluationDate) {

        log.debug("Resolving overrides for patient {} measure {} on {}", patientId, measureId, evaluationDate);

        Map<String, Object> resolvedValues = new HashMap<>();

        // Step 1: Get patient-specific overrides (highest priority)
        List<PatientMeasureOverrideEntity> patientOverrides = overrideRepository.findEffectiveOverrides(
                tenantId, patientId, measureId, evaluationDate);

        for (PatientMeasureOverrideEntity override : patientOverrides) {
            Object typedValue = parseOverrideValue(override.getOverrideValue(), override.getValueType());
            resolvedValues.put(override.getOverrideField(), typedValue);
            log.trace("Applied patient override: {} = {}", override.getOverrideField(), typedValue);
        }

        // Step 2: Get profile-based overrides (medium priority)
        List<PatientProfileAssignmentEntity> profileAssignments =
                profileAssignmentRepository.findEffectiveAssignments(tenantId, patientId, evaluationDate);

        // Get all applicable profiles and sort by priority (descending)
        List<MeasureConfigProfileEntity> profiles = profileAssignments.stream()
                .map(assignment -> profileRepository.findByIdAndTenantId(assignment.getProfileId(), tenantId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(profile -> profile.getMeasureId().equals(measureId) && profile.getActive())
                .sorted(Comparator.comparing(MeasureConfigProfileEntity::getPriority).reversed())
                .collect(Collectors.toList());

        for (MeasureConfigProfileEntity profile : profiles) {
            if (profile.getConfigOverrides() != null) {
                for (Map.Entry<String, Object> entry : profile.getConfigOverrides().entrySet()) {
                    // Only apply if not already overridden by patient-specific override
                    if (!resolvedValues.containsKey(entry.getKey())) {
                        resolvedValues.put(entry.getKey(), entry.getValue());
                        log.trace("Applied profile override from {}: {} = {}",
                                profile.getProfileCode(), entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        log.info("Resolved {} override values for patient {} measure {}", resolvedValues.size(), patientId, measureId);

        return resolvedValues;
    }

    /**
     * Parse override value based on type
     */
    private Object parseOverrideValue(String value, String type) {
        if (value == null) {
            return null;
        }

        return switch (type) {
            case "NUMERIC" -> Double.parseDouble(value);
            case "DATE" -> LocalDate.parse(value);
            case "BOOLEAN" -> Boolean.parseBoolean(value);
            case "JSON" -> value; // Return as string, caller can parse if needed
            default -> value; // TEXT
        };
    }

    /**
     * Get overrides pending approval
     */
    public List<PatientMeasureOverrideEntity> getPendingApprovals(String tenantId) {
        return overrideRepository.findPendingApproval(tenantId);
    }

    /**
     * Count active overrides for a patient
     */
    public long countActiveOverrides(String tenantId, UUID patientId) {
        return overrideRepository.countActiveByPatient(tenantId, patientId);
    }
}
