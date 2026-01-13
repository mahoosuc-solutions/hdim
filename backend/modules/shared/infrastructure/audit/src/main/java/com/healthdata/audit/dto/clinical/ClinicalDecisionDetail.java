package com.healthdata.audit.dto.clinical;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive clinical decision detail
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalDecisionDetail {
    
    private String decisionId;
    private String decisionType;
    private String reviewStatus;
    private LocalDateTime decisionTimestamp;
    
    // Patient context
    private PatientContext patientContext;
    
    // Clinical recommendation
    private ClinicalRecommendation recommendation;
    
    // Supporting evidence
    private List<ClinicalEvidence> evidence;
    
    // Drug interactions (if medication alert)
    private List<DrugInteraction> drugInteractions;
    
    // Care gaps (if care gap alert)
    private List<CareGap> careGaps;
    
    // Risk factors (if risk stratification)
    private RiskAssessment riskAssessment;
    
    // Review history
    private List<ReviewAction> reviewHistory;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientContext {
        private String patientId;
        private String patientName;
        private Integer age;
        private String gender;
        private List<String> activeConditions;
        private List<String> currentMedications;
        private List<String> allergies;
        private String riskLevel;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClinicalRecommendation {
        private String recommendationType;
        private String recommendationText;
        private String urgency; // IMMEDIATE, URGENT, ROUTINE
        private String evidenceGrade;
        private Double confidenceScore;
        private String specialty;
        private Map<String, Object> actionItems;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClinicalEvidence {
        private String evidenceId;
        private String evidenceType; // GUIDELINE, STUDY, CLINICAL_TRIAL, EXPERT_CONSENSUS
        private String evidenceGrade;
        private String citation;
        private String summary;
        private String relevanceScore;
        private LocalDateTime publishedDate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrugInteraction {
        private String drugA;
        private String drugB;
        private String interactionSeverity;
        private String interactionType;
        private String clinicalEffect;
        private String managementRecommendation;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CareGap {
        private String gapType; // SCREENING_OVERDUE, PREVENTIVE_CARE, FOLLOW_UP_NEEDED
        private String serviceDescription;
        private LocalDateTime dueDate;
        private Integer daysPastDue;
        private String guidelineReference;
        private String priority;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskAssessment {
        private String overallRiskLevel;
        private Double riskScore;
        private List<RiskFactor> riskFactors;
        private List<String> mitigationStrategies;
        private String assessmentModel;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskFactor {
        private String factorName;
        private String factorValue;
        private String contribution; // HIGH, MODERATE, LOW
        private Boolean modifiable;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewAction {
        private String actionType; // APPROVED, REJECTED, OVERRIDE_APPLIED, NOTES_ADDED
        private String reviewedBy;
        private LocalDateTime reviewedAt;
        private String notes;
        private Map<String, Object> actionDetails;
    }
}
