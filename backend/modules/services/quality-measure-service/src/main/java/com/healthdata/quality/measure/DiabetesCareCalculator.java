package com.healthdata.quality.measure;

import com.healthdata.quality.model.*;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HEDIS Comprehensive Diabetes Care (CDC) Measure Calculator
 *
 * Evaluates 6 sub-measures for diabetic patients:
 * 1. HbA1c Testing
 * 2. HbA1c < 8% (good control)
 * 3. HbA1c > 9% (poor control - inverse measure)
 * 4. Eye Exam (diabetic retinopathy screening)
 * 5. Nephropathy Screening
 * 6. BP Control < 140/90
 *
 * HEDIS Specification: CDC-E (2024)
 *
 * Based on JavaScript implementation from hedis-dashboard/diabetes-care.js
 */
@Component
public class DiabetesCareCalculator implements MeasureCalculator {

    private static final String MEASURE_ID = "CDC";
    private static final String MEASURE_NAME = "Comprehensive Diabetes Care";
    private static final String VERSION = "2024";

    // SNOMED CT codes for diabetes diagnosis
    private static final List<String> DIABETES_CODES = Arrays.asList(
        "44054006",  // Type 2 diabetes mellitus
        "73211009",  // Type 1 diabetes mellitus
        "8801005",   // Secondary diabetes mellitus
        "46635009"   // Diabetes mellitus type 1
    );

    private static final String GESTATIONAL_DIABETES_CODE = "11687002";

    // Exclusion codes
    private static final String HOSPICE_CODE = "305336008";
    private static final String PALLIATIVE_CARE_CODE = "385763009";

    // LOINC codes for lab tests
    private static final String HBA1C_LOINC = "4548-4";
    private static final String BP_PANEL_LOINC = "85354-9";
    private static final String SYSTOLIC_BP_LOINC = "8480-6";
    private static final String DIASTOLIC_BP_LOINC = "8462-4";

    // SNOMED codes for procedures
    private static final List<String> EYE_EXAM_CODES = Arrays.asList(
        "252779009",  // Diabetic retinopathy screening
        "410551004",  // Diabetic retinal exam
        "308110009"   // Ophthalmological examination
    );

    private static final List<String> EYE_EXAM_LOINC_CODES = Arrays.asList(
        "32451-7",  // Physical findings of Eye
        "70939-2"   // Diabetic eye exam
    );

    // LOINC codes for nephropathy screening
    private static final List<String> NEPHROPATHY_LOINC_CODES = Arrays.asList(
        "33914-3",  // Microalbumin Creatinine Ratio
        "14958-3",  // Microalbumin
        "30000-4",  // Urine microalbumin
        "14959-1"   // Urine microalbumin/creatinine ratio
    );

    // RxNorm codes for ACE/ARB medications
    private static final List<String> ACE_ARB_MEDICATION_CODES = Arrays.asList(
        "316049",  // Lisinopril
        "316151",  // Enalapril
        "69749",   // Losartan
        "83818"    // Valsartan
    );

    @Override
    public MeasureResult calculate(PatientData patientData) {
        MeasureResult result = MeasureResult.builder()
            .measureId(MEASURE_ID)
            .measureName(MEASURE_NAME)
            .patientId(patientData.getPatient() != null ? patientData.getPatient().getId() : null)
            .build();

        // Check eligibility (has diabetes diagnosis)
        DiagnosisCheck diabetesCheck = checkDiabetesDiagnosis(patientData);
        if (!diabetesCheck.hasCondition()) {
            result.setExclusionReason(diabetesCheck.getExclusionReason() != null
                ? diabetesCheck.getExclusionReason()
                : "No diabetes diagnosis");
            return result;
        }

        // Check for exclusions (hospice, palliative care)
        ExclusionCheck exclusionCheck = checkExclusions(patientData);
        if (exclusionCheck.isExcluded()) {
            result.setDenominatorExclusion(true);
            result.setExclusionReason(exclusionCheck.getReason());
            return result;
        }

        // Patient is eligible
        result.setEligible(true);
        result.setDenominatorMembership(true);

        // Calculate all sub-measures
        Map<String, SubMeasureResult> subMeasures = calculateSubMeasures(patientData);
        result.setSubMeasures(subMeasures);

        // Identify care gaps
        List<CareGap> careGaps = identifyCareGaps(subMeasures, patientData);
        result.setCareGaps(careGaps);

        // Generate clinical recommendations
        List<Recommendation> recommendations = generateRecommendations(subMeasures, careGaps);
        result.setRecommendations(recommendations);

        return result;
    }

    /**
     * Check if patient has a diabetes diagnosis
     */
    private DiagnosisCheck checkDiabetesDiagnosis(PatientData patientData) {
        List<Condition> conditions = patientData.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return new DiagnosisCheck(false, "No conditions found");
        }

        // Check for gestational diabetes only
        boolean hasGestational = conditions.stream()
            .anyMatch(c -> hasCode(c.getCode(), GESTATIONAL_DIABETES_CODE));

        boolean hasOtherDiabetes = conditions.stream()
            .anyMatch(c -> hasCodes(c.getCode(), DIABETES_CODES));

        if (hasGestational && !hasOtherDiabetes) {
            return new DiagnosisCheck(false, "Gestational diabetes only");
        }

        return new DiagnosisCheck(hasOtherDiabetes, null);
    }

    /**
     * Check for exclusion criteria
     */
    private ExclusionCheck checkExclusions(PatientData patientData) {
        // Check for hospice encounters
        boolean inHospice = patientData.getEncounters().stream()
            .anyMatch(enc -> enc.getType().stream()
                .anyMatch(type -> hasCode(type, HOSPICE_CODE) ||
                    hasDisplayText(type, "hospice")));

        if (inHospice) {
            return new ExclusionCheck(true, "Patient in hospice care");
        }

        // Check for palliative care
        boolean hasPalliativeCare = patientData.getConditions().stream()
            .anyMatch(c -> hasCode(c.getCode(), PALLIATIVE_CARE_CODE) ||
                hasDisplayText(c.getCode(), "palliative"));

        if (hasPalliativeCare) {
            return new ExclusionCheck(true, "Patient receiving palliative care");
        }

        return new ExclusionCheck(false, null);
    }

    /**
     * Calculate all sub-measures
     */
    private Map<String, SubMeasureResult> calculateSubMeasures(PatientData patientData) {
        Map<String, SubMeasureResult> subMeasures = new HashMap<>();
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);

        // HbA1c Testing and Control
        HbA1cResults hba1c = calculateHbA1cMeasures(patientData, oneYearAgo);
        subMeasures.put("HbA1c Testing", hba1c.testing);
        subMeasures.put("HbA1c < 8%", hba1c.control8);
        subMeasures.put("HbA1c > 9%", hba1c.poorControl);

        // Eye Exam
        subMeasures.put("Eye Exam", calculateEyeExam(patientData, oneYearAgo));

        // Nephropathy Screening
        subMeasures.put("Nephropathy Screening", calculateNephropathy(patientData, oneYearAgo));

        // BP Control
        subMeasures.put("BP Control < 140/90", calculateBPControl(patientData));

        return subMeasures;
    }

    /**
     * Calculate HbA1c sub-measures
     */
    private HbA1cResults calculateHbA1cMeasures(PatientData patientData, LocalDate oneYearAgo) {
        List<Observation> hba1cObs = patientData.getObservations().stream()
            .filter(obs -> hasCode(obs.getCode(), HBA1C_LOINC))
            .filter(obs -> getObservationDate(obs).isAfter(oneYearAgo))
            .sorted(Comparator.comparing(this::getObservationDate).reversed())
            .collect(Collectors.toList());

        if (hba1cObs.isEmpty()) {
            return new HbA1cResults(
                SubMeasureResult.builder().numeratorMembership(false).build(),
                SubMeasureResult.builder().numeratorMembership(false).build(),
                SubMeasureResult.builder().numeratorMembership(false).build()
            );
        }

        Observation mostRecent = hba1cObs.get(0);
        Double value = getNumericValue(mostRecent);

        if (value == null) {
            return new HbA1cResults(
                SubMeasureResult.builder().numeratorMembership(false).build(),
                SubMeasureResult.builder().numeratorMembership(false).build(),
                SubMeasureResult.builder().numeratorMembership(false).build()
            );
        }

        return new HbA1cResults(
            SubMeasureResult.builder()
                .numeratorMembership(true)
                .numericValue(value)
                .value(String.format("%.1f%%", value))
                .date(getObservationDate(mostRecent))
                .build(),
            SubMeasureResult.builder()
                .numeratorMembership(value < 8.0)
                .numericValue(value)
                .value(String.format("%.1f%%", value))
                .build(),
            SubMeasureResult.builder()
                .numeratorMembership(value > 9.0)
                .numericValue(value)
                .value(String.format("%.1f%%", value))
                .build()
        );
    }

    /**
     * Calculate eye exam sub-measure
     */
    private SubMeasureResult calculateEyeExam(PatientData patientData, LocalDate oneYearAgo) {
        // Check procedures
        boolean hasEyeProcedure = patientData.getProcedures().stream()
            .anyMatch(proc -> hasCodes(proc.getCode(), EYE_EXAM_CODES) &&
                getProcedureDate(proc).isAfter(oneYearAgo));

        // Check observations
        boolean hasEyeObservation = patientData.getObservations().stream()
            .anyMatch(obs -> hasCodes(obs.getCode(), EYE_EXAM_LOINC_CODES) &&
                getObservationDate(obs).isAfter(oneYearAgo));

        return SubMeasureResult.builder()
            .numeratorMembership(hasEyeProcedure || hasEyeObservation)
            .method(hasEyeProcedure ? "procedure" : (hasEyeObservation ? "observation" : "none"))
            .build();
    }

    /**
     * Calculate nephropathy screening sub-measure
     */
    private SubMeasureResult calculateNephropathy(PatientData patientData, LocalDate oneYearAgo) {
        // Check for nephropathy screening
        boolean hasScreening = patientData.getObservations().stream()
            .anyMatch(obs -> hasCodes(obs.getCode(), NEPHROPATHY_LOINC_CODES) &&
                getObservationDate(obs).isAfter(oneYearAgo));

        // Check for ACE/ARB medications (evidence of treatment)
        boolean hasACEARB = patientData.getMedicationStatements().stream()
            .anyMatch(med -> hasCodes(med.getMedicationCodeableConcept(), ACE_ARB_MEDICATION_CODES) &&
                "active".equals(med.getStatus().toCode()));

        return SubMeasureResult.builder()
            .numeratorMembership(hasScreening || hasACEARB)
            .method(hasScreening ? "screening" : (hasACEARB ? "treatment" : "none"))
            .build();
    }

    /**
     * Calculate blood pressure control sub-measure
     */
    private SubMeasureResult calculateBPControl(PatientData patientData) {
        List<Observation> bpObs = patientData.getObservations().stream()
            .filter(obs -> hasCode(obs.getCode(), BP_PANEL_LOINC))
            .sorted(Comparator.comparing(this::getObservationDate).reversed())
            .collect(Collectors.toList());

        if (bpObs.isEmpty()) {
            return SubMeasureResult.builder().numeratorMembership(false).build();
        }

        Observation mostRecent = bpObs.get(0);

        // Extract systolic and diastolic components
        Double systolic = getComponentValue(mostRecent, SYSTOLIC_BP_LOINC);
        Double diastolic = getComponentValue(mostRecent, DIASTOLIC_BP_LOINC);

        if (systolic == null || diastolic == null) {
            return SubMeasureResult.builder().numeratorMembership(false).build();
        }

        boolean isControlled = systolic < 140 && diastolic < 90;

        return SubMeasureResult.builder()
            .numeratorMembership(isControlled)
            .value(String.format("%.0f/%.0f", systolic, diastolic))
            .numericValue(systolic) // Store systolic for reference
            .metadata(String.format("Diastolic: %.0f", diastolic))
            .build();
    }

    /**
     * Identify care gaps based on sub-measure results
     */
    private List<CareGap> identifyCareGaps(Map<String, SubMeasureResult> subMeasures, PatientData patientData) {
        List<CareGap> gaps = new ArrayList<>();

        // HbA1c Testing Gap
        if (!subMeasures.get("HbA1c Testing").isNumeratorMembership()) {
            gaps.add(CareGap.builder()
                .type("missing-hba1c-test")
                .description("No HbA1c test in the past 12 months")
                .severity("high")
                .action("Schedule HbA1c test immediately")
                .measureComponent("HbA1c Testing")
                .build());
        }

        // Eye Exam Gap
        if (!subMeasures.get("Eye Exam").isNumeratorMembership()) {
            gaps.add(CareGap.builder()
                .type("missing-eye-exam")
                .description("No diabetic eye exam in the past 12 months")
                .severity("high")
                .action("Schedule diabetic retinopathy screening")
                .measureComponent("Eye Exam")
                .build());
        }

        // Nephropathy Screening Gap
        if (!subMeasures.get("Nephropathy Screening").isNumeratorMembership()) {
            gaps.add(CareGap.builder()
                .type("missing-nephropathy-screening")
                .description("No nephropathy screening or ACE/ARB therapy")
                .severity("medium")
                .action("Order urine microalbumin test or consider ACE/ARB therapy")
                .measureComponent("Nephropathy Screening")
                .build());
        }

        // BP Control Gap
        SubMeasureResult bpResult = subMeasures.get("BP Control < 140/90");
        if (!bpResult.isNumeratorMembership() && bpResult.getValue() != null) {
            gaps.add(CareGap.builder()
                .type("uncontrolled-blood-pressure")
                .description(String.format("Blood pressure %s exceeds target", bpResult.getValue()))
                .severity("high")
                .action("Adjust antihypertensive therapy")
                .measureComponent("BP Control")
                .build());
        } else if (bpResult.getValue() == null) {
            gaps.add(CareGap.builder()
                .type("missing-bp-reading")
                .description("No recent blood pressure reading")
                .severity("medium")
                .action("Check blood pressure at next visit")
                .measureComponent("BP Control")
                .build());
        }

        return gaps;
    }

    /**
     * Generate clinical recommendations
     */
    private List<Recommendation> generateRecommendations(Map<String, SubMeasureResult> subMeasures, List<CareGap> careGaps) {
        List<Recommendation> recommendations = new ArrayList<>();

        // Poor HbA1c control (> 9%)
        if (subMeasures.get("HbA1c > 9%").isNumeratorMembership()) {
            recommendations.add(Recommendation.builder()
                .priority("high")
                .action("Consider intensive medication management")
                .rationale("HbA1c > 9% indicates poor glycemic control")
                .category("medication")
                .build());

            recommendations.add(Recommendation.builder()
                .priority("high")
                .action("Refer to diabetes educator")
                .rationale("Patient education can improve self-management")
                .category("referral")
                .build());
        }
        // Moderate control (8-9%)
        else if (subMeasures.get("HbA1c Testing").getNumericValue() != null &&
                 subMeasures.get("HbA1c Testing").getNumericValue() >= 8.0 &&
                 subMeasures.get("HbA1c Testing").getNumericValue() < 9.0) {
            recommendations.add(Recommendation.builder()
                .priority("medium")
                .action("Consider medication adjustment")
                .rationale("HbA1c 8-9% may benefit from therapy intensification")
                .category("medication")
                .build());
        }

        // Multiple care gaps
        if (careGaps.size() > 2) {
            recommendations.add(Recommendation.builder()
                .priority("high")
                .action("Schedule comprehensive diabetes care visit")
                .rationale(String.format("%d care gaps identified", careGaps.size()))
                .category("visit")
                .build());
        }

        // Lifestyle modifications
        if (subMeasures.get("HbA1c Testing").getNumericValue() != null &&
            subMeasures.get("HbA1c Testing").getNumericValue() > 7.0) {
            recommendations.add(Recommendation.builder()
                .priority("medium")
                .action("Reinforce lifestyle modifications")
                .rationale("Diet and exercise remain cornerstone of diabetes management")
                .category("lifestyle")
                .build());
        }

        return recommendations;
    }

    // ========== Utility Methods ==========

    private boolean hasCode(CodeableConcept concept, String code) {
        if (concept == null || concept.getCoding() == null) return false;
        return concept.getCoding().stream()
            .anyMatch(c -> code.equals(c.getCode()));
    }

    private boolean hasCodes(CodeableConcept concept, List<String> codes) {
        if (concept == null || concept.getCoding() == null) return false;
        return concept.getCoding().stream()
            .anyMatch(c -> codes.contains(c.getCode()));
    }

    private boolean hasDisplayText(CodeableConcept concept, String searchText) {
        if (concept == null || concept.getCoding() == null) return false;
        return concept.getCoding().stream()
            .anyMatch(c -> c.getDisplay() != null &&
                c.getDisplay().toLowerCase().contains(searchText.toLowerCase()));
    }

    private LocalDate getObservationDate(Observation obs) {
        if (obs.getEffectiveDateTimeType() != null && obs.getEffectiveDateTimeType().getValue() != null) {
            return obs.getEffectiveDateTimeType().getValue().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.MIN;
    }

    private LocalDate getProcedureDate(Procedure proc) {
        if (proc.getPerformedDateTimeType() != null && proc.getPerformedDateTimeType().getValue() != null) {
            return proc.getPerformedDateTimeType().getValue().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (proc.getPerformedPeriod() != null && proc.getPerformedPeriod().getStart() != null) {
            return proc.getPerformedPeriod().getStart().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.MIN;
    }

    private Double getNumericValue(Observation obs) {
        if (obs.getValueQuantity() != null) {
            return obs.getValueQuantity().getValue().doubleValue();
        }
        return null;
    }

    private Double getComponentValue(Observation obs, String componentCode) {
        if (obs.getComponent() == null) return null;
        return obs.getComponent().stream()
            .filter(comp -> hasCode(comp.getCode(), componentCode))
            .map(comp -> comp.getValueQuantity() != null ?
                comp.getValueQuantity().getValue().doubleValue() : null)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    @Override
    public String getMeasureId() {
        return MEASURE_ID;
    }

    @Override
    public String getMeasureName() {
        return MEASURE_NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    // ========== Inner Classes ==========

    private static class DiagnosisCheck {
        private final boolean hasCondition;
        private final String exclusionReason;

        DiagnosisCheck(boolean hasCondition, String exclusionReason) {
            this.hasCondition = hasCondition;
            this.exclusionReason = exclusionReason;
        }

        boolean hasCondition() { return hasCondition; }
        String getExclusionReason() { return exclusionReason; }
    }

    private static class ExclusionCheck {
        private final boolean isExcluded;
        private final String reason;

        ExclusionCheck(boolean isExcluded, String reason) {
            this.isExcluded = isExcluded;
            this.reason = reason;
        }

        boolean isExcluded() { return isExcluded; }
        String getReason() { return reason; }
    }

    private static class HbA1cResults {
        final SubMeasureResult testing;
        final SubMeasureResult control8;
        final SubMeasureResult poorControl;

        HbA1cResults(SubMeasureResult testing, SubMeasureResult control8, SubMeasureResult poorControl) {
            this.testing = testing;
            this.control8 = control8;
            this.poorControl = poorControl;
        }
    }
}
