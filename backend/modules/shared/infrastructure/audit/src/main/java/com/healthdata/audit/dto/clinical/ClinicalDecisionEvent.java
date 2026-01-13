package com.healthdata.audit.dto.clinical;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Clinical decision event for queue display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalDecisionEvent {
    
    private String decisionId;
    private String decisionType;
    private String patientId;
    private String patientName;
    private String alertSeverity;
    private String reviewStatus;
    private LocalDateTime decisionTimestamp;
    private String evidenceGrade;
    private Double confidenceScore;
    private String specialtyArea;
    private String clinicalRecommendation;
    private String priority; // CRITICAL, HIGH, MEDIUM, LOW
    private Boolean hasOverride;
    private String overrideReason;
    private Integer relatedAlertsCount;
}
