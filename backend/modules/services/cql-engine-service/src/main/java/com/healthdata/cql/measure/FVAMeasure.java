package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * FVA - Influenza Vaccinations for Adults Ages 18-64 (HEDIS)
 *
 * Evaluates whether adults ages 18-64 received an influenza vaccination during
 * the flu season (July 1 - March 31).
 *
 * Annual influenza vaccination is recommended by CDC for all adults to prevent
 * seasonal flu, reduce hospitalizations, and decrease flu-related mortality.
 *
 * High-priority populations:
 * - Pregnant women
 * - Adults with chronic conditions (diabetes, heart disease, asthma, COPD)
 * - Healthcare workers
 * - Caregivers of young children or elderly
 */
@Component
public class FVAMeasure extends AbstractHedisMeasure {

    private static final List<String> INFLUENZA_VACCINE_CODES = Arrays.asList(
        // CVX codes for influenza vaccines
        "140",       // CVX - Influenza, seasonal, injectable, preservative free
        "141",       // CVX - Influenza, seasonal, injectable
        "149",       // CVX - Influenza, live, intranasal
        "150",       // CVX - Influenza, injectable, quadrivalent, preservative free
        "153",       // CVX - Influenza, injectable, MDCK, preservative free
        "155",       // CVX - Influenza, recombinant, injectable, preservative free
        "158",       // CVX - Influenza, injectable, quadrivalent
        "161",       // CVX - Influenza, injectable, quadrivalent, preservative free, pediatric
        "166",       // CVX - Influenza, intradermal, quadrivalent, preservative free
        "168",       // CVX - Influenza, trivalent, adjuvanted
        "171",       // CVX - Influenza, injectable, MDCK, preservative free, quadrivalent
        "185",       // CVX - Influenza, recombinant, quadrivalent,preservative free
        "186",       // CVX - Influenza, injectable, MDCK, quadrivalent, preservative
        "197"        // CVX - Influenza, high-dose, quadrivalent
    );

    private static final List<String> HIGH_RISK_CONDITIONS = Arrays.asList(
        // Chronic conditions
        "44054006",  // SNOMED - Diabetes mellitus
        "195967001", // SNOMED - Asthma
        "13645005",  // SNOMED - Chronic obstructive pulmonary disease
        "49601007",  // SNOMED - Disorder of cardiovascular system
        "90708001",  // SNOMED - Kidney disease
        "235856003", // SNOMED - Liver disease
        // Immunocompromised
        "370388006", // SNOMED - Patient immunocompromised
        "86406008",  // SNOMED - HIV infection
        "363346000", // SNOMED - Malignant neoplastic disease
        // Pregnancy
        "77386006"   // SNOMED - Pregnancy
    );

    @Override
    public String getMeasureId() {
        return "FVA";
    }

    @Override
    public String getMeasureName() {
        return "Influenza Vaccinations for Adults Ages 18-64";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'FVA-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating FVA measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 18-64)")
                .build();
        }

        resultBuilder.inDenominator(true);

        JsonNode patient = getPatientData(tenantId, patientId);
        Integer age = getPatientAge(patient);

        // Calculate current flu season dates
        LocalDate today = LocalDate.now();
        LocalDate fluSeasonStart;
        LocalDate fluSeasonEnd;

        if (today.getMonthValue() >= 7) {
            // Current flu season (July 1 current year - March 31 next year)
            fluSeasonStart = LocalDate.of(today.getYear(), Month.JULY, 1);
            fluSeasonEnd = LocalDate.of(today.getYear() + 1, Month.MARCH, 31);
        } else {
            // Previous flu season (July 1 last year - March 31 current year)
            fluSeasonStart = LocalDate.of(today.getYear() - 1, Month.JULY, 1);
            fluSeasonEnd = LocalDate.of(today.getYear(), Month.MARCH, 31);
        }

        // Check for flu vaccination during flu season
        String dateFilter = "ge" + fluSeasonStart.toString() + "&date=le" + fluSeasonEnd.toString();
        JsonNode fluVaccinations = searchImmunizationsByCVX(tenantId, patientId,
            INFLUENZA_VACCINE_CODES, dateFilter);
        List<JsonNode> vaccineEntries = getEntries(fluVaccinations);

        boolean hasFluvaccination = !vaccineEntries.isEmpty();
        String vaccinationDate = hasFluvaccination ? getEffectiveDate(vaccineEntries.get(0)) : null;

        // Check for high-risk conditions (higher priority for vaccination)
        JsonNode highRiskConditions = getConditions(tenantId, patientId,
            String.join(",", HIGH_RISK_CONDITIONS));
        boolean hasHighRiskCondition = !getEntries(highRiskConditions).isEmpty();

        resultBuilder.inNumerator(hasFluvaccination);
        resultBuilder.complianceRate(hasFluvaccination ? 1.0 : 0.0);
        resultBuilder.score(hasFluvaccination ? 100.0 : 0.0);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!hasFluvaccination) {
            // Determine if still within flu season
            boolean inFluSeason = !today.isBefore(fluSeasonStart) && !today.isAfter(fluSeasonEnd);
            String priority = hasHighRiskCondition ? "high" : "medium";

            if (inFluSeason) {
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("MISSING_ANNUAL_FLU_VACCINE")
                    .description(String.format("Adult age %d without influenza vaccination for current flu season", age))
                    .recommendedAction("Administer influenza vaccine; CDC recommends annual vaccination for all adults")
                    .priority(priority)
                    .dueDate(fluSeasonEnd)
                    .build());
            } else {
                // After flu season ended - note for next year
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("MISSED_LAST_FLU_SEASON")
                    .description("Did not receive influenza vaccine during last flu season")
                    .recommendedAction("Ensure vaccination during next flu season (starting July); set reminder")
                    .priority(priority)
                    .dueDate(LocalDate.of(today.getYear(), Month.OCTOBER, 31))
                    .build());
            }

            if (hasHighRiskCondition) {
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("HIGH_RISK_PATIENT_UNVACCINATED")
                    .description("High-risk patient (chronic condition) without flu vaccination - increased risk for complications")
                    .recommendedAction("PRIORITY: Vaccinate as soon as possible; flu can cause serious complications in high-risk patients")
                    .priority("high")
                    .dueDate(inFluSeason ? LocalDate.now().plusWeeks(1) : fluSeasonEnd)
                    .build());
            }
        }

        resultBuilder.careGaps(careGaps);

        String vaccinationStatus = hasFluvaccination ?
            String.format("Vaccinated for %d-%d flu season", fluSeasonStart.getYear(), fluSeasonEnd.getYear()) :
            String.format("NOT vaccinated for %d-%d flu season", fluSeasonStart.getYear(), fluSeasonEnd.getYear());

        boolean inCurrentFluSeason = !today.isBefore(fluSeasonStart) && !today.isAfter(fluSeasonEnd);

        resultBuilder.details(java.util.Map.of(
            "patientAge", age,
            "hasFluvaccination", hasFluvaccination,
            "vaccinationDate", vaccinationDate != null ? vaccinationDate : "None",
            "vaccinationStatus", vaccinationStatus,
            "fluSeasonStart", fluSeasonStart.toString(),
            "fluSeasonEnd", fluSeasonEnd.toString(),
            "inFluSeason", inCurrentFluSeason,
            "hasHighRiskCondition", hasHighRiskCondition
        ));

        resultBuilder.evidence(java.util.Map.of(
            "receivedFluVaccine", hasFluvaccination,
            "vaccinatedDuringFluSeason", hasFluvaccination,
            "highRiskPatient", hasHighRiskCondition
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        // Must be age 18-64 (adults, excluding elderly who have separate measure)
        Integer age = getPatientAge(patient);
        if (age == null || age < 18 || age > 64) {
            return false;
        }

        return true;
    }

    /**
     * Search for immunizations by CVX vaccine codes
     */
    private JsonNode searchImmunizationsByCVX(String tenantId, UUID patientId,
                                               List<String> cvxCodes, String dateFilter) {
        try {
            // Convert CVX codes to vaccine code parameter for FHIR query
            String vaccineCodeParam = cvxCodes.stream()
                .map(code -> "http://hl7.org/fhir/sid/cvx|" + code)
                .collect(java.util.stream.Collectors.joining(","));

            String immunizationsJson = fhirClient.searchImmunizations(tenantId, patientId, vaccineCodeParam, dateFilter);
            return objectMapper.readTree(immunizationsJson);
        } catch (Exception e) {
            logger.error("Failed to search immunizations for patient {}: {}", patientId, e.getMessage());
            return objectMapper.createObjectNode();
        }
    }
}
