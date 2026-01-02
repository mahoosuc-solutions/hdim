package com.healthdata.sdoh.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Impact of SDOH on health outcomes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SdohImpact {
    private String impactId;
    private String patientId;
    private SdohCategory category;
    private ImpactLevel impactLevel;
    private Map<String, Object> healthOutcomeMetrics;
    private String description;
    private Double predictedHospitalizationRisk;
    private Double predictedEmergencyVisitRisk;
    private Double predictedMedicationAdherenceImpact;

    public enum ImpactLevel {
        MINIMAL,
        LOW,
        MODERATE,
        HIGH,
        SEVERE
    }
}
