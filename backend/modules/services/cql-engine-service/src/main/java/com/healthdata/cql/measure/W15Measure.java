package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * W15 - Well-Child Visits in the Third, Fourth, Fifth and Sixth Years of Life (HEDIS)
 *
 * Evaluates whether children aged 3-6 years received at least one well-child visit
 * with a primary care provider during the measurement year.
 */
@Component
public class W15Measure extends AbstractHedisMeasure {

    private static final List<String> WELL_CHILD_VISIT_CODES = Arrays.asList(
        "410620009", // Well child visit (SNOMED)
        "410621008", // Well baby visit (SNOMED)
        "410625004", // Well adolescent visit (SNOMED)
        "390906007", // Well child visit (SNOMED)
        "424619006", // Prenatal visit (SNOMED)
        "185349003"  // Encounter for check up (SNOMED)
    );

    private static final List<String> PREVENTIVE_CARE_CODES = Arrays.asList(
        "738751004", // Preventive care encounter (SNOMED)
        "439708006", // Home visit by physician (SNOMED)
        "444971000124105" // Annual wellness visit (SNOMED)
    );

    private static final List<String> DEVELOPMENTAL_SCREENING_CODES = Arrays.asList(
        "252957005", // Developmental screening (SNOMED)
        "171207006", // Childhood developmental screening (SNOMED)
        "428211000124100" // Assessment of development (SNOMED)
    );

    @Override
    public String getMeasureId() {
        return "W15";
    }

    @Override
    public String getMeasureName() {
        return "Well-Child Visits (3-6 years)";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'W15-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating W15 measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 3-6)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for well-child visits in the last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();

        // Check for well-child visits
        JsonNode wellChildVisits = getEncounters(tenantId, patientId,
            String.join(",", WELL_CHILD_VISIT_CODES), dateFilter);
        List<JsonNode> wellChildEntries = getEntries(wellChildVisits);

        // Check for preventive care visits
        JsonNode preventiveVisits = getEncounters(tenantId, patientId,
            String.join(",", PREVENTIVE_CARE_CODES), dateFilter);
        List<JsonNode> preventiveEntries = getEntries(preventiveVisits);

        // Check for developmental screenings
        JsonNode developmentalScreenings = getObservations(tenantId, patientId,
            String.join(",", DEVELOPMENTAL_SCREENING_CODES), dateFilter);
        List<JsonNode> screeningEntries = getEntries(developmentalScreenings);

        boolean hasWellChildVisit = !wellChildEntries.isEmpty();
        boolean hasPreventiveVisit = !preventiveEntries.isEmpty();
        boolean hasDevelopmentalScreening = !screeningEntries.isEmpty();

        // Patient is in numerator if they had at least one well-child or preventive visit
        boolean hasQualifyingVisit = hasWellChildVisit || hasPreventiveVisit;
        resultBuilder.inNumerator(hasQualifyingVisit);
        resultBuilder.complianceRate(hasQualifyingVisit ? 1.0 : 0.0);
        resultBuilder.score(hasQualifyingVisit ? 100.0 : 0.0);

        if (hasQualifyingVisit) {
            int totalVisits = wellChildEntries.size() + preventiveEntries.size();
            String lastVisitDate = hasWellChildVisit
                ? getEffectiveDate(wellChildEntries.get(0))
                : getEffectiveDate(preventiveEntries.get(0));

            resultBuilder.evidence(java.util.Map.of(
                "wellChildVisits", wellChildEntries.size(),
                "preventiveVisits", preventiveEntries.size(),
                "totalVisits", totalVisits,
                "lastVisitDate", lastVisitDate,
                "hasDevelopmentalScreening", hasDevelopmentalScreening
            ));

            resultBuilder.details(java.util.Map.of(
                "hasWellChildVisit", true,
                "lastVisitDate", lastVisitDate,
                "totalVisitsInYear", totalVisits,
                "hasDevelopmentalScreening", hasDevelopmentalScreening
            ));

            // Still check for developmental screening care gap
            if (!hasDevelopmentalScreening) {
                resultBuilder.careGaps(List.of(
                    MeasureResult.CareGap.builder()
                        .gapType("MISSING_DEVELOPMENTAL_SCREENING")
                        .description("No developmental screening documented in last 12 months")
                        .recommendedAction("Perform developmental screening at next well-child visit")
                        .priority("medium")
                        .dueDate(LocalDate.now().plusMonths(3))
                        .build()
                ));
            }
        } else {
            // No qualifying visit - identify care gap
            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("MISSING_WELL_CHILD_VISIT")
                    .description("No well-child visit in the last 12 months")
                    .recommendedAction("Schedule annual well-child visit with primary care provider")
                    .priority("high")
                    .dueDate(LocalDate.now().plusMonths(1))
                    .build()
            ));

            resultBuilder.details(java.util.Map.of(
                "hasWellChildVisit", false,
                "totalVisitsInYear", 0
            ));
        }

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        // Eligible for children aged 3-6 years
        return age != null && age >= 3 && age <= 6;
    }
}
