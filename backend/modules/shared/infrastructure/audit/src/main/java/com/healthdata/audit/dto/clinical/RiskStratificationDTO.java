package com.healthdata.audit.dto.clinical;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Risk stratification information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskStratificationDTO {
    
    private String stratificationId;
    private String patientId;
    private String riskCategory; // CARDIOVASCULAR, DIABETES, FALL_RISK, READMISSION
    private String overallRiskLevel;
    private Double riskScore;
    private List<RiskFactorDTO> contributingFactors;
    private String assessmentModel;
    private String evidenceGrade;
    private List<String> recommendedInterventions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskFactorDTO {
        private String factorName;
        private String factorValue;
        private String contribution;
        private Boolean modifiable;
    }
}
