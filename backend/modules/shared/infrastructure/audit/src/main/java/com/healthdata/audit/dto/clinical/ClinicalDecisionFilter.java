package com.healthdata.audit.dto.clinical;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Filter criteria for clinical decision review history
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalDecisionFilter {
    
    private String decisionType; // MEDICATION_ALERT, CARE_GAP, RISK_STRATIFICATION, CLINICAL_PATHWAY
    private String alertSeverity; // CRITICAL, HIGH, MODERATE, LOW
    private String reviewStatus; // PENDING, APPROVED, REJECTED, NEEDS_REVISION
    private LocalDate startDate;
    private LocalDate endDate;
    private String patientRiskLevel; // HIGH, MEDIUM, LOW
    private String evidenceGrade; // A, B, C, D
    private Boolean hasOverride;
    private String specialtyArea; // CARDIOLOGY, ENDOCRINOLOGY, ONCOLOGY, etc.
}
