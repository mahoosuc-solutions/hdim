package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * COL - Colorectal Cancer Screening (HEDIS)
 *
 * Evaluates colorectal cancer screening for adults aged 50-75.
 * Options: Colonoscopy (10 years), Flexible sigmoidoscopy (5 years), FIT (1 year)
 */
@Component
public class COLMeasure extends AbstractHedisMeasure {

    private static final List<String> COLONOSCOPY_CODES = Arrays.asList(
        "73761001",  // Colonoscopy
        "310634005", // Complete colonoscopy
        "446521004"  // Total colonoscopy
    );

    private static final List<String> SIGMOIDOSCOPY_CODES = Arrays.asList(
        "44441009",  // Flexible sigmoidoscopy
        "425634007"  // Sigmoidoscopy
    );

    private static final List<String> FIT_CODES = Arrays.asList(
        "27396-1",   // Fecal occult blood test
        "56490-6",   // FIT
        "56491-4"    // FIT DNA
    );

    @Override
    public String getMeasureId() {
        return "COL";
    }

    @Override
    public String getMeasureName() {
        return "Colorectal Cancer Screening";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'COL-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating COL measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 50-75)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for colonoscopy in last 10 years
        String colonoscopyDateFilter = "ge" + LocalDate.now().minusYears(10).toString();
        JsonNode colonoscopyProcs = getProcedures(tenantId, patientId, String.join(",", COLONOSCOPY_CODES), colonoscopyDateFilter);
        boolean hasColonoscopy = !getEntries(colonoscopyProcs).isEmpty();

        // Check for sigmoidoscopy in last 5 years
        String sigmoidoscopyDateFilter = "ge" + LocalDate.now().minusYears(5).toString();
        JsonNode sigmoidoscopyProcs = getProcedures(tenantId, patientId, String.join(",", SIGMOIDOSCOPY_CODES), sigmoidoscopyDateFilter);
        boolean hasSigmoidoscopy = !getEntries(sigmoidoscopyProcs).isEmpty();

        // Check for FIT in last 1 year
        String fitDateFilter = "ge" + LocalDate.now().minusYears(1).toString();
        JsonNode fitTests = getObservations(tenantId, patientId, String.join(",", FIT_CODES), fitDateFilter);
        boolean hasFIT = !getEntries(fitTests).isEmpty();

        boolean compliant = hasColonoscopy || hasSigmoidoscopy || hasFIT;
        resultBuilder.inNumerator(compliant);
        resultBuilder.complianceRate(compliant ? 1.0 : 0.0);
        resultBuilder.score(compliant ? 100.0 : 0.0);

        if (compliant) {
            if (hasColonoscopy) {
                String lastDate = getEffectiveDate(getEntries(colonoscopyProcs).get(0));
                resultBuilder.evidence(java.util.Map.of("lastColonoscopy", lastDate));
                resultBuilder.details(java.util.Map.of("screeningCompleted", true, "screeningType", "Colonoscopy", "lastScreeningDate", lastDate));
            } else if (hasSigmoidoscopy) {
                String lastDate = getEffectiveDate(getEntries(sigmoidoscopyProcs).get(0));
                resultBuilder.evidence(java.util.Map.of("lastSigmoidoscopy", lastDate));
                resultBuilder.details(java.util.Map.of("screeningCompleted", true, "screeningType", "Sigmoidoscopy", "lastScreeningDate", lastDate));
            } else {
                String lastDate = getEffectiveDate(getEntries(fitTests).get(0));
                resultBuilder.evidence(java.util.Map.of("lastFIT", lastDate));
                resultBuilder.details(java.util.Map.of("screeningCompleted", true, "screeningType", "FIT", "lastScreeningDate", lastDate));
            }
        } else {
            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("MISSING_COLORECTAL_SCREENING")
                    .description("No colorectal cancer screening in required timeframe")
                    .recommendedAction("Schedule colonoscopy, sigmoidoscopy, or FIT test")
                    .priority("high")
                    .dueDate(LocalDate.now().plusMonths(2))
                    .build()
            ));
            resultBuilder.details(java.util.Map.of("screeningCompleted", false));
        }

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        return age != null && age >= 50 && age <= 75;
    }
}
