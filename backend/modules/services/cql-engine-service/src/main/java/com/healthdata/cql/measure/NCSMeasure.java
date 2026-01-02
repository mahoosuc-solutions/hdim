package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * NCS - Non-Recommended Cervical Cancer Screening in Adolescent Females (HEDIS)
 *
 * Identifies inappropriate cervical cancer screening in females under age 21.
 * USPSTF and major guidelines recommend AGAINST screening before age 21.
 *
 * This is an INVERSE measure - LOWER rates are better (less overuse).
 * Being in the numerator indicates INAPPROPRIATE SCREENING.
 */
@Component
public class NCSMeasure extends AbstractHedisMeasure {

    private static final List<String> CERVICAL_CANCER_SCREENING_CODES = Arrays.asList(
        "439958008", // SNOMED - Pap test
        "169676009", // SNOMED - Cervical smear test
        "440623000", // SNOMED - Liquid-based cervical cytology
        "168406007", // SNOMED - Cervical screening
        "315124004", // SNOMED - Cervical cytology
        "44738004"   // SNOMED - Pap smear cervix (procedure)
    );

    private static final List<String> HPV_TEST_CODES = Arrays.asList(
        "21440-3",   // LOINC - HPV DNA
        "59263-4",   // LOINC - HPV 16 DNA
        "59264-2",   // LOINC - HPV 18 DNA
        "69002-4",   // LOINC - HPV 16+18+31+33+35+39+45+51+52+56+58+59+66+68 DNA
        "77379-6"    // LOINC - HPV high risk DNA
    );

    private static final int MINIMUM_RECOMMENDED_AGE = 21;

    @Override
    public String getMeasureId() {
        return "NCS";
    }

    @Override
    public String getMeasureName() {
        return "Non-Recommended Cervical Cancer Screening in Adolescent Females";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'NCS-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating NCS measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be female under age 21)")
                .build();
        }

        resultBuilder.inDenominator(true);

        JsonNode patient = getPatientData(tenantId, patientId);
        Integer age = getPatientAge(patient);

        // Check for cervical cancer screening in last 3 years
        String dateFilter = "ge" + LocalDate.now().minusYears(3).toString();

        // Check for Pap tests
        JsonNode papTests = getObservations(tenantId, patientId,
            String.join(",", CERVICAL_CANCER_SCREENING_CODES), dateFilter);
        boolean hasPapTest = !getEntries(papTests).isEmpty();
        String papTestDate = hasPapTest ? getEffectiveDate(getEntries(papTests).get(0)) : null;

        // Check for HPV tests
        JsonNode hpvTests = getObservations(tenantId, patientId,
            String.join(",", HPV_TEST_CODES), dateFilter);
        boolean hasHPVTest = !getEntries(hpvTests).isEmpty();
        String hpvTestDate = hasHPVTest ? getEffectiveDate(getEntries(hpvTests).get(0)) : null;

        // Any screening indicates inappropriate use
        boolean hasInappropriateScreening = hasPapTest || hasHPVTest;
        String screeningType = hasPapTest ? "Pap test" : hasHPVTest ? "HPV test" : "None";
        String screeningDate = hasPapTest ? papTestDate : hpvTestDate;

        // For INVERSE measure: being in numerator = inappropriate screening (bad outcome)
        resultBuilder.inNumerator(hasInappropriateScreening);
        // Compliance = NOT having inappropriate screening
        resultBuilder.complianceRate(hasInappropriateScreening ? 0.0 : 1.0);
        resultBuilder.score(hasInappropriateScreening ? 0.0 : 100.0);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (hasInappropriateScreening) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("INAPPROPRIATE_CERVICAL_SCREENING")
                .description(String.format("Cervical cancer screening performed at age %d (screening not recommended before age 21)", age))
                .recommendedAction("Follow USPSTF guidelines: No cervical cancer screening for females under 21, regardless of sexual activity")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(3))
                .build());

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("OVERSCREENING_EDUCATION")
                .description("Early screening causes more harm than benefit (false positives, unnecessary procedures)")
                .recommendedAction("Educate providers on evidence-based screening guidelines; implement age-based screening protocols")
                .priority("low")
                .dueDate(LocalDate.now().plusMonths(6))
                .build());
        }

        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "patientAge", age,
            "minimumRecommendedAge", MINIMUM_RECOMMENDED_AGE,
            "hasPapTest", hasPapTest,
            "papTestDate", papTestDate != null ? papTestDate : "None",
            "hasHPVTest", hasHPVTest,
            "hpvTestDate", hpvTestDate != null ? hpvTestDate : "None",
            "hasInappropriateScreening", hasInappropriateScreening,
            "screeningType", screeningType,
            "screeningDate", screeningDate != null ? screeningDate : "None"
        ));

        resultBuilder.evidence(java.util.Map.of(
            "inappropriateScreening", hasInappropriateScreening,
            "screeningMethod", screeningType,
            "appropriateCare", !hasInappropriateScreening
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        // Must be female
        String gender = getPatientGender(patient);
        if (!"female".equalsIgnoreCase(gender)) {
            return false;
        }

        // Must be under age 21
        Integer age = getPatientAge(patient);
        if (age == null || age >= MINIMUM_RECOMMENDED_AGE) {
            return false;
        }

        // Must be between 16-20 (sexually active adolescents most likely to be screened)
        if (age < 16) {
            return false;
        }

        return true;
    }

    /**
     * Get patient gender from FHIR Patient resource
     */
    private String getPatientGender(JsonNode patient) {
        try {
            if (patient.has("gender")) {
                return patient.get("gender").asText();
            }
        } catch (Exception e) {
            logger.warn("Could not extract patient gender: {}", e.getMessage());
        }
        return "unknown";
    }
}
