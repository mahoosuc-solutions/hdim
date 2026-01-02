package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * CCS - Cervical Cancer Screening (HEDIS)
 *
 * Evaluates cervical cancer screening for women aged 21-64.
 * Target: Pap smear within last 3 years or HPV test within last 5 years
 */
@Component
public class CCSMeasure extends AbstractHedisMeasure {

    private static final List<String> PAP_SMEAR_CODES = Arrays.asList(
        "10524-7",   // Cytology Cervix
        "19762-4",   // Pap smear
        "19764-0",   // Cervical cytology
        "447639003"  // Cervical cytology SNOMED
    );

    private static final List<String> HPV_TEST_CODES = Arrays.asList(
        "21440-3",   // HPV DNA test
        "59420-0",   // HPV high risk
        "77379-6"    // HPV genotype
    );

    @Override
    public String getMeasureId() {
        return "CCS";
    }

    @Override
    public String getMeasureName() {
        return "Cervical Cancer Screening";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'CCS-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating CCS measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be female, age 21-64)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for Pap smear in last 3 years
        String papDateFilter = "ge" + LocalDate.now().minusYears(3).toString();
        JsonNode papTests = getObservations(tenantId, patientId, String.join(",", PAP_SMEAR_CODES), papDateFilter);
        boolean hasPapSmear = !getEntries(papTests).isEmpty();

        // Check for HPV test in last 5 years (for women 30+)
        JsonNode patient = getPatientData(tenantId, patientId);
        Integer age = getPatientAge(patient);
        boolean hasHPVTest = false;

        if (age != null && age >= 30) {
            String hpvDateFilter = "ge" + LocalDate.now().minusYears(5).toString();
            JsonNode hpvTests = getObservations(tenantId, patientId, String.join(",", HPV_TEST_CODES), hpvDateFilter);
            hasHPVTest = !getEntries(hpvTests).isEmpty();
        }

        boolean compliant = hasPapSmear || hasHPVTest;
        resultBuilder.inNumerator(compliant);
        resultBuilder.complianceRate(compliant ? 1.0 : 0.0);
        resultBuilder.score(compliant ? 100.0 : 0.0);

        if (compliant) {
            if (hasPapSmear) {
                String lastPapDate = getEffectiveDate(getEntries(papTests).get(0));
                resultBuilder.evidence(java.util.Map.of("lastPapSmear", lastPapDate));
                resultBuilder.details(java.util.Map.of("screeningCompleted", true, "screeningType", "Pap smear", "lastScreeningDate", lastPapDate));
            } else {
                String lastHPVDate = getEffectiveDate(getEntries(getObservations(tenantId, patientId, String.join(",", HPV_TEST_CODES), "ge" + LocalDate.now().minusYears(5).toString())).get(0));
                resultBuilder.evidence(java.util.Map.of("lastHPVTest", lastHPVDate));
                resultBuilder.details(java.util.Map.of("screeningCompleted", true, "screeningType", "HPV test", "lastScreeningDate", lastHPVDate));
            }
        } else {
            String recommendedTest = (age != null && age >= 30) ? "Pap smear or HPV test" : "Pap smear";
            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("MISSING_CERVICAL_SCREENING")
                    .description("No cervical cancer screening in required timeframe")
                    .recommendedAction("Schedule " + recommendedTest)
                    .priority("high")
                    .dueDate(LocalDate.now().plusMonths(1))
                    .build()
            ));
            resultBuilder.details(java.util.Map.of("screeningCompleted", false, "recommendedTest", recommendedTest));
        }

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        // Must be female
        boolean isFemale = false;
        if (patient.has("gender")) {
            String gender = patient.get("gender").asText();
            isFemale = gender.equalsIgnoreCase("female");
        }

        // Age 21-64
        Integer age = getPatientAge(patient);
        boolean isAgeEligible = age != null && age >= 21 && age <= 64;

        return isFemale && isAgeEligible;
    }
}
