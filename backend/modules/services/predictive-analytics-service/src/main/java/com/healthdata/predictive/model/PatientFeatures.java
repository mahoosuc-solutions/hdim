package com.healthdata.predictive.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ML features extracted from patient data
 * Demographics, diagnoses, utilization, labs, medications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientFeatures {

    /**
     * Patient identifier
     */
    private String patientId;

    /**
     * Tenant identifier
     */
    private String tenantId;

    // Demographics
    private Integer age;
    private String gender;
    private String race;
    private String ethnicity;

    // Clinical history
    private Integer charlsonComorbidityIndex;
    private List<String> activeDiagnoses;
    private List<String> chronicConditions;
    private Integer activeConditionCount;

    // Utilization
    private Integer hospitalizationsPastYear;
    private Integer edVisitsPast6Months;
    private Integer outpatientVisitsPast6Months;
    private Integer lengthOfStayLastAdmission;
    private String lastAdmissionAcuity;
    private LocalDate lastDischargeDate;

    // Medications
    private Integer activeMedicationCount;
    private List<String> activeMedications;
    private Integer medicationChangesPast30Days;
    private Integer highRiskMedicationCount;

    // Labs (most recent values)
    private Double hemoglobinA1c;
    private Double serumCreatinine;
    private Double egfr;
    private Double ldlCholesterol;
    private Double bloodPressureSystolic;
    private Double bloodPressureDiastolic;
    private Double bmi;

    // Social determinants
    private Integer socialRiskScore;
    private Boolean hasHomeHealthServices;
    private Boolean hasSupportSystem;

    // Behavioral
    private Boolean smokingStatus;
    private Boolean alcoholUse;

    // Raw feature vector for ML model input
    private double[] featureVector;

    // Feature names corresponding to feature vector
    private List<String> featureNames;

    // Additional metadata
    private Map<String, Object> metadata;

    /**
     * Get feature vector dimension
     */
    public int getFeatureVectorDimension() {
        return featureVector != null ? featureVector.length : 0;
    }
}
