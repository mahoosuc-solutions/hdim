package com.healthdata.payer.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * State-specific Medicaid configuration and requirements.
 * Each state has different reporting requirements and quality measures.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicaidStateConfig {

    /**
     * State code (e.g., NY, CA, TX, FL)
     */
    private String stateCode;

    /**
     * State name
     */
    private String stateName;

    /**
     * Medicaid program name (varies by state)
     */
    private String programName;

    /**
     * Required HEDIS measures for this state
     */
    private List<String> requiredMeasures;

    /**
     * State-specific quality thresholds (measure code -> threshold)
     */
    private Map<String, Double> qualityThresholds;

    /**
     * Reporting frequency (MONTHLY, QUARTERLY, ANNUAL)
     */
    private ReportingFrequency reportingFrequency;

    /**
     * Whether state requires NCQA accreditation
     */
    private boolean ncqaAccreditationRequired;

    /**
     * State-specific performance goals
     */
    private Map<String, Double> performanceGoals;

    /**
     * Penalty thresholds for non-compliance
     */
    private Map<String, Double> penaltyThresholds;

    /**
     * Quality bonus payment eligibility criteria
     */
    private Map<String, Object> bonusCriteria;

    /**
     * Additional state-specific requirements
     */
    private Map<String, Object> additionalRequirements;

    /**
     * Effective date for this configuration
     */
    private String effectiveDate;

    /**
     * Configuration version
     */
    private String version;

    public enum ReportingFrequency {
        MONTHLY, QUARTERLY, ANNUAL
    }

    /**
     * Predefined configurations for common states
     */
    public static class StateConfigs {

        public static MedicaidStateConfig newYork() {
            return MedicaidStateConfig.builder()
                .stateCode("NY")
                .stateName("New York")
                .programName("New York Medicaid Managed Care")
                .requiredMeasures(List.of(
                    "CBP", "CDC-H9", "CDC-E", "BCS", "COL", "W30", "AAP", "CIS",
                    "AMR", "FUH-7", "FUH-30", "ADD", "AMB", "PCR"
                ))
                .reportingFrequency(ReportingFrequency.QUARTERLY)
                .ncqaAccreditationRequired(true)
                .qualityThresholds(Map.of(
                    "CBP", 0.65,
                    "CDC-H9", 0.75,
                    "BCS", 0.70,
                    "COL", 0.60
                ))
                .performanceGoals(Map.of(
                    "CBP", 0.70,
                    "CDC-H9", 0.80,
                    "BCS", 0.75,
                    "COL", 0.65
                ))
                .version("2024.1")
                .build();
        }

        public static MedicaidStateConfig california() {
            return MedicaidStateConfig.builder()
                .stateCode("CA")
                .stateName("California")
                .programName("Medi-Cal Managed Care")
                .requiredMeasures(List.of(
                    "CBP", "CDC-H9", "BCS", "COL", "CCS", "W30", "AAP", "CIS",
                    "IMA", "WCV", "AMR", "FUH-7", "ADD"
                ))
                .reportingFrequency(ReportingFrequency.ANNUAL)
                .ncqaAccreditationRequired(true)
                .qualityThresholds(Map.of(
                    "CBP", 0.60,
                    "CDC-H9", 0.70,
                    "BCS", 0.65,
                    "COL", 0.55
                ))
                .performanceGoals(Map.of(
                    "CBP", 0.68,
                    "CDC-H9", 0.78,
                    "BCS", 0.73,
                    "COL", 0.63
                ))
                .version("2024.1")
                .build();
        }

        public static MedicaidStateConfig texas() {
            return MedicaidStateConfig.builder()
                .stateCode("TX")
                .stateName("Texas")
                .programName("Texas STAR Medicaid")
                .requiredMeasures(List.of(
                    "CBP", "CDC-H9", "BCS", "COL", "W30", "AAP", "CIS",
                    "WCV", "AMR", "ADD", "AMB"
                ))
                .reportingFrequency(ReportingFrequency.ANNUAL)
                .ncqaAccreditationRequired(false)
                .qualityThresholds(Map.of(
                    "CBP", 0.58,
                    "CDC-H9", 0.68,
                    "BCS", 0.62,
                    "W30", 0.65
                ))
                .performanceGoals(Map.of(
                    "CBP", 0.65,
                    "CDC-H9", 0.75,
                    "BCS", 0.70,
                    "W30", 0.72
                ))
                .version("2024.1")
                .build();
        }

        public static MedicaidStateConfig florida() {
            return MedicaidStateConfig.builder()
                .stateCode("FL")
                .stateName("Florida")
                .programName("Florida Medicaid Managed Care")
                .requiredMeasures(List.of(
                    "CBP", "CDC-H9", "BCS", "COL", "W30", "AAP", "CIS",
                    "WCV", "AMR", "FUH-7", "ADD"
                ))
                .reportingFrequency(ReportingFrequency.QUARTERLY)
                .ncqaAccreditationRequired(true)
                .qualityThresholds(Map.of(
                    "CBP", 0.62,
                    "CDC-H9", 0.72,
                    "BCS", 0.68,
                    "W30", 0.70
                ))
                .performanceGoals(Map.of(
                    "CBP", 0.70,
                    "CDC-H9", 0.80,
                    "BCS", 0.75,
                    "W30", 0.78
                ))
                .version("2024.1")
                .build();
        }
    }
}
