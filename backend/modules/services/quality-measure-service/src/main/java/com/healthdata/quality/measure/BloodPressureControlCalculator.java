package com.healthdata.quality.measure;

import com.healthdata.quality.model.*;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HEDIS Controlling High Blood Pressure (CBP) Measure Calculator
 *
 * Evaluates whether patients with hypertension have adequately controlled
 * blood pressure (< 140/90 mmHg).
 *
 * HEDIS Specification: CBP-E (2024)
 */
@Component
public class BloodPressureControlCalculator implements MeasureCalculator {

    private static final String MEASURE_ID = "CBP";
    private static final String MEASURE_NAME = "Controlling High Blood Pressure";
    private static final String VERSION = "2024";

    // ICD-10 codes for hypertension
    private static final List<String> HYPERTENSION_ICD10_CODES = Arrays.asList(
        "I10",    // Essential (primary) hypertension
        "I11",    // Hypertensive heart disease
        "I11.0",  // Hypertensive heart disease with heart failure
        "I11.9",  // Hypertensive heart disease without heart failure
        "I12",    // Hypertensive chronic kidney disease
        "I13",    // Hypertensive heart and chronic kidney disease
        "I15",    // Secondary hypertension
        "I16"     // Hypertensive crisis
    );

    // SNOMED codes for hypertension
    private static final List<String> HYPERTENSION_SNOMED_CODES = Arrays.asList(
        "38341003",  // Hypertensive disorder
        "59621000",  // Essential hypertension
        "59720008",  // Sustained diastolic hypertension
        "123799005", // Renovascular hypertension
        "194781006"  // Secondary malignant renovascular hypertension
    );

    // LOINC codes for blood pressure
    private static final String SYSTOLIC_BP_LOINC = "8480-6";
    private static final String DIASTOLIC_BP_LOINC = "8462-4";
    private static final String BP_PANEL_LOINC = "85354-9";

    // Thresholds
    private static final int SYSTOLIC_THRESHOLD = 140;
    private static final int DIASTOLIC_THRESHOLD = 90;

    @Override
    public MeasureResult calculate(PatientData patientData) {
        MeasureResult result = MeasureResult.builder()
            .measureId(MEASURE_ID)
            .measureName(MEASURE_NAME)
            .patientId(patientData.getPatient() != null
                ? parsePatientId(patientData.getPatient().getId())
                : null)
            .build();

        Patient patient = patientData.getPatient();
        if (patient == null) {
            result.setExclusionReason("No patient data");
            return result;
        }

        // Check age eligibility (18-85)
        int age = calculateAge(patient);
        if (age < 18 || age > 85) {
            result.setExclusionReason("Age not in range 18-85 (age: " + age + ")");
            return result;
        }

        // Check for hypertension diagnosis
        if (!hasHypertensionDiagnosis(patientData)) {
            result.setExclusionReason("No hypertension diagnosis");
            return result;
        }

        result.setEligible(true);
        result.setDenominatorMembership(true);

        // Get most recent blood pressure reading
        BPReading latestBP = getMostRecentBPReading(patientData);

        if (latestBP == null) {
            // No BP reading on file - create care gap
            List<CareGap> careGaps = new ArrayList<>();
            careGaps.add(CareGap.builder()
                .type("missing-bp-reading")
                .description("No blood pressure reading on file")
                .severity("high")
                .action("Obtain blood pressure measurement")
                .measureComponent("Blood Pressure Control")
                .build());
            result.setCareGaps(careGaps);

            List<Recommendation> recommendations = new ArrayList<>();
            recommendations.add(Recommendation.builder()
                .priority("high")
                .action("Measure blood pressure at next visit")
                .rationale("Patient with hypertension needs blood pressure measurement")
                .category("screening")
                .build());
            result.setRecommendations(recommendations);
            return result;
        }

        // Evaluate BP control
        boolean systolicControlled = latestBP.systolic < SYSTOLIC_THRESHOLD;
        boolean diastolicControlled = latestBP.diastolic < DIASTOLIC_THRESHOLD;
        boolean isControlled = systolicControlled && diastolicControlled;

        // Create sub-measures
        Map<String, SubMeasureResult> subMeasures = new HashMap<>();
        subMeasures.put("Systolic BP Control", SubMeasureResult.builder()
            .numeratorMembership(systolicControlled)
            .value(String.valueOf(latestBP.systolic))
            .numericValue((double) latestBP.systolic)
            .build());
        subMeasures.put("Diastolic BP Control", SubMeasureResult.builder()
            .numeratorMembership(diastolicControlled)
            .value(String.valueOf(latestBP.diastolic))
            .numericValue((double) latestBP.diastolic)
            .build());

        result.setSubMeasures(subMeasures);

        if (!isControlled) {
            // Create care gap
            String description = !systolicControlled && !diastolicControlled
                ? String.format("Blood pressure elevated (%d/%d mmHg)", latestBP.systolic, latestBP.diastolic)
                : !systolicControlled
                    ? String.format("Systolic BP elevated (%d mmHg)", latestBP.systolic)
                    : String.format("Diastolic BP elevated (%d mmHg)", latestBP.diastolic);

            String severity = (latestBP.systolic >= 160 || latestBP.diastolic >= 100) ? "critical" : "high";

            List<CareGap> careGaps = new ArrayList<>();
            careGaps.add(CareGap.builder()
                .type("uncontrolled-blood-pressure")
                .description(description)
                .severity(severity)
                .action("Intensify blood pressure management")
                .measureComponent("Blood Pressure Control")
                .build());
            result.setCareGaps(careGaps);

            List<Recommendation> recommendations = new ArrayList<>();
            recommendations.add(Recommendation.builder()
                .priority(severity)
                .action("Adjust antihypertensive therapy")
                .rationale(String.format("Current BP: %d/%d mmHg. Target: < %d/%d mmHg.",
                    latestBP.systolic, latestBP.diastolic, SYSTOLIC_THRESHOLD, DIASTOLIC_THRESHOLD))
                .category("medication")
                .build());
            if (latestBP.systolic >= 160 || latestBP.diastolic >= 100) {
                recommendations.add(Recommendation.builder()
                    .priority("critical")
                    .action("URGENT: Stage 2 hypertension requires prompt intervention")
                    .rationale("Blood pressure significantly elevated")
                    .category("medication")
                    .build());
            }
            result.setRecommendations(recommendations);
        }

        return result;
    }

    private boolean hasHypertensionDiagnosis(PatientData patientData) {
        if (patientData.getConditions() == null) return false;

        return patientData.getConditions().stream()
            .filter(cond -> cond.getCode() != null && cond.getCode().hasCoding())
            .flatMap(cond -> cond.getCode().getCoding().stream())
            .anyMatch(coding ->
                HYPERTENSION_ICD10_CODES.contains(coding.getCode()) ||
                HYPERTENSION_SNOMED_CODES.contains(coding.getCode()));
    }

    private BPReading getMostRecentBPReading(PatientData patientData) {
        if (patientData.getObservations() == null) return null;

        // Get systolic and diastolic observations
        List<Observation> bpObservations = patientData.getObservations().stream()
            .filter(obs -> obs.getCode() != null && obs.getCode().hasCoding())
            .filter(obs -> obs.getCode().getCoding().stream()
                .anyMatch(c -> SYSTOLIC_BP_LOINC.equals(c.getCode()) ||
                              DIASTOLIC_BP_LOINC.equals(c.getCode()) ||
                              BP_PANEL_LOINC.equals(c.getCode())))
            .sorted((a, b) -> compareObservationDates(b, a)) // Most recent first
            .collect(Collectors.toList());

        if (bpObservations.isEmpty()) return null;

        // Try to find a BP panel first
        for (Observation obs : bpObservations) {
            if (obs.getCode().getCoding().stream().anyMatch(c -> BP_PANEL_LOINC.equals(c.getCode()))) {
                BPReading reading = extractBPFromPanel(obs);
                if (reading != null) return reading;
            }
        }

        // Fall back to individual systolic/diastolic readings
        Integer systolic = null;
        Integer diastolic = null;
        LocalDate readingDate = null;

        for (Observation obs : bpObservations) {
            if (systolic != null && diastolic != null) break;

            for (Coding coding : obs.getCode().getCoding()) {
                if (SYSTOLIC_BP_LOINC.equals(coding.getCode()) && obs.hasValueQuantity()) {
                    systolic = obs.getValueQuantity().getValue().intValue();
                    readingDate = extractObservationDate(obs);
                } else if (DIASTOLIC_BP_LOINC.equals(coding.getCode()) && obs.hasValueQuantity()) {
                    diastolic = obs.getValueQuantity().getValue().intValue();
                    if (readingDate == null) readingDate = extractObservationDate(obs);
                }
            }
        }

        if (systolic != null && diastolic != null) {
            return new BPReading(systolic, diastolic, readingDate);
        }

        return null;
    }

    private BPReading extractBPFromPanel(Observation obs) {
        if (!obs.hasComponent()) return null;

        Integer systolic = null;
        Integer diastolic = null;

        for (Observation.ObservationComponentComponent component : obs.getComponent()) {
            if (component.getCode() != null && component.getCode().hasCoding()) {
                for (Coding coding : component.getCode().getCoding()) {
                    if (SYSTOLIC_BP_LOINC.equals(coding.getCode()) && component.hasValueQuantity()) {
                        systolic = component.getValueQuantity().getValue().intValue();
                    } else if (DIASTOLIC_BP_LOINC.equals(coding.getCode()) && component.hasValueQuantity()) {
                        diastolic = component.getValueQuantity().getValue().intValue();
                    }
                }
            }
        }

        if (systolic != null && diastolic != null) {
            return new BPReading(systolic, diastolic, extractObservationDate(obs));
        }
        return null;
    }

    private int compareObservationDates(Observation a, Observation b) {
        LocalDate dateA = extractObservationDate(a);
        LocalDate dateB = extractObservationDate(b);
        if (dateA == null && dateB == null) return 0;
        if (dateA == null) return -1;
        if (dateB == null) return 1;
        return dateA.compareTo(dateB);
    }

    private LocalDate extractObservationDate(Observation obs) {
        try {
            if (obs.getEffectiveDateTimeType() != null) {
                return obs.getEffectiveDateTimeType().getValue().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private int calculateAge(Patient patient) {
        if (patient.getBirthDate() == null) return 0;
        LocalDate birthDate = patient.getBirthDate().toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDate();
        return java.time.Period.between(birthDate, LocalDate.now()).getYears();
    }

    private UUID parsePatientId(String id) {
        try {
            if (id != null && id.contains("/")) {
                id = id.substring(id.lastIndexOf("/") + 1);
            }
            return UUID.fromString(id);
        } catch (Exception e) {
            return null;
        }
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

    private static class BPReading {
        final int systolic;
        final int diastolic;
        final LocalDate date;

        BPReading(int systolic, int diastolic, LocalDate date) {
            this.systolic = systolic;
            this.diastolic = diastolic;
            this.date = date;
        }
    }
}
