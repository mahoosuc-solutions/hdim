package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AIS - Adult Immunization Status (19+)
 *
 * CMS Measure ID: CMS147v12
 * APP Plus 2028: Required measure for ACO performance
 *
 * Measure Description:
 * Percentage of adults 19 years of age and older who are up-to-date
 * on recommended routine vaccines:
 *
 * - Influenza: Annual vaccination (all adults 19+)
 * - Td/Tdap: Every 10 years (all adults 19+)
 * - Zoster: Two-dose series (adults 50+)
 * - Pneumococcal: One-time vaccination (adults 65+)
 *
 * Eligible Population:
 * - Ages 19 years and older at the start of the measurement period
 * - With at least one qualifying encounter during the measurement period
 *
 * Numerator:
 * - Patients with documented immunization or valid contraindication/refusal
 *
 * Denominator Exclusions:
 * - Hospice care
 * - Documented allergy or contraindication to specific vaccine
 */
@Component
public class AISMeasure extends AbstractHedisMeasure {

    // CVX codes for Influenza vaccines
    private static final List<String> INFLUENZA_CVX_CODES = Arrays.asList(
        "140",  // Influenza, seasonal, injectable, preservative free
        "141",  // Influenza, seasonal, injectable
        "144",  // Seasonal influenza, intradermal, preservative free
        "149",  // Influenza, live, intranasal, quadrivalent
        "150",  // Influenza, injectable, quadrivalent, preservative free
        "153",  // Influenza, injectable, Madin Darby Canine Kidney, preservative free
        "155",  // Influenza, recombinant, injectable, preservative free
        "158",  // Influenza, injectable, quadrivalent
        "161",  // Influenza, injectable, quadrivalent, preservative free, pediatric
        "166",  // Influenza, intradermal, quadrivalent, preservative free
        "168",  // Influenza, trivalent, adjuvanted
        "171",  // Influenza, injectable, MDCK, preservative free, quadrivalent
        "185",  // Influenza, recombinant, quadrivalent, injectable, preservative free
        "186",  // Influenza, injectable, MDCK, quadrivalent, preservative
        "197",  // Influenza, high-dose seasonal, quadrivalent
        "205"   // Influenza vaccine, quadrivalent, adjuvanted
    );

    // CVX codes for Td/Tdap vaccines
    private static final List<String> TDAP_CVX_CODES = Arrays.asList(
        "09",   // Td (tetanus and diphtheria toxoids)
        "113",  // Td (adult), preservative free
        "115",  // Tdap
        "138",  // Td (adult)
        "139",  // Td(adult) unspecified formulation
        "196"   // Td, absorbed, preservative free, adult use
    );

    // CVX codes for Zoster (Shingles) vaccines
    private static final List<String> ZOSTER_CVX_CODES = Arrays.asList(
        "121",  // Zoster vaccine, live
        "187",  // Zoster vaccine recombinant (Shingrix)
        "188"   // Zoster vaccine, recombinant (unspecified formulation)
    );

    // CVX codes for Pneumococcal vaccines
    private static final List<String> PNEUMOCOCCAL_CVX_CODES = Arrays.asList(
        "33",   // Pneumococcal polysaccharide PPV23
        "100",  // Pneumococcal conjugate PCV 7
        "109",  // Pneumococcal, unspecified formulation
        "133",  // Pneumococcal conjugate PCV 13
        "152",  // Pneumococcal conjugate PCV 10
        "215",  // Pneumococcal conjugate PCV 15
        "216"   // Pneumococcal conjugate PCV 20
    );

    // All vaccine CVX codes combined
    private static final List<String> ALL_VACCINE_CODES;
    static {
        ALL_VACCINE_CODES = new ArrayList<>();
        ALL_VACCINE_CODES.addAll(INFLUENZA_CVX_CODES);
        ALL_VACCINE_CODES.addAll(TDAP_CVX_CODES);
        ALL_VACCINE_CODES.addAll(ZOSTER_CVX_CODES);
        ALL_VACCINE_CODES.addAll(PNEUMOCOCCAL_CVX_CODES);
    }

    // SNOMED CT codes for vaccine-related allergies/contraindications
    private static final List<String> VACCINE_CONTRAINDICATION_CODES = Arrays.asList(
        "293104008", // Adverse reaction to vaccine
        "91930004",  // Allergy to eggs
        "294466006", // Anaphylactic reaction to vaccine
        "140587009", // Allergy to influenza vaccine
        "294468007"  // Anaphylaxis caused by vaccine
    );

    @Override
    public String getMeasureId() {
        return "AIS";
    }

    @Override
    public String getMeasureName() {
        return "Adult Immunization Status";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'AIS-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating AIS measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        // Get patient age
        JsonNode patient = getPatientData(tenantId, patientId);
        Integer age = getPatientAge(patient);

        // Check eligibility (must be 19+)
        if (age == null || age < 19) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient is under 19 years of age")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for hospice exclusion
        if (isInHospice(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(true)
                .inNumerator(false)
                .exclusionReason("Patient is in hospice care")
                .build();
        }

        // Evaluate each vaccine component based on age
        List<MeasureResult.CareGap> careGaps = new ArrayList<>();
        int requiredVaccines = 0;
        int completedVaccines = 0;

        // Influenza - required for all 19+
        requiredVaccines++;
        boolean hasInfluenza = evaluateInfluenzaStatus(tenantId, patientId, resultBuilder, careGaps);
        if (hasInfluenza) completedVaccines++;

        // Td/Tdap - required for all 19+
        requiredVaccines++;
        boolean hasTdap = evaluateTdapStatus(tenantId, patientId, resultBuilder, careGaps);
        if (hasTdap) completedVaccines++;

        // Zoster - required for 50+
        if (age >= 50) {
            requiredVaccines++;
            boolean hasZoster = evaluateZosterStatus(tenantId, patientId, resultBuilder, careGaps);
            if (hasZoster) completedVaccines++;
        }

        // Pneumococcal - required for 65+
        if (age >= 65) {
            requiredVaccines++;
            boolean hasPneumococcal = evaluatePneumococcalStatus(tenantId, patientId, resultBuilder, careGaps);
            if (hasPneumococcal) completedVaccines++;
        }

        // Set care gaps
        if (!careGaps.isEmpty()) {
            resultBuilder.careGaps(careGaps);
        }

        // Calculate compliance
        double complianceRate = (double) completedVaccines / requiredVaccines;
        boolean inNumerator = completedVaccines == requiredVaccines;

        resultBuilder
            .inNumerator(inNumerator)
            .complianceRate(complianceRate)
            .score(complianceRate * 100)
            .details(java.util.Map.of(
                "patientAge", age,
                "requiredVaccines", requiredVaccines,
                "completedVaccines", completedVaccines,
                "hasInfluenza", hasInfluenza,
                "hasTdap", hasTdap,
                "hasZoster", age >= 50,
                "hasPneumococcal", age >= 65
            ));

        MeasureResult result = resultBuilder.build();
        logger.info("AIS evaluation complete for patient {}: score={}, inNumerator={}",
            patientId, result.getScore(), result.isInNumerator());

        return result;
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        Integer age = getPatientAge(patient);
        return age != null && age >= 19;
    }

    /**
     * Check if patient has had influenza vaccine within the last year.
     */
    private boolean evaluateInfluenzaStatus(String tenantId, String patientId,
                                            MeasureResult.MeasureResultBuilder resultBuilder,
                                            List<MeasureResult.CareGap> careGaps) {
        // Look for flu shot in last 12 months (flu season)
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();

        JsonNode immunizations = getImmunizations(tenantId, patientId,
            String.join(",", INFLUENZA_CVX_CODES), dateFilter);

        List<JsonNode> fluShots = getEntries(immunizations);

        if (fluShots.isEmpty()) {
            // Check for contraindication
            if (hasContraindication(tenantId, patientId, "influenza")) {
                resultBuilder.evidence(java.util.Map.of("influenzaContraindication", true));
                return true; // Excluded from numerator requirement
            }

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_INFLUENZA_VACCINE")
                .description("No influenza vaccine documented in the past 12 months")
                .recommendedAction("Administer annual influenza vaccine")
                .priority("high")
                .dueDate(LocalDate.now().plusDays(30))
                .build());
            return false;
        }

        String lastFluDate = getEffectiveDate(fluShots.get(0));
        resultBuilder.evidence(java.util.Map.of("lastInfluenzaVaccine", lastFluDate));
        return true;
    }

    /**
     * Check if patient has had Td/Tdap vaccine within the last 10 years.
     */
    private boolean evaluateTdapStatus(String tenantId, String patientId,
                                       MeasureResult.MeasureResultBuilder resultBuilder,
                                       List<MeasureResult.CareGap> careGaps) {
        // Look for Td/Tdap in last 10 years
        String dateFilter = "ge" + LocalDate.now().minusYears(10).toString();

        JsonNode immunizations = getImmunizations(tenantId, patientId,
            String.join(",", TDAP_CVX_CODES), dateFilter);

        List<JsonNode> tdapShots = getEntries(immunizations);

        if (tdapShots.isEmpty()) {
            // Check for contraindication
            if (hasContraindication(tenantId, patientId, "tdap")) {
                resultBuilder.evidence(java.util.Map.of("tdapContraindication", true));
                return true;
            }

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_TDAP_VACCINE")
                .description("No Td/Tdap vaccine documented in the past 10 years")
                .recommendedAction("Administer Td or Tdap vaccine")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(3))
                .build());
            return false;
        }

        String lastTdapDate = getEffectiveDate(tdapShots.get(0));
        resultBuilder.evidence(java.util.Map.of("lastTdapVaccine", lastTdapDate));
        return true;
    }

    /**
     * Check if patient (50+) has completed zoster vaccine series.
     */
    private boolean evaluateZosterStatus(String tenantId, String patientId,
                                         MeasureResult.MeasureResultBuilder resultBuilder,
                                         List<MeasureResult.CareGap> careGaps) {
        // Look for zoster vaccine (Shingrix is 2-dose series)
        JsonNode immunizations = getImmunizations(tenantId, patientId,
            String.join(",", ZOSTER_CVX_CODES), null);

        List<JsonNode> zosterShots = getEntries(immunizations);

        // Shingrix requires 2 doses
        int zosterDoses = zosterShots.size();

        if (zosterDoses < 2) {
            // Check for contraindication
            if (hasContraindication(tenantId, patientId, "zoster")) {
                resultBuilder.evidence(java.util.Map.of("zosterContraindication", true));
                return true;
            }

            String description = zosterDoses == 0
                ? "No zoster (shingles) vaccine documented"
                : "Only 1 of 2 required zoster vaccine doses documented";

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("INCOMPLETE_ZOSTER_SERIES")
                .description(description)
                .recommendedAction(zosterDoses == 0
                    ? "Initiate Shingrix 2-dose series"
                    : "Administer second dose of Shingrix")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(2))
                .build());
            return false;
        }

        resultBuilder.evidence(java.util.Map.of("zosterDosesCompleted", zosterDoses));
        return true;
    }

    /**
     * Check if patient (65+) has received pneumococcal vaccine.
     */
    private boolean evaluatePneumococcalStatus(String tenantId, String patientId,
                                               MeasureResult.MeasureResultBuilder resultBuilder,
                                               List<MeasureResult.CareGap> careGaps) {
        // Look for any pneumococcal vaccine
        JsonNode immunizations = getImmunizations(tenantId, patientId,
            String.join(",", PNEUMOCOCCAL_CVX_CODES), null);

        List<JsonNode> pneumoShots = getEntries(immunizations);

        if (pneumoShots.isEmpty()) {
            // Check for contraindication
            if (hasContraindication(tenantId, patientId, "pneumococcal")) {
                resultBuilder.evidence(java.util.Map.of("pneumococcalContraindication", true));
                return true;
            }

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_PNEUMOCOCCAL_VACCINE")
                .description("No pneumococcal vaccine documented")
                .recommendedAction("Administer PCV20 or PCV15 followed by PPSV23")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
            return false;
        }

        String lastPneumoDate = getEffectiveDate(pneumoShots.get(0));
        resultBuilder.evidence(java.util.Map.of("lastPneumococcalVaccine", lastPneumoDate));
        return true;
    }

    /**
     * Check if patient is in hospice care.
     */
    private boolean isInHospice(String tenantId, String patientId) {
        // Check for hospice encounter or service
        JsonNode encounters = getEncounters(tenantId, patientId, "183919006", null); // SNOMED hospice
        return !getEntries(encounters).isEmpty();
    }

    /**
     * Check for vaccine contraindication or documented refusal.
     */
    private boolean hasContraindication(String tenantId, String patientId, String vaccineType) {
        JsonNode conditions = getConditions(tenantId, patientId, null);
        List<JsonNode> conditionEntries = getEntries(conditions);

        for (JsonNode condition : conditionEntries) {
            if (hasCode(condition, VACCINE_CONTRAINDICATION_CODES)) {
                // Check if specific to vaccine type based on condition text
                String display = "";
                if (condition.has("code") && condition.get("code").has("text")) {
                    display = condition.get("code").get("text").asText().toLowerCase();
                }
                if (display.contains(vaccineType) || display.contains("vaccine")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get immunizations for a patient.
     */
    protected JsonNode getImmunizations(String tenantId, String patientId, String codes, String date) {
        try {
            // Use FHIR client to search immunizations
            // In real implementation, this would call the FHIR service
            String immunizationsJson = fhirClient.searchImmunizations(tenantId, patientId, codes, date);
            return objectMapper.readTree(immunizationsJson);
        } catch (Exception e) {
            logger.error("Error fetching immunizations for patient {}: {}", patientId, e.getMessage());
            return null;
        }
    }
}
