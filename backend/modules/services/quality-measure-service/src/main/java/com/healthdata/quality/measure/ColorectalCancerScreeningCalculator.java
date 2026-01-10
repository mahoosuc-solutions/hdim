package com.healthdata.quality.measure;

import com.healthdata.quality.model.*;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * HEDIS Colorectal Cancer Screening (COL) Measure Calculator
 *
 * Evaluates whether eligible adults (ages 45-75) have had appropriate
 * colorectal cancer screening:
 * - Colonoscopy within 10 years
 * - Flexible sigmoidoscopy within 5 years
 * - FIT-DNA (Cologuard) within 3 years
 * - Fecal occult blood test (FOBT) within 1 year
 *
 * HEDIS Specification: COL-E (2024)
 */
@Component
public class ColorectalCancerScreeningCalculator implements MeasureCalculator {

    private static final String MEASURE_ID = "COL";
    private static final String MEASURE_NAME = "Colorectal Cancer Screening";
    private static final String VERSION = "2024";

    // CPT codes for colonoscopy
    private static final List<String> COLONOSCOPY_CPT_CODES = Arrays.asList(
        "45378",  // Colonoscopy, diagnostic
        "45380",  // Colonoscopy with biopsy
        "45381",  // Colonoscopy with submucosal injection
        "45382",  // Colonoscopy with control of bleeding
        "45384",  // Colonoscopy with ablation
        "45385",  // Colonoscopy with polypectomy
        "44388",  // Colonoscopy through stoma
        "44389",  // Colonoscopy through stoma with biopsy
        "44391",  // Colonoscopy through stoma with ablation
        "44392",  // Colonoscopy through stoma with polypectomy
        "G0105", // Colorectal cancer screening; colonoscopy on individual at high risk
        "G0121"  // Colorectal cancer screening; colonoscopy on individual not meeting criteria for high risk
    );

    // CPT codes for flexible sigmoidoscopy
    private static final List<String> SIGMOIDOSCOPY_CPT_CODES = Arrays.asList(
        "45330",  // Sigmoidoscopy, diagnostic
        "45331",  // Sigmoidoscopy with biopsy
        "45333",  // Sigmoidoscopy with ablation
        "45338",  // Sigmoidoscopy with polypectomy
        "G0104"   // Colorectal cancer screening; flexible sigmoidoscopy
    );

    // CPT codes for FOBT/FIT
    private static final List<String> FOBT_CPT_CODES = Arrays.asList(
        "82270",  // Occult blood, feces screening
        "82274",  // Blood, occult, feces, immunoassay (FIT)
        "G0328"   // Colorectal cancer screening; fecal-occult blood test
    );

    // CPT codes for FIT-DNA (Cologuard)
    private static final List<String> FIT_DNA_CPT_CODES = Arrays.asList(
        "81528",  // Fecal DNA (Cologuard)
        "G0464"   // Colorectal cancer screening; stool-based DNA (Cologuard)
    );

    // Exclusion: Colorectal cancer or total colectomy
    private static final List<String> EXCLUSION_CODES = Arrays.asList(
        "363406005", // Malignant tumor of colon
        "363351006", // Malignant tumor of rectum
        "26390003",  // Total colectomy
        "303401008"  // Total proctocolectomy
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

        Patient patient = patientData.getPatient();
        if (patient == null) {
            result.setExclusionReason("No patient data");
            return result;
        }

        // Check age eligibility (45-75)
        int age = calculateAge(patient);
        if (age < 45 || age > 75) {
            result.setExclusionReason("Age not in range 45-75 (age: " + age + ")");
            return result;
        }

        // Check for exclusions (colorectal cancer, total colectomy)
        if (hasExclusionCondition(patientData)) {
            result.setExclusionReason("Colorectal cancer or total colectomy on record");
            return result;
        }

        result.setEligible(true);
        result.setDenominatorMembership(true);

        // Check for screening within appropriate timeframes
        LocalDate now = LocalDate.now();
        boolean hasColonoscopy = hasRecentProcedure(patientData, COLONOSCOPY_CPT_CODES, now.minusYears(10));
        boolean hasSigmoidoscopy = hasRecentProcedure(patientData, SIGMOIDOSCOPY_CPT_CODES, now.minusYears(5));
        boolean hasFitDna = hasRecentProcedure(patientData, FIT_DNA_CPT_CODES, now.minusYears(3));
        boolean hasFobt = hasRecentProcedure(patientData, FOBT_CPT_CODES, now.minusYears(1));

        boolean isCompliant = hasColonoscopy || hasSigmoidoscopy || hasFitDna || hasFobt;

        // Create sub-measures
        Map<String, SubMeasureResult> subMeasures = new HashMap<>();
        subMeasures.put("Colonoscopy", SubMeasureResult.builder()
            .numeratorMembership(hasColonoscopy)
            .method(hasColonoscopy ? "procedure" : "none")
            .build());
        subMeasures.put("Sigmoidoscopy", SubMeasureResult.builder()
            .numeratorMembership(hasSigmoidoscopy)
            .method(hasSigmoidoscopy ? "procedure" : "none")
            .build());
        subMeasures.put("FIT-DNA", SubMeasureResult.builder()
            .numeratorMembership(hasFitDna)
            .method(hasFitDna ? "procedure" : "none")
            .build());
        subMeasures.put("FOBT/FIT", SubMeasureResult.builder()
            .numeratorMembership(hasFobt)
            .method(hasFobt ? "procedure" : "none")
            .build());

        result.setSubMeasures(subMeasures);

        if (!isCompliant) {
            // Create care gap
            List<CareGap> careGaps = new ArrayList<>();
            careGaps.add(CareGap.builder()
                .type("missing-colorectal-screening")
                .description("No colorectal cancer screening on record")
                .severity(age >= 50 ? "high" : "medium")
                .action("Schedule colorectal cancer screening")
                .measureComponent("Colorectal Cancer Screening")
                .build());
            result.setCareGaps(careGaps);

            List<Recommendation> recommendations = new ArrayList<>();
            recommendations.add(Recommendation.builder()
                .priority(age >= 50 ? "high" : "medium")
                .action("Schedule colonoscopy (preferred) or stool-based test")
                .rationale("Patient is due for colorectal cancer screening")
                .category("screening")
                .build());
            result.setRecommendations(recommendations);
        }

        return result;
    }

    private boolean hasExclusionCondition(PatientData patientData) {
        if (patientData.getConditions() == null) return false;

        return patientData.getConditions().stream()
            .filter(cond -> cond.getCode() != null && cond.getCode().hasCoding())
            .flatMap(cond -> cond.getCode().getCoding().stream())
            .anyMatch(coding -> EXCLUSION_CODES.contains(coding.getCode()));
    }

    private boolean hasRecentProcedure(PatientData patientData, List<String> cptCodes, LocalDate cutoffDate) {
        if (patientData.getProcedures() == null) return false;

        return patientData.getProcedures().stream()
            .filter(proc -> proc.getCode() != null && proc.getCode().hasCoding())
            .anyMatch(proc -> {
                boolean hasCode = proc.getCode().getCoding().stream()
                    .anyMatch(coding -> cptCodes.contains(coding.getCode()));
                if (!hasCode) return false;

                LocalDate procDate = extractProcedureDate(proc);
                return procDate != null && procDate.isAfter(cutoffDate);
            });
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
}
