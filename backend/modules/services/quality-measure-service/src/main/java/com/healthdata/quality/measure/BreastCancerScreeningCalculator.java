package com.healthdata.quality.measure;

import com.healthdata.quality.model.*;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * HEDIS Breast Cancer Screening (BCS) Measure Calculator
 *
 * Evaluates whether eligible women (ages 50-74) have had a mammogram within
 * the past 27 months (2 years plus 3-month grace period).
 *
 * HEDIS Specification: BCS-E (2024)
 */
@Component
public class BreastCancerScreeningCalculator implements MeasureCalculator {

    private static final String MEASURE_ID = "BCS";
    private static final String MEASURE_NAME = "Breast Cancer Screening";
    private static final String VERSION = "2024";

    // CPT codes for mammography
    private static final List<String> MAMMOGRAPHY_CPT_CODES = Arrays.asList(
        "77067",  // Screening mammography, bilateral (2D)
        "77066",  // Diagnostic mammography
        "77065",  // Diagnostic mammography, unilateral
        "77063",  // Screening digital breast tomosynthesis (3D)
        "G0202",  // Screening mammography, including CAD
        "G0204",  // Diagnostic mammography, including CAD
        "G0206"   // Diagnostic mammography with CAD, bilateral
    );

    // SNOMED codes for mammography
    private static final List<String> MAMMOGRAPHY_SNOMED_CODES = Arrays.asList(
        "24623002",  // Screening mammography
        "241055006", // Mammography of breast
        "71651007",  // Mammography
        "726551006"  // Digital breast tomosynthesis
    );

    // Exclusion codes
    private static final List<String> BILATERAL_MASTECTOMY_CODES = Arrays.asList(
        "428571003", // Bilateral mastectomy
        "14693006",  // Total mastectomy of both breasts
        "61614008"   // Bilateral modified radical mastectomy
    );

    @Override
    public MeasureResult calculate(PatientData patientData) {
        MeasureResult result = MeasureResult.builder()
            .measureId(MEASURE_ID)
            .measureName(MEASURE_NAME)
            .patientId(patientData.getPatient() != null
                ? parsePatientId(patientData.getPatient().getId())
                : null)
            .build();

        // Check gender eligibility (female only)
        Patient patient = patientData.getPatient();
        if (patient == null || patient.getGender() != Enumerations.AdministrativeGender.FEMALE) {
            result.setExclusionReason("Not female");
            return result;
        }

        // Check age eligibility (50-74)
        int age = calculateAge(patient);
        if (age < 50 || age > 74) {
            result.setExclusionReason("Age not in range 50-74 (age: " + age + ")");
            return result;
        }

        // Check for bilateral mastectomy exclusion
        if (hasBilateralMastectomy(patientData)) {
            result.setExclusionReason("Bilateral mastectomy on record");
            return result;
        }

        result.setEligible(true);
        result.setDenominatorMembership(true);

        // Check for mammography in the past 27 months
        LocalDate cutoffDate = LocalDate.now().minusMonths(27);
        boolean hasMammogram = hasRecentMammography(patientData, cutoffDate);

        // Create sub-measure for mammography screening
        Map<String, SubMeasureResult> subMeasures = new HashMap<>();
        subMeasures.put("Mammography Screening", SubMeasureResult.builder()
            .numeratorMembership(hasMammogram)
            .method(hasMammogram ? "procedure" : "none")
            .build());
        result.setSubMeasures(subMeasures);

        if (!hasMammogram) {
            // Create care gap
            List<CareGap> careGaps = new ArrayList<>();
            careGaps.add(CareGap.builder()
                .type("missing-mammography")
                .description("No mammography screening in past 27 months")
                .severity("high")
                .action("Schedule screening mammography (CPT 77067)")
                .measureComponent("Mammography Screening")
                .build());
            result.setCareGaps(careGaps);

            // Add recommendation
            List<Recommendation> recommendations = new ArrayList<>();
            recommendations.add(Recommendation.builder()
                .priority("high")
                .action("Schedule bilateral screening mammography")
                .rationale("Patient is due for breast cancer screening mammography")
                .category("screening")
                .build());
            result.setRecommendations(recommendations);
        }

        return result;
    }

    private boolean hasBilateralMastectomy(PatientData patientData) {
        if (patientData.getProcedures() == null) return false;

        return patientData.getProcedures().stream()
            .filter(proc -> proc.getCode() != null && proc.getCode().hasCoding())
            .flatMap(proc -> proc.getCode().getCoding().stream())
            .anyMatch(coding -> BILATERAL_MASTECTOMY_CODES.contains(coding.getCode()));
    }

    private boolean hasRecentMammography(PatientData patientData, LocalDate cutoffDate) {
        // Check procedures
        if (patientData.getProcedures() != null) {
            for (Procedure proc : patientData.getProcedures()) {
                if (proc.getCode() != null && proc.getCode().hasCoding()) {
                    for (Coding coding : proc.getCode().getCoding()) {
                        if (MAMMOGRAPHY_CPT_CODES.contains(coding.getCode()) ||
                            MAMMOGRAPHY_SNOMED_CODES.contains(coding.getCode())) {
                            // Check procedure date
                            LocalDate procDate = extractProcedureDate(proc);
                            if (procDate != null && procDate.isAfter(cutoffDate)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        // Check observations (some systems record mammograms as observations)
        if (patientData.getObservations() != null) {
            for (Observation obs : patientData.getObservations()) {
                if (obs.getCode() != null && obs.getCode().hasCoding()) {
                    for (Coding coding : obs.getCode().getCoding()) {
                        if (MAMMOGRAPHY_CPT_CODES.contains(coding.getCode()) ||
                            MAMMOGRAPHY_SNOMED_CODES.contains(coding.getCode())) {
                            LocalDate obsDate = extractObservationDate(obs);
                            if (obsDate != null && obsDate.isAfter(cutoffDate)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private LocalDate extractProcedureDate(Procedure proc) {
        try {
            if (proc.getPerformedDateTimeType() != null) {
                return proc.getPerformedDateTimeType().getValue().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            } else if (proc.getPerformedPeriod() != null && proc.getPerformedPeriod().getStart() != null) {
                return proc.getPerformedPeriod().getStart().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            }
        } catch (Exception e) {
            // Ignore date parsing errors
        }
        return null;
    }

    private LocalDate extractObservationDate(Observation obs) {
        try {
            if (obs.getEffectiveDateTimeType() != null) {
                return obs.getEffectiveDateTimeType().getValue().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            }
        } catch (Exception e) {
            // Ignore date parsing errors
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
            // Handle "Patient/uuid" format
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
}
