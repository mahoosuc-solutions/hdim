package com.healthdata.quality.measure;

import com.healthdata.quality.model.*;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * HEDIS Cervical Cancer Screening (CCS) Measure Calculator
 *
 * Evaluates whether eligible women (ages 21-64) have had appropriate
 * cervical cancer screening:
 * - Pap test within 3 years (ages 21-64)
 * - HPV test within 5 years (ages 30-64, with or without Pap)
 *
 * HEDIS Specification: CCS-E (2024)
 */
@Component
public class CervicalCancerScreeningCalculator implements MeasureCalculator {

    private static final String MEASURE_ID = "CCS";
    private static final String MEASURE_NAME = "Cervical Cancer Screening";
    private static final String VERSION = "2024";

    // CPT codes for Pap smear
    private static final List<String> PAP_TEST_CPT_CODES = Arrays.asList(
        "88141",  // Pap smear screening, automated
        "88142",  // Pap smear screening, automated, thin prep
        "88143",  // Pap smear screening, automated, re-screen
        "88147",  // Pap smear screening, automation or semi-automation
        "88148",  // Pap smear screening, manual, re-screen
        "88150",  // Cytopathology, manual, Bethesda
        "88152",  // Cytopathology, manual, liquid based
        "88153",  // Cytopathology, manual, liquid based, with computer-assisted
        "88164",  // Cytopathology, manual, ThinPrep
        "88165",  // Cytopathology, manual, ThinPrep with computer-assisted
        "88166",  // Cytopathology, manual, ThinPrep computer-only
        "88167",  // Cytopathology, manual, ThinPrep, Bethesda
        "88174",  // Cytopathology, automated ThinPrep
        "88175",  // Cytopathology, automated ThinPrep, computer-assisted
        "Q0091", // Screening Pap smear, obtaining, preparation, conveyance
        "G0101", // Cervical or vaginal cancer screening; pelvic and clinical breast examination
        "G0123", // Screening cytopathology, cervical or vaginal (any reporting system)
        "G0124", // Screening cytopathology, cervical or vaginal, automated thin layer
        "G0141", // Screening cytopathology smears, cervical or vaginal, performed by automated system
        "G0143", // Screening cytopathology, cervical or vaginal, collected in preservative fluid
        "G0144", // Screening cytopathology, cervical or vaginal, automated thin layer preparation
        "G0145", // Screening cytopathology, cervical or vaginal, automated, with physician review
        "G0147", // Screening cytopathology smears, any method
        "G0148"  // Screening cytopathology smears, any method, with physician review
    );

    // CPT codes for HPV test
    private static final List<String> HPV_TEST_CPT_CODES = Arrays.asList(
        "87620",  // HPV, infectious agent detection, direct probe
        "87621",  // HPV, infectious agent detection, amplified probe
        "87622",  // HPV, infectious agent detection, quantification
        "87624",  // HPV, infectious agent detection, high risk types
        "87625"   // HPV, infectious agent detection, types 16 and 18 only
    );

    // SNOMED codes for cervical cancer screening
    private static final List<String> SCREENING_SNOMED_CODES = Arrays.asList(
        "171149006", // Screening for malignant neoplasm of cervix
        "243877001", // Cancer cervix screening status
        "413087003", // Cervical smear taken
        "440623000"  // Microscopy cervical Papanicolaou smear
    );

    // Exclusion: Hysterectomy with removal of cervix
    private static final List<String> HYSTERECTOMY_CODES = Arrays.asList(
        "116140006", // Total hysterectomy
        "116142003", // Total abdominal hysterectomy
        "236886002", // Hysterectomy
        "307771009", // Radical hysterectomy
        "361222003", // Bilateral removal of ovaries with uterus
        "446446002"  // Total abdominal hysterectomy with bilateral salpingo-oophorectomy
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
        if (patient == null || patient.getGender() != Enumerations.AdministrativeGender.FEMALE) {
            result.setExclusionReason("Not female");
            return result;
        }

        // Check age eligibility (21-64)
        int age = calculateAge(patient);
        if (age < 21 || age > 64) {
            result.setExclusionReason("Age not in range 21-64 (age: " + age + ")");
            return result;
        }

        // Check for hysterectomy with removal of cervix
        if (hasHysterectomy(patientData)) {
            result.setExclusionReason("Hysterectomy with removal of cervix on record");
            return result;
        }

        result.setEligible(true);
        result.setDenominatorMembership(true);

        LocalDate now = LocalDate.now();
        boolean hasPapTest = hasRecentTest(patientData, PAP_TEST_CPT_CODES, SCREENING_SNOMED_CODES, now.minusYears(3));
        boolean hasHpvTest = age >= 30 && hasRecentTest(patientData, HPV_TEST_CPT_CODES, Collections.emptyList(), now.minusYears(5));

        boolean isCompliant = hasPapTest || (age >= 30 && hasHpvTest);

        // Create sub-measures
        Map<String, SubMeasureResult> subMeasures = new HashMap<>();
        subMeasures.put("Pap Test", SubMeasureResult.builder()
            .numeratorMembership(hasPapTest)
            .method(hasPapTest ? "procedure" : "none")
            .build());
        if (age >= 30) {
            subMeasures.put("HPV Test", SubMeasureResult.builder()
                .numeratorMembership(hasHpvTest)
                .method(hasHpvTest ? "procedure" : "none")
                .build());
        }

        result.setSubMeasures(subMeasures);

        if (!isCompliant) {
            List<CareGap> careGaps = new ArrayList<>();
            careGaps.add(CareGap.builder()
                .type("missing-cervical-screening")
                .description("Cervical cancer screening overdue")
                .severity("high")
                .action("Schedule cervical cancer screening")
                .measureComponent("Cervical Cancer Screening")
                .build());
            result.setCareGaps(careGaps);

            List<Recommendation> recommendations = new ArrayList<>();
            if (age < 30) {
                recommendations.add(Recommendation.builder()
                    .priority("high")
                    .action("Schedule Pap test")
                    .rationale("Recommend Pap test every 3 years for ages 21-29")
                    .category("screening")
                    .build());
            } else {
                recommendations.add(Recommendation.builder()
                    .priority("high")
                    .action("Schedule Pap test or HPV test")
                    .rationale("Recommend Pap test every 3 years, or HPV test every 5 years for ages 30-64")
                    .category("screening")
                    .build());
            }
            result.setRecommendations(recommendations);
        }

        return result;
    }

    private boolean hasHysterectomy(PatientData patientData) {
        if (patientData.getProcedures() == null) return false;

        return patientData.getProcedures().stream()
            .filter(proc -> proc.getCode() != null && proc.getCode().hasCoding())
            .flatMap(proc -> proc.getCode().getCoding().stream())
            .anyMatch(coding -> HYSTERECTOMY_CODES.contains(coding.getCode()));
    }

    private boolean hasRecentTest(PatientData patientData, List<String> cptCodes, List<String> snomedCodes, LocalDate cutoffDate) {
        // Check procedures
        if (patientData.getProcedures() != null) {
            for (Procedure proc : patientData.getProcedures()) {
                if (proc.getCode() != null && proc.getCode().hasCoding()) {
                    for (Coding coding : proc.getCode().getCoding()) {
                        if (cptCodes.contains(coding.getCode()) || snomedCodes.contains(coding.getCode())) {
                            LocalDate procDate = extractProcedureDate(proc);
                            if (procDate != null && procDate.isAfter(cutoffDate)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        // Check observations
        if (patientData.getObservations() != null) {
            for (Observation obs : patientData.getObservations()) {
                if (obs.getCode() != null && obs.getCode().hasCoding()) {
                    for (Coding coding : obs.getCode().getCoding()) {
                        if (cptCodes.contains(coding.getCode()) || snomedCodes.contains(coding.getCode())) {
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
            // Ignore
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
}
