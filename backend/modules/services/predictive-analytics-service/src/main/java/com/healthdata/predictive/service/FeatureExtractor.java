package com.healthdata.predictive.service;

import com.healthdata.predictive.model.PatientFeatures;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Feature extraction service for ML models
 * Extracts and normalizes features from patient data
 */
@Service
@Slf4j
public class FeatureExtractor {

    // Feature vector indices
    private static final int FEATURE_AGE = 0;
    private static final int FEATURE_GENDER = 1;
    private static final int FEATURE_CCI = 2;
    private static final int FEATURE_ACTIVE_CONDITIONS = 3;
    private static final int FEATURE_HOSP_PAST_YEAR = 4;
    private static final int FEATURE_ED_VISITS = 5;
    private static final int FEATURE_OUTPATIENT_VISITS = 6;
    private static final int FEATURE_ACTIVE_MEDS = 7;
    private static final int FEATURE_MED_CHANGES = 8;
    private static final int FEATURE_HBA1C = 9;
    private static final int FEATURE_CREATININE = 10;
    private static final int FEATURE_EGFR = 11;
    private static final int FEATURE_BP_SYSTOLIC = 12;
    private static final int FEATURE_BP_DIASTOLIC = 13;
    private static final int FEATURE_BMI = 14;
    private static final int FEATURE_SOCIAL_RISK = 15;
    private static final int FEATURE_LOS = 16;

    private static final int NUM_FEATURES = 17;

    private static final List<String> FEATURE_NAMES = Arrays.asList(
        "age", "gender", "charlson_comorbidity_index", "active_conditions",
        "hospitalizations_past_year", "ed_visits_past_6m", "outpatient_visits_past_6m",
        "active_medications", "medication_changes_30d", "hemoglobin_a1c",
        "serum_creatinine", "egfr", "bp_systolic", "bp_diastolic", "bmi",
        "social_risk_score", "length_of_stay"
    );

    // Charlson Comorbidity Index conditions
    private static final Map<String, Integer> CHARLSON_WEIGHTS = new HashMap<>();
    static {
        CHARLSON_WEIGHTS.put("myocardial-infarction", 1);
        CHARLSON_WEIGHTS.put("congestive-heart-failure", 1);
        CHARLSON_WEIGHTS.put("peripheral-vascular-disease", 1);
        CHARLSON_WEIGHTS.put("cerebrovascular-disease", 1);
        CHARLSON_WEIGHTS.put("dementia", 1);
        CHARLSON_WEIGHTS.put("copd", 1);
        CHARLSON_WEIGHTS.put("rheumatologic-disease", 1);
        CHARLSON_WEIGHTS.put("peptic-ulcer-disease", 1);
        CHARLSON_WEIGHTS.put("mild-liver-disease", 1);
        CHARLSON_WEIGHTS.put("diabetes", 1);
        CHARLSON_WEIGHTS.put("diabetes-with-complications", 2);
        CHARLSON_WEIGHTS.put("hemiplegia", 2);
        CHARLSON_WEIGHTS.put("renal-disease", 2);
        CHARLSON_WEIGHTS.put("cancer", 2);
        CHARLSON_WEIGHTS.put("moderate-severe-liver-disease", 3);
        CHARLSON_WEIGHTS.put("metastatic-solid-tumor", 6);
        CHARLSON_WEIGHTS.put("aids", 6);
    }

    /**
     * Extract features from patient data
     */
    public PatientFeatures extractFeatures(String tenantId, String patientId, Map<String, Object> patientData) {
        log.debug("Extracting features for patient: {}, tenant: {}", patientId, tenantId);

        // Validation
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        if (patientId == null || patientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Patient ID cannot be null or empty");
        }
        if (patientData == null) {
            throw new IllegalArgumentException("Patient data cannot be null");
        }

        // Build features
        PatientFeatures.PatientFeaturesBuilder builder = PatientFeatures.builder()
            .patientId(patientId)
            .tenantId(tenantId);

        // Demographics
        builder.age(getIntValue(patientData, "age"));
        builder.gender(getStringValue(patientData, "gender"));
        builder.race(getStringValue(patientData, "race"));
        builder.ethnicity(getStringValue(patientData, "ethnicity"));

        // Chronic conditions
        List<String> chronicConditions = getListValue(patientData, "chronicConditions");
        builder.chronicConditions(chronicConditions);
        builder.activeConditionCount(chronicConditions != null ? chronicConditions.size() : 0);

        List<String> activeDiagnoses = getListValue(patientData, "activeDiagnoses");
        builder.activeDiagnoses(activeDiagnoses);

        // Calculate Charlson Comorbidity Index
        int cci = calculateCharlsonIndex(chronicConditions, getIntValue(patientData, "age"));
        builder.charlsonComorbidityIndex(cci);

        // Utilization
        builder.hospitalizationsPastYear(getIntValue(patientData, "hospitalizationsPastYear"));
        builder.edVisitsPast6Months(getIntValue(patientData, "edVisitsPast6Months"));
        builder.outpatientVisitsPast6Months(getIntValue(patientData, "outpatientVisitsPast6Months"));
        builder.lengthOfStayLastAdmission(getIntValue(patientData, "lengthOfStay"));
        builder.lastAdmissionAcuity(getStringValue(patientData, "lastAdmissionAcuity"));

        // Medications
        List<String> activeMedications = getListValue(patientData, "activeMedications");
        builder.activeMedications(activeMedications);
        builder.activeMedicationCount(activeMedications != null ? activeMedications.size() : 0);
        builder.medicationChangesPast30Days(getIntValue(patientData, "medicationChangesPast30Days"));
        builder.highRiskMedicationCount(getIntValue(patientData, "highRiskMedicationCount"));

        // Labs
        builder.hemoglobinA1c(getDoubleValue(patientData, "hemoglobinA1c"));
        builder.serumCreatinine(getDoubleValue(patientData, "serumCreatinine"));
        builder.egfr(getDoubleValue(patientData, "egfr"));
        builder.ldlCholesterol(getDoubleValue(patientData, "ldlCholesterol"));

        // Vitals
        builder.bloodPressureSystolic(getDoubleValue(patientData, "bloodPressureSystolic"));
        builder.bloodPressureDiastolic(getDoubleValue(patientData, "bloodPressureDiastolic"));
        builder.bmi(getDoubleValue(patientData, "bmi"));

        // Social determinants
        builder.socialRiskScore(getIntValue(patientData, "socialRiskScore"));
        builder.hasHomeHealthServices(getBooleanValue(patientData, "hasHomeHealthServices"));
        builder.hasSupportSystem(getBooleanValue(patientData, "hasSupportSystem"));

        // Behavioral
        builder.smokingStatus(getBooleanValue(patientData, "smokingStatus"));
        builder.alcoholUse(getBooleanValue(patientData, "alcoholUse"));

        PatientFeatures features = builder.build();

        // Generate feature vector
        double[] featureVector = generateFeatureVector(features);
        features.setFeatureVector(featureVector);
        features.setFeatureNames(FEATURE_NAMES);

        log.debug("Extracted {} features for patient: {}", featureVector.length, patientId);

        return features;
    }

    /**
     * Generate feature vector from patient features
     */
    private double[] generateFeatureVector(PatientFeatures features) {
        double[] vector = new double[NUM_FEATURES];

        vector[FEATURE_AGE] = features.getAge() != null ? features.getAge() : 0.0;
        vector[FEATURE_GENDER] = encodeGender(features.getGender());
        vector[FEATURE_CCI] = features.getCharlsonComorbidityIndex() != null ? features.getCharlsonComorbidityIndex() : 0.0;
        vector[FEATURE_ACTIVE_CONDITIONS] = features.getActiveConditionCount() != null ? features.getActiveConditionCount() : 0.0;
        vector[FEATURE_HOSP_PAST_YEAR] = features.getHospitalizationsPastYear() != null ? features.getHospitalizationsPastYear() : 0.0;
        vector[FEATURE_ED_VISITS] = features.getEdVisitsPast6Months() != null ? features.getEdVisitsPast6Months() : 0.0;
        vector[FEATURE_OUTPATIENT_VISITS] = features.getOutpatientVisitsPast6Months() != null ? features.getOutpatientVisitsPast6Months() : 0.0;
        vector[FEATURE_ACTIVE_MEDS] = features.getActiveMedicationCount() != null ? features.getActiveMedicationCount() : 0.0;
        vector[FEATURE_MED_CHANGES] = features.getMedicationChangesPast30Days() != null ? features.getMedicationChangesPast30Days() : 0.0;
        vector[FEATURE_HBA1C] = features.getHemoglobinA1c() != null ? features.getHemoglobinA1c() : 0.0;
        vector[FEATURE_CREATININE] = features.getSerumCreatinine() != null ? features.getSerumCreatinine() : 0.0;
        vector[FEATURE_EGFR] = features.getEgfr() != null ? features.getEgfr() : 0.0;
        vector[FEATURE_BP_SYSTOLIC] = features.getBloodPressureSystolic() != null ? features.getBloodPressureSystolic() : 0.0;
        vector[FEATURE_BP_DIASTOLIC] = features.getBloodPressureDiastolic() != null ? features.getBloodPressureDiastolic() : 0.0;
        vector[FEATURE_BMI] = features.getBmi() != null ? features.getBmi() : 0.0;
        vector[FEATURE_SOCIAL_RISK] = features.getSocialRiskScore() != null ? features.getSocialRiskScore() : 0.0;
        vector[FEATURE_LOS] = features.getLengthOfStayLastAdmission() != null ? features.getLengthOfStayLastAdmission() : 0.0;

        return vector;
    }

    /**
     * Normalize feature vector using min-max normalization
     */
    public double[] normalizeFeatures(double[] features) {
        if (features == null) {
            throw new IllegalArgumentException("Feature vector cannot be null");
        }

        double[] normalized = new double[features.length];

        // Normalization ranges (based on typical clinical values)
        double[] maxValues = {
            120.0,  // age
            1.0,    // gender
            15.0,   // CCI
            20.0,   // active conditions
            10.0,   // hospitalizations
            20.0,   // ED visits
            50.0,   // outpatient visits
            30.0,   // active meds
            10.0,   // med changes
            15.0,   // HbA1c
            5.0,    // creatinine
            120.0,  // eGFR
            200.0,  // BP systolic
            120.0,  // BP diastolic
            50.0,   // BMI
            10.0,   // social risk
            30.0    // LOS
        };

        for (int i = 0; i < features.length && i < maxValues.length; i++) {
            if (maxValues[i] > 0) {
                normalized[i] = features[i] / maxValues[i];
                // Clip to [0, 1] range
                normalized[i] = Math.max(0.0, Math.min(1.0, normalized[i]));
            } else {
                normalized[i] = features[i];
            }
        }

        return normalized;
    }

    /**
     * Calculate Charlson Comorbidity Index
     */
    private int calculateCharlsonIndex(List<String> conditions, Integer age) {
        int score = 0;

        if (conditions != null) {
            for (String condition : conditions) {
                String normalizedCondition = condition.toLowerCase().trim();
                score += CHARLSON_WEIGHTS.getOrDefault(normalizedCondition, 0);
            }
        }

        // Add age adjustment
        if (age != null) {
            if (age >= 50 && age < 60) {
                score += 1;
            } else if (age >= 60 && age < 70) {
                score += 2;
            } else if (age >= 70 && age < 80) {
                score += 3;
            } else if (age >= 80) {
                score += 4;
            }
        }

        return score;
    }

    /**
     * Encode gender as numeric value
     */
    private double encodeGender(String gender) {
        if (gender == null) {
            return 0.0;
        }
        switch (gender.toLowerCase()) {
            case "male":
                return 1.0;
            case "female":
                return 0.0;
            default:
                return 0.5; // Other/unknown
        }
    }

    // Helper methods for safe data extraction

    private Integer getIntValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private Double getDoubleValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    private Boolean getBooleanValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    @SuppressWarnings("unchecked")
    private List<String> getListValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            return ((List<?>) value).stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        }
        return null;
    }
}
