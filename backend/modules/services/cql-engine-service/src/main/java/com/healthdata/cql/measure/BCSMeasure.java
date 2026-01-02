package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * BCS - Breast Cancer Screening (HEDIS)
 *
 * Evaluates mammography screening for women aged 50-74.
 * Target: Mammogram within last 27 months
 */
@Component
public class BCSMeasure extends AbstractHedisMeasure {

    private static final List<String> MAMMOGRAPHY_CODES = Arrays.asList(
        "24606-6",   // Mammography study
        "71651007",  // Mammography (SNOMED)
        "24604-1"    // Mammography diagnostic
    );

    @Override
    public String getMeasureId() {
        return "BCS";
    }

    @Override
    public String getMeasureName() {
        return "Breast Cancer Screening";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'BCS-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating BCS measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be female, age 50-74)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Look for mammogram in last 27 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(27).toString();
        JsonNode procedures = getProcedures(tenantId, patientId, String.join(",", MAMMOGRAPHY_CODES), dateFilter);
        List<JsonNode> mammograms = getEntries(procedures);

        boolean hasRecentMammogram = !mammograms.isEmpty();
        resultBuilder.inNumerator(hasRecentMammogram);
        resultBuilder.complianceRate(hasRecentMammogram ? 1.0 : 0.0);
        resultBuilder.score(hasRecentMammogram ? 100.0 : 0.0);

        if (hasRecentMammogram) {
            String lastMammogramDate = getEffectiveDate(mammograms.get(0));
            resultBuilder.evidence(java.util.Map.of("lastMammogram", lastMammogramDate));
            resultBuilder.details(java.util.Map.of("mammogramCompleted", true, "lastMammogramDate", lastMammogramDate));
        } else {
            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("MISSING_MAMMOGRAM")
                    .description("No mammogram in last 27 months")
                    .recommendedAction("Schedule screening mammography")
                    .priority("high")
                    .dueDate(LocalDate.now().plusMonths(1))
                    .build()
            ));
            resultBuilder.details(java.util.Map.of("mammogramCompleted", false));
        }

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        // Check gender (must be female)
        boolean isFemale = false;
        if (patient.has("gender")) {
            String gender = patient.get("gender").asText();
            isFemale = gender.equalsIgnoreCase("female");
        }

        // Check age (50-74 years)
        Integer age = getPatientAge(patient);
        boolean isAgeEligible = age != null && age >= 50 && age <= 74;

        return isFemale && isAgeEligible;
    }
}
