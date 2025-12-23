package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * LSC - Lead Screening in Children (HEDIS)
 *
 * Evaluates whether children received blood lead screening at appropriate ages.
 * Lead exposure can cause developmental delays, learning difficulties, and behavioral problems.
 *
 * Two age-based components:
 * - Children screened by age 2
 * - Children screened by age 6 (if living in high-risk areas)
 */
@Component
public class LSCMeasure extends AbstractHedisMeasure {

    private static final List<String> LEAD_SCREENING_CODES = Arrays.asList(
        "5671-3",    // LOINC - Lead [Mass/volume] in Blood
        "10368-9",   // LOINC - Lead [Mass/volume] in Capillary blood
        "77307-7",   // LOINC - Lead [Mass/volume] in Venous blood
        "5672-1",    // LOINC - Lead [Presence] in Blood
        "14910-5",   // LOINC - Lead [Moles/volume] in Blood
        "30241-0"    // LOINC - Lead panel - Blood
    );

    private static final List<String> HIGH_RISK_INDICATORS = Arrays.asList(
        // Environmental risk factors
        "102487001", // SNOMED - High risk environment for lead exposure
        "425753007", // SNOMED - History of exposure to environmental pollutant
        "417662000", // SNOMED - History of occupational exposure
        // Behavioral risk
        "271821002", // SNOMED - Pica (eating non-food items)
        "72863001"   // SNOMED - Snoring (poor nutrition marker)
    );

    @Override
    public String getMeasureId() {
        return "LSC";
    }

    @Override
    public String getMeasureName() {
        return "Lead Screening in Children";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'LSC-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating LSC measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be child age 1-6)")
                .build();
        }

        resultBuilder.inDenominator(true);

        JsonNode patient = getPatientData(tenantId, patientId);
        Integer age = getPatientAge(patient);
        LocalDate birthDate = getPatientBirthDate(patient);

        // Check for lead screening tests
        JsonNode leadScreenings = getObservations(tenantId, patientId,
            String.join(",", LEAD_SCREENING_CODES), null);
        List<JsonNode> screeningEntries = getEntries(leadScreenings);

        // Check for high-risk indicators
        JsonNode riskFactors = getConditions(tenantId, patientId,
            String.join(",", HIGH_RISK_INDICATORS));
        boolean hasHighRiskFactors = !getEntries(riskFactors).isEmpty();

        // Determine screening requirements based on age
        boolean needsScreeningByAge2 = age >= 1 && age <= 2;
        boolean needsScreeningByAge6 = age > 2 && age <= 6 && hasHighRiskFactors;

        // Check for screening completion
        boolean hasScreeningByAge2 = false;
        boolean hasAnyScreening = !screeningEntries.isEmpty();
        String mostRecentScreeningDate = null;
        Double mostRecentLeadLevel = null;

        if (hasAnyScreening) {
            JsonNode mostRecentScreening = screeningEntries.get(0);
            mostRecentScreeningDate = getEffectiveDate(mostRecentScreening);
            LocalDate screeningDate = LocalDate.parse(mostRecentScreeningDate);

            // Extract lead level if available
            try {
                if (mostRecentScreening.has("valueQuantity")) {
                    JsonNode valueQuantity = mostRecentScreening.get("valueQuantity");
                    if (valueQuantity.has("value")) {
                        mostRecentLeadLevel = valueQuantity.get("value").asDouble();
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not extract lead level: {}", e.getMessage());
            }

            // Check if screening was done by age 2
            LocalDate age2Date = birthDate.plusYears(2);
            hasScreeningByAge2 = !screeningDate.isAfter(age2Date);
        }

        // Calculate compliance based on age-specific requirements
        boolean meetsGoal = false;
        String complianceReason = "";

        if (needsScreeningByAge2) {
            meetsGoal = hasScreeningByAge2;
            complianceReason = hasScreeningByAge2 ?
                "Screened by age 2 (recommended)" :
                "Not screened by age 2";
        } else if (needsScreeningByAge6) {
            meetsGoal = hasAnyScreening;
            complianceReason = hasAnyScreening ?
                "High-risk child screened" :
                "High-risk child not screened";
        } else {
            // Child age 3-6 without high risk - screening recommended but not required
            meetsGoal = hasAnyScreening;
            complianceReason = hasAnyScreening ?
                "Screened (good preventive care)" :
                "No screening documented";
        }

        resultBuilder.inNumerator(meetsGoal);
        resultBuilder.complianceRate(meetsGoal ? 1.0 : 0.0);
        resultBuilder.score(meetsGoal ? 100.0 : 0.0);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!meetsGoal) {
            if (needsScreeningByAge2 && !hasScreeningByAge2) {
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("MISSING_LEAD_SCREENING_AGE_2")
                    .description(String.format("Child age %d has not been screened for lead exposure by age 2", age))
                    .recommendedAction("Order blood lead level test; AAP recommends universal screening at 12 and 24 months")
                    .priority("high")
                    .dueDate(LocalDate.now().plusMonths(1))
                    .build());
            } else if (needsScreeningByAge6 && !hasAnyScreening) {
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("MISSING_LEAD_SCREENING_HIGH_RISK")
                    .description("High-risk child without lead screening")
                    .recommendedAction("Order blood lead level test; high-risk factors identified")
                    .priority("high")
                    .dueDate(LocalDate.now().plusMonths(1))
                    .build());
            }
        } else if (mostRecentLeadLevel != null && mostRecentLeadLevel >= 5.0) {
            // Elevated lead level (CDC reference level is 3.5 µg/dL, but 5+ needs action)
            String severity = mostRecentLeadLevel >= 10.0 ? "High" :
                            mostRecentLeadLevel >= 5.0 ? "Moderate" : "Mild";

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("ELEVATED_BLOOD_LEAD_LEVEL")
                .description(String.format("%s blood lead level: %.1f µg/dL (reference <3.5)", severity, mostRecentLeadLevel))
                .recommendedAction("Follow CDC guidelines: identify lead source, nutritional counseling, repeat testing, consider chelation therapy if ≥45 µg/dL")
                .priority(mostRecentLeadLevel >= 10.0 ? "high" : "medium")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());
        }

        resultBuilder.careGaps(careGaps);

        String leadLevelStatus = "Unknown";
        if (mostRecentLeadLevel != null) {
            if (mostRecentLeadLevel < 3.5) {
                leadLevelStatus = String.format("Normal (%.1f µg/dL)", mostRecentLeadLevel);
            } else if (mostRecentLeadLevel < 5.0) {
                leadLevelStatus = String.format("Mild elevation (%.1f µg/dL)", mostRecentLeadLevel);
            } else if (mostRecentLeadLevel < 10.0) {
                leadLevelStatus = String.format("Moderate elevation (%.1f µg/dL)", mostRecentLeadLevel);
            } else {
                leadLevelStatus = String.format("High elevation (%.1f µg/dL)", mostRecentLeadLevel);
            }
        }

        resultBuilder.details(java.util.Map.of(
            "patientAge", age,
            "hasScreening", hasAnyScreening,
            "screeningDate", mostRecentScreeningDate != null ? mostRecentScreeningDate : "None",
            "leadLevel", leadLevelStatus,
            "hasHighRiskFactors", hasHighRiskFactors,
            "screenedByAge2", hasScreeningByAge2,
            "complianceReason", complianceReason
        ));

        resultBuilder.evidence(java.util.Map.of(
            "screeningCompleted", hasAnyScreening,
            "screenedByAge2", hasScreeningByAge2,
            "highRiskChild", hasHighRiskFactors,
            "leadLevelNormal", mostRecentLeadLevel != null && mostRecentLeadLevel < 5.0
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        // Must be child age 1-6
        Integer age = getPatientAge(patient);
        if (age == null || age < 1 || age > 6) {
            return false;
        }

        return true;
    }

    /**
     * Get patient birth date from FHIR Patient resource
     */
    private LocalDate getPatientBirthDate(JsonNode patient) {
        try {
            if (patient.has("birthDate")) {
                return LocalDate.parse(patient.get("birthDate").asText());
            }
        } catch (Exception e) {
            logger.warn("Could not extract patient birth date: {}", e.getMessage());
        }
        return LocalDate.now().minusYears(3); // Default fallback
    }
}
