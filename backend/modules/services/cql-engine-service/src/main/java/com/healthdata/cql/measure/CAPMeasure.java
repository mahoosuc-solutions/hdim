package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * CAP - Children and Adolescents' Access to Primary Care Practitioners (HEDIS)
 *
 * Evaluates whether children and adolescents had at least one ambulatory or preventive
 * care visit with a PCP during the measurement year.
 *
 * Four age-based components:
 * - Ages 12-24 months (at least 1 visit)
 * - Ages 25 months - 6 years (at least 1 visit)
 * - Ages 7-11 years (at least 1 visit)
 * - Ages 12-19 years (at least 1 visit)
 *
 * Regular primary care access ensures timely preventive services, immunizations,
 * and management of acute/chronic conditions.
 */
@Component
public class CAPMeasure extends AbstractHedisMeasure {

    private static final List<String> PRIMARY_CARE_VISIT_CODES = Arrays.asList(
        // Office visits
        "185349003", // SNOMED - Encounter for check up
        "390906007", // SNOMED - Follow-up encounter
        "308335008", // SNOMED - Patient encounter procedure
        "185345009", // SNOMED - Encounter for symptom
        // Well-child visits
        "410620009", // SNOMED - Well child visit
        "390840006", // SNOMED - Well baby visit
        "170099002", // SNOMED - Child health examination
        // Preventive care
        "185288003", // SNOMED - Preventive medical examination
        "76464004",  // SNOMED - Health supervision of infant
        "170283000", // SNOMED - Health supervision of child
        // Outpatient visits
        "33879002",  // SNOMED - Administration of vaccine
        "185317003", // SNOMED - Telephone encounter
        "185465003"  // SNOMED - Virtual encounter
    );

    private static final List<String> PCP_SPECIALTY_CODES = Arrays.asList(
        "419772000", // SNOMED - Family practice
        "394802001", // SNOMED - General medicine
        "408443003", // SNOMED - General medical practice
        "394537008", // SNOMED - Pediatric medicine
        "408459003"  // SNOMED - Pediatric specialty
    );

    @Override
    public String getMeasureId() {
        return "CAP";
    }

    @Override
    public String getMeasureName() {
        return "Children and Adolescents' Access to Primary Care Practitioners";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'CAP-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating CAP measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be child/adolescent age 0-19)")
                .build();
        }

        resultBuilder.inDenominator(true);

        JsonNode patient = getPatientData(tenantId, patientId);
        Integer age = getPatientAge(patient);

        // Check for primary care visits in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusYears(1).toString();
        JsonNode primaryCareVisits = getEncounters(tenantId, patientId,
            String.join(",", PRIMARY_CARE_VISIT_CODES), dateFilter);
        List<JsonNode> visitEntries = getEntries(primaryCareVisits);

        boolean hasVisit = !visitEntries.isEmpty();
        int visitCount = visitEntries.size();
        String mostRecentVisitDate = hasVisit ? getEffectiveDate(visitEntries.get(0)) : null;

        // Determine expected visit frequency by age group
        String ageGroup;
        int recommendedVisits;

        if (age <= 2) {
            // Ages 12-24 months
            ageGroup = "12-24 months";
            recommendedVisits = 3; // More frequent for young children
        } else if (age <= 6) {
            // Ages 25 months - 6 years
            ageGroup = "25 months - 6 years";
            recommendedVisits = 1;
        } else if (age <= 11) {
            // Ages 7-11 years
            ageGroup = "7-11 years";
            recommendedVisits = 1;
        } else {
            // Ages 12-19 years
            ageGroup = "12-19 years";
            recommendedVisits = 1;
        }

        boolean meetsVisitRequirement = hasVisit;

        resultBuilder.inNumerator(meetsVisitRequirement);
        resultBuilder.complianceRate(meetsVisitRequirement ? 1.0 : 0.0);
        resultBuilder.score(meetsVisitRequirement ? 100.0 : 0.0);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!hasVisit) {
            String priority = age <= 6 ? "high" : "medium";

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_PRIMARY_CARE_ACCESS")
                .description(String.format("Child/adolescent age %d (%s) without primary care visit in last year", age, ageGroup))
                .recommendedAction("Schedule wellness visit with PCP; ensure up-to-date immunizations; assess developmental milestones; provide anticipatory guidance")
                .priority(priority)
                .dueDate(LocalDate.now().plusMonths(1))
                .build());

            if (age <= 2) {
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("MISSED_EARLY_CHILDHOOD_MONITORING")
                    .description("Infant/toddler without regular well-child visits - critical period for development")
                    .recommendedAction("Schedule catch-up visits; assess growth, development, nutrition; complete age-appropriate immunizations")
                    .priority("high")
                    .dueDate(LocalDate.now().plusWeeks(2))
                    .build());
            }

            if (age >= 12 && age <= 19) {
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("MISSED_ADOLESCENT_PREVENTIVE_VISIT")
                    .description("Adolescent without annual wellness visit - important for preventive screening and counseling")
                    .recommendedAction("Schedule adolescent wellness visit; provide confidential screening (depression, substance use, sexual health)")
                    .priority("medium")
                    .dueDate(LocalDate.now().plusMonths(2))
                    .build());
            }
        } else if (visitCount < recommendedVisits && age <= 2) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("INSUFFICIENT_WELL_CHILD_VISITS")
                .description(String.format("Only %d visit(s) in last year (recommend %d for age %d months)", visitCount, recommendedVisits, age))
                .recommendedAction("Schedule additional well-child visits per AAP Bright Futures guidelines")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }

        resultBuilder.careGaps(careGaps);

        String accessStatus = "Unknown";
        if (hasVisit) {
            if (visitCount >= recommendedVisits) {
                accessStatus = String.format("Excellent access (%d visits)", visitCount);
            } else {
                accessStatus = String.format("Adequate access (%d visit)", visitCount);
            }
        } else {
            accessStatus = "No primary care access in last year";
        }

        resultBuilder.details(java.util.Map.of(
            "patientAge", age,
            "ageGroup", ageGroup,
            "hasVisit", hasVisit,
            "visitCount", visitCount,
            "mostRecentVisitDate", mostRecentVisitDate != null ? mostRecentVisitDate : "None",
            "recommendedVisits", recommendedVisits,
            "accessStatus", accessStatus
        ));

        resultBuilder.evidence(java.util.Map.of(
            "primaryCareAccessConfirmed", hasVisit,
            "meetsVisitRequirement", meetsVisitRequirement,
            "adequateVisitFrequency", visitCount >= recommendedVisits
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        // Must be age 1-19 years (children and adolescents)
        Integer age = getPatientAge(patient);
        if (age == null || age < 1 || age > 19) {
            return false;
        }

        return true;
    }
}
