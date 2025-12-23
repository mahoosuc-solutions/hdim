package com.healthdata.quality.service;

import com.healthdata.quality.persistence.CareGapEntity;
import com.healthdata.quality.persistence.RiskAssessmentEntity;
import com.healthdata.quality.persistence.RiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Care Gap Prioritization Service
 * Determines priority and due dates for care gaps based on patient risk and gap category
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CareGapPrioritizationService {

    private final RiskAssessmentRepository riskAssessmentRepository;

    /**
     * Determine priority for a care gap based on:
     * - Patient risk level
     * - Gap category
     * - Measure type
     */
    public CareGapEntity.Priority determinePriority(
        String tenantId,
        UUID patientId,
        String measureId,
        CareGapEntity.GapCategory category
    ) {
        // Get patient risk assessment
        Optional<RiskAssessmentEntity> riskAssessmentOpt =
            riskAssessmentRepository.findLatestByTenantIdAndPatientId(tenantId, patientId);

        // Mental health gaps are always elevated to URGENT
        if (category == CareGapEntity.GapCategory.MENTAL_HEALTH) {
            log.debug("Mental health gap detected - elevating to URGENT priority");
            return CareGapEntity.Priority.URGENT;
        }

        // If no risk assessment, default to HIGH (conservative approach)
        if (riskAssessmentOpt.isEmpty()) {
            log.debug("No risk assessment found for patient {} - defaulting to HIGH priority", patientId);
            return CareGapEntity.Priority.HIGH;
        }

        RiskAssessmentEntity riskAssessment = riskAssessmentOpt.get();
        RiskAssessmentEntity.RiskLevel riskLevel = riskAssessment.getRiskLevel();

        // Determine priority based on risk level and category
        return determinePriorityFromRiskAndCategory(riskLevel, category, riskAssessment);
    }

    /**
     * Determine priority from risk level and category
     */
    private CareGapEntity.Priority determinePriorityFromRiskAndCategory(
        RiskAssessmentEntity.RiskLevel riskLevel,
        CareGapEntity.GapCategory category,
        RiskAssessmentEntity riskAssessment
    ) {
        // High/Very High risk patients
        if (riskLevel == RiskAssessmentEntity.RiskLevel.HIGH ||
            riskLevel == RiskAssessmentEntity.RiskLevel.VERY_HIGH) {

            // Chronic disease gaps for high-risk patients = URGENT
            if (category == CareGapEntity.GapCategory.CHRONIC_DISEASE) {
                return CareGapEntity.Priority.URGENT;
            }

            // Medication gaps for high-risk patients = URGENT
            if (category == CareGapEntity.GapCategory.MEDICATION) {
                return CareGapEntity.Priority.URGENT;
            }

            // Other gaps for high-risk patients = HIGH
            return CareGapEntity.Priority.HIGH;
        }

        // Medium/Moderate risk patients
        if (riskLevel == RiskAssessmentEntity.RiskLevel.MODERATE) {

            // Chronic disease gaps = HIGH
            if (category == CareGapEntity.GapCategory.CHRONIC_DISEASE) {
                return CareGapEntity.Priority.HIGH;
            }

            // Medication gaps for patients with chronic conditions = HIGH
            if (category == CareGapEntity.GapCategory.MEDICATION &&
                riskAssessment.getChronicConditionCount() != null &&
                riskAssessment.getChronicConditionCount() > 0) {
                return CareGapEntity.Priority.HIGH;
            }

            // Other gaps = MEDIUM
            return CareGapEntity.Priority.MEDIUM;
        }

        // Low risk patients
        // Preventive care = MEDIUM
        if (category == CareGapEntity.GapCategory.PREVENTIVE_CARE ||
            category == CareGapEntity.GapCategory.SCREENING) {
            return CareGapEntity.Priority.MEDIUM;
        }

        // Other gaps for low risk = LOW
        return CareGapEntity.Priority.LOW;
    }

    /**
     * Calculate due date based on priority
     * - URGENT: 7 days
     * - HIGH: 14 days
     * - MEDIUM: 30 days
     * - LOW: 90 days
     */
    public Instant calculateDueDate(CareGapEntity.Priority priority) {
        Instant now = Instant.now();

        return switch (priority) {
            case URGENT -> now.plus(7, ChronoUnit.DAYS);
            case HIGH -> now.plus(14, ChronoUnit.DAYS);
            case MEDIUM -> now.plus(30, ChronoUnit.DAYS);
            case LOW -> now.plus(90, ChronoUnit.DAYS);
        };
    }

    /**
     * Calculate due date based on measure periodicity
     * Some measures have specific reporting periods
     */
    public Instant calculateDueDateForMeasure(
        String measureId,
        CareGapEntity.Priority priority
    ) {
        // Use measure-specific logic if needed
        // For now, use standard priority-based calculation
        return calculateDueDate(priority);
    }
}
