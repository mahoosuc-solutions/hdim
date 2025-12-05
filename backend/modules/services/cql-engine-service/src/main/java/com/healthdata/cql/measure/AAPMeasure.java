package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * AAP - Adults' Access to Preventive/Ambulatory Health Services (HEDIS)
 *
 * Evaluates whether adults had an ambulatory or preventive care visit during the measurement year.
 * This measure assesses healthcare utilization and access to primary care services.
 *
 * Age groups:
 * - 20-44 years
 * - 45-64 years
 * - 65+ years
 */
@Component
public class AAPMeasure extends AbstractHedisMeasure {

    private static final List<String> AMBULATORY_VISIT_CODES = Arrays.asList(
        "185349003", // Encounter for check up (SNOMED)
        "185345009", // Encounter for symptom (SNOMED)
        "439740005", // Postoperative follow-up visit (SNOMED)
        "185463005", // Visit out of hours (SNOMED)
        "185465003", // Acute illness visit (SNOMED)
        "308335008", // Patient encounter procedure (SNOMED)
        "390906007", // Follow-up encounter (SNOMED)
        "406547006"  // Urgent care clinic (SNOMED)
    );

    private static final List<String> PREVENTIVE_VISIT_CODES = Arrays.asList(
        "410620009", // Well adult visit (SNOMED)
        "444971000124105", // Annual wellness visit (SNOMED)
        "456201000124103", // Comprehensive preventive medicine (SNOMED)
        "738751004"  // Preventive care encounter (SNOMED)
    );

    private static final List<String> PRIMARY_CARE_VISIT_CODES = Arrays.asList(
        "185389009", // Follow-up visit (SNOMED)
        "30346009",  // Evaluation and management (SNOMED)
        "185347006"  // Encounter for problem (SNOMED)
    );

    private static final List<String> TELEHEALTH_CODES = Arrays.asList(
        "185317003", // Telephone encounter (SNOMED)
        "448337001", // Telemedicine consultation (SNOMED)
        "308720009"  // Letter encounter (SNOMED)
    );

    @Override
    public String getMeasureId() {
        return "AAP";
    }

    @Override
    public String getMeasureName() {
        return "Adults' Access to Preventive/Ambulatory Health Services";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'AAP-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating AAP measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 20+)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for any qualifying visit in the last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();

        // Check ambulatory visits
        JsonNode ambulatoryVisits = getEncounters(tenantId, patientId,
            String.join(",", AMBULATORY_VISIT_CODES), dateFilter);
        boolean hasAmbulatoryVisit = !getEntries(ambulatoryVisits).isEmpty();

        // Check preventive visits
        JsonNode preventiveVisits = getEncounters(tenantId, patientId,
            String.join(",", PREVENTIVE_VISIT_CODES), dateFilter);
        boolean hasPreventiveVisit = !getEntries(preventiveVisits).isEmpty();

        // Check primary care visits
        JsonNode primaryCareVisits = getEncounters(tenantId, patientId,
            String.join(",", PRIMARY_CARE_VISIT_CODES), dateFilter);
        boolean hasPrimaryCareVisit = !getEntries(primaryCareVisits).isEmpty();

        // Check telehealth visits
        JsonNode telehealthVisits = getEncounters(tenantId, patientId,
            String.join(",", TELEHEALTH_CODES), dateFilter);
        boolean hasTelehealthVisit = !getEntries(telehealthVisits).isEmpty();

        // Patient is in numerator if they had ANY qualifying visit
        boolean hasVisit = hasAmbulatoryVisit || hasPreventiveVisit ||
                          hasPrimaryCareVisit || hasTelehealthVisit;

        resultBuilder.inNumerator(hasVisit);
        resultBuilder.complianceRate(hasVisit ? 1.0 : 0.0);
        resultBuilder.score(hasVisit ? 100.0 : 0.0);

        if (hasVisit) {
            // Determine visit type for evidence
            String visitType = hasPreventiveVisit ? "Preventive" :
                              hasAmbulatoryVisit ? "Ambulatory" :
                              hasPrimaryCareVisit ? "Primary Care" : "Telehealth";

            JsonNode visits = hasPreventiveVisit ? preventiveVisits :
                             hasAmbulatoryVisit ? ambulatoryVisits :
                             hasPrimaryCareVisit ? primaryCareVisits : telehealthVisits;

            String lastVisitDate = getEffectiveDate(getEntries(visits).get(0));

            resultBuilder.evidence(java.util.Map.of(
                "hadVisit", true,
                "visitType", visitType,
                "lastVisitDate", lastVisitDate,
                "totalVisits", getEntries(visits).size()
            ));

            resultBuilder.details(java.util.Map.of(
                "hadAmbulatoryOrPreventiveVisit", true,
                "visitType", visitType,
                "lastVisitDate", lastVisitDate,
                "hasAmbulatoryVisit", hasAmbulatoryVisit,
                "hasPreventiveVisit", hasPreventiveVisit,
                "hasPrimaryCareVisit", hasPrimaryCareVisit,
                "hasTelehealthVisit", hasTelehealthVisit
            ));
        } else {
            // Identify care gap
            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("MISSING_ANNUAL_VISIT")
                    .description("No ambulatory or preventive care visit in the last 12 months")
                    .recommendedAction("Schedule annual wellness visit or primary care appointment")
                    .priority("medium")
                    .dueDate(LocalDate.now().plusMonths(1))
                    .build()
            ));

            resultBuilder.details(java.util.Map.of(
                "hadAmbulatoryOrPreventiveVisit", false
            ));
        }

        // Add patient age group for stratification
        JsonNode patient = getPatientData(tenantId, patientId);
        Integer age = getPatientAge(patient);
        String ageGroup = getAgeGroup(age);
        resultBuilder.evidence(java.util.Map.of(
            "ageGroup", ageGroup,
            "age", age
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        // Eligible for adults aged 20 and older
        return age != null && age >= 20;
    }

    /**
     * Determine age group for stratification
     */
    private String getAgeGroup(Integer age) {
        if (age == null) return "Unknown";
        if (age >= 20 && age <= 44) return "20-44";
        if (age >= 45 && age <= 64) return "45-64";
        if (age >= 65) return "65+";
        return "Unknown";
    }
}
