package com.healthdata.quality.service;

import com.healthdata.quality.persistence.PatientMeasureAssignmentEntity;
import com.healthdata.quality.persistence.PatientMeasureAssignmentRepository;
import com.healthdata.quality.persistence.PatientMeasureEligibilityCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Measure Assignment Service
 * Manages patient-specific measure assignments (manual and automatic).
 *
 * Features:
 * - Manual assignment by care coordinators
 * - Automatic assignment based on eligibility rules
 * - Assignment lifecycle management (activate, deactivate)
 * - Effective date management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeasureAssignmentService {

    private final PatientMeasureAssignmentRepository assignmentRepository;
    private final PatientMeasureEligibilityCacheRepository cacheRepository;

    /**
     * Get all active measure assignments for a patient
     */
    public List<PatientMeasureAssignmentEntity> getActiveAssignments(String tenantId, UUID patientId) {
        log.debug("Fetching active measure assignments for patient {} in tenant {}", patientId, tenantId);
        return assignmentRepository.findActiveByPatient(tenantId, patientId);
    }

    /**
     * Get effective measure assignments for a patient on a specific date
     */
    public List<PatientMeasureAssignmentEntity> getEffectiveAssignments(
            String tenantId,
            UUID patientId,
            LocalDate date) {
        log.debug("Fetching effective measure assignments for patient {} on {}", patientId, date);
        return assignmentRepository.findEffectiveAssignments(tenantId, patientId, date);
    }

    /**
     * Manually assign a measure to a patient
     */
    @Transactional
    public PatientMeasureAssignmentEntity assignMeasure(
            String tenantId,
            UUID patientId,
            UUID measureId,
            UUID assignedBy,
            String assignmentReason,
            LocalDate effectiveFrom,
            LocalDate effectiveUntil,
            Map<String, Object> eligibilityCriteria) {

        // Check for existing active assignment
        var existing = assignmentRepository.findActiveByPatientAndMeasure(tenantId, patientId, measureId);
        if (existing.isPresent()) {
            log.warn("Active assignment already exists for patient {} and measure {}", patientId, measureId);
            throw new IllegalStateException("Measure is already assigned to this patient");
        }

        PatientMeasureAssignmentEntity assignment = PatientMeasureAssignmentEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId(measureId)
                .assignedBy(assignedBy)
                .assignmentReason(assignmentReason)
                .active(true)
                .effectiveFrom(effectiveFrom != null ? effectiveFrom : LocalDate.now())
                .effectiveUntil(effectiveUntil)
                .eligibilityCriteria(eligibilityCriteria)
                .autoAssigned(false)
                .createdBy(assignedBy)
                .build();

        assignment = assignmentRepository.save(assignment);

        log.info("Manually assigned measure {} to patient {} by user {} in tenant {}",
                measureId, patientId, assignedBy, tenantId);

        // Invalidate eligibility cache
        cacheRepository.invalidateByPatientAndMeasure(tenantId, patientId, measureId);

        return assignment;
    }

    /**
     * Automatically assign measures based on eligibility rules
     */
    @Transactional
    public List<PatientMeasureAssignmentEntity> autoAssignMeasures(
            String tenantId,
            UUID patientId,
            List<UUID> measureIds,
            Map<String, Object> eligibilityCriteria) {

        log.info("Auto-assigning {} measures to patient {} in tenant {}", measureIds.size(), patientId, tenantId);

        List<PatientMeasureAssignmentEntity> assignments = measureIds.stream()
                .filter(measureId -> assignmentRepository
                        .findActiveByPatientAndMeasure(tenantId, patientId, measureId)
                        .isEmpty())
                .map(measureId -> PatientMeasureAssignmentEntity.builder()
                        .tenantId(tenantId)
                        .patientId(patientId)
                        .measureId(measureId)
                        .assignmentReason("Automatic assignment based on eligibility criteria")
                        .active(true)
                        .effectiveFrom(LocalDate.now())
                        .eligibilityCriteria(eligibilityCriteria)
                        .autoAssigned(true)
                        .createdBy(UUID.fromString("00000000-0000-0000-0000-000000000000")) // System user
                        .build())
                .toList();

        assignments = assignmentRepository.saveAll(assignments);

        log.info("Successfully auto-assigned {} measures to patient {}", assignments.size(), patientId);

        // Invalidate cache for all assigned measures
        assignments.forEach(assignment ->
                cacheRepository.invalidateByPatientAndMeasure(tenantId, patientId, assignment.getMeasureId()));

        return assignments;
    }

    /**
     * Deactivate a measure assignment
     */
    @Transactional
    public PatientMeasureAssignmentEntity deactivateAssignment(
            String tenantId,
            UUID assignmentId,
            UUID deactivatedBy) {

        PatientMeasureAssignmentEntity assignment = assignmentRepository
                .findByIdAndTenantId(assignmentId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        if (!assignment.getActive()) {
            log.warn("Assignment {} is already inactive", assignmentId);
            return assignment;
        }

        assignment.setActive(false);
        assignment.setEffectiveUntil(LocalDate.now());
        assignment.setUpdatedAt(OffsetDateTime.now());

        assignment = assignmentRepository.save(assignment);

        log.info("Deactivated assignment {} for patient {} by user {}",
                assignmentId, assignment.getPatientId(), deactivatedBy);

        // Invalidate eligibility cache
        cacheRepository.invalidateByPatientAndMeasure(
                tenantId, assignment.getPatientId(), assignment.getMeasureId());

        return assignment;
    }

    /**
     * Update assignment effective dates
     */
    @Transactional
    public PatientMeasureAssignmentEntity updateEffectiveDates(
            String tenantId,
            UUID assignmentId,
            LocalDate effectiveFrom,
            LocalDate effectiveUntil) {

        PatientMeasureAssignmentEntity assignment = assignmentRepository
                .findByIdAndTenantId(assignmentId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        assignment.setEffectiveFrom(effectiveFrom);
        assignment.setEffectiveUntil(effectiveUntil);
        assignment.setUpdatedAt(OffsetDateTime.now());

        assignment = assignmentRepository.save(assignment);

        log.info("Updated effective dates for assignment {} to {}-{}",
                assignmentId, effectiveFrom, effectiveUntil);

        return assignment;
    }

    /**
     * Count active assignments for a patient
     */
    public long countActiveAssignments(String tenantId, UUID patientId) {
        return assignmentRepository.countActiveByPatient(tenantId, patientId);
    }

    /**
     * Get all auto-assigned measures
     */
    public List<PatientMeasureAssignmentEntity> getAutoAssignedMeasures(String tenantId) {
        return assignmentRepository.findByAutoAssigned(tenantId, true);
    }

    /**
     * Get all manually assigned measures
     */
    public List<PatientMeasureAssignmentEntity> getManuallyAssignedMeasures(String tenantId) {
        return assignmentRepository.findByAutoAssigned(tenantId, false);
    }
}
