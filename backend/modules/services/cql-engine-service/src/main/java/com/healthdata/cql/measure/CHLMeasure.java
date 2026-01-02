package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * CHL - Chlamydia Screening in Women (HEDIS)
 *
 * Evaluates whether sexually active women ages 16-24 were screened for chlamydia
 * during the measurement year.
 *
 * Chlamydia is the most common STI and can lead to pelvic inflammatory disease,
 * ectopic pregnancy, and infertility if untreated.
 *
 * USPSTF Grade A recommendation for screening sexually active women ≤24 years.
 */
@Component
public class CHLMeasure extends AbstractHedisMeasure {

    private static final List<String> CHLAMYDIA_SCREENING_CODES = Arrays.asList(
        // Laboratory tests
        "21613-5",   // LOINC - Chlamydia trachomatis DNA [Presence] in Urine
        "43304-5",   // LOINC - Chlamydia trachomatis rRNA [Presence] in Specimen
        "45084-1",   // LOINC - Chlamydia trachomatis DNA [Presence] in Vaginal fluid
        "50387-0",   // LOINC - Chlamydia trachomatis [Presence] in Cervix
        "6349-5",    // LOINC - Chlamydia trachomatis [Presence] in Specimen by Organism specific culture
        "23838-6",   // LOINC - Chlamydia trachomatis rRNA [Presence] in Genital specimen
        "42931-6",   // LOINC - Chlamydia trachomatis DNA [Presence] in Cervix
        "53925-4"    // LOINC - Chlamydia trachomatis rRNA [Presence] in Urine
    );

    private static final List<String> CHLAMYDIA_DIAGNOSIS_CODES = Arrays.asList(
        "240589008", // SNOMED - Chlamydial infection
        "105629000", // SNOMED - Chlamydial urethritis
        "266168003", // SNOMED - Chlamydial cervicitis
        "78692002",  // SNOMED - Chlamydial infection of lower genital tract
        "89058006"   // SNOMED - Chlamydia trachomatis infection
    );

    private static final List<String> SEXUAL_ACTIVITY_INDICATORS = Arrays.asList(
        // Contraception
        "169553002", // SNOMED - Oral contraceptive
        "312081001", // SNOMED - Intrauterine contraceptive device
        "454451000124107", // SNOMED - Long acting reversible contraception
        // Pregnancy/delivery
        "77386006",  // SNOMED - Pregnancy
        "177184002", // SNOMED - Normal delivery
        // STI history
        "8098009",   // SNOMED - Sexually transmitted disease
        // Gynecologic conditions
        "198130006"  // SNOMED - Female pelvic inflammatory disease
    );

    @Override
    public String getMeasureId() {
        return "CHL";
    }

    @Override
    public String getMeasureName() {
        return "Chlamydia Screening in Women";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'CHL-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating CHL measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be sexually active female age 16-24)")
                .build();
        }

        resultBuilder.inDenominator(true);

        JsonNode patient = getPatientData(tenantId, patientId);
        Integer age = getPatientAge(patient);

        // Check for chlamydia screening in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusYears(1).toString();
        JsonNode screenings = getObservations(tenantId, patientId,
            String.join(",", CHLAMYDIA_SCREENING_CODES), dateFilter);
        List<JsonNode> screeningEntries = getEntries(screenings);

        boolean hasScreening = !screeningEntries.isEmpty();
        String screeningDate = hasScreening ? getEffectiveDate(screeningEntries.get(0)) : null;

        // Check screening result if available
        String screeningResult = "Unknown";
        boolean screeningPositive = false;

        if (hasScreening) {
            JsonNode screening = screeningEntries.get(0);
            try {
                if (screening.has("valueCodeableConcept")) {
                    JsonNode valueCodeable = screening.get("valueCodeableConcept");
                    if (valueCodeable.has("coding")) {
                        JsonNode coding = valueCodeable.get("coding").get(0);
                        String code = coding.get("code").asText();
                        // Check for positive result codes
                        screeningPositive = code.contains("10828004") || // Positive
                                          code.contains("260373001");   // Detected
                        screeningResult = screeningPositive ? "Positive - detected" : "Negative";
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not extract screening result: {}", e.getMessage());
            }
        }

        // Check for chlamydia diagnosis (indicates past infection)
        JsonNode diagnoses = getConditions(tenantId, patientId,
            String.join(",", CHLAMYDIA_DIAGNOSIS_CODES));
        boolean hasChlamydiaDiagnosis = !getEntries(diagnoses).isEmpty();

        resultBuilder.inNumerator(hasScreening);
        resultBuilder.complianceRate(hasScreening ? 1.0 : 0.0);
        resultBuilder.score(hasScreening ? 100.0 : 0.0);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!hasScreening) {
            // Determine priority based on age (younger = higher priority)
            String priority = age <= 20 ? "high" : "medium";

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_CHLAMYDIA_SCREENING")
                .description(String.format("Sexually active woman age %d without annual chlamydia screening", age))
                .recommendedAction("Order chlamydia screening (NAAT urine or swab); USPSTF Grade A recommendation for women ≤24")
                .priority(priority)
                .dueDate(LocalDate.now().plusMonths(1))
                .build());

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("STI_PREVENTION_COUNSELING")
                .description("Opportunity for STI prevention counseling during screening visit")
                .recommendedAction("Discuss safer sex practices, condom use, and STI prevention")
                .priority("low")
                .dueDate(LocalDate.now().plusMonths(3))
                .build());
        } else if (screeningPositive) {
            // Positive screening requires treatment and partner notification
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("POSITIVE_CHLAMYDIA_TREATMENT")
                .description("Positive chlamydia screen - requires treatment and partner notification")
                .recommendedAction("Prescribe azithromycin 1g single dose OR doxycycline 100mg BID × 7 days; partner treatment; test of cure in 3 months")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(1))
                .build());
        }

        // Additional gap if history of chlamydia (higher risk for reinfection)
        if (hasChlamydiaDiagnosis) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("CHLAMYDIA_REINFECTION_RISK")
                .description("History of chlamydia infection - higher risk for reinfection")
                .recommendedAction("Rescreening recommended 3 months after treatment; enhanced STI prevention counseling")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(3))
                .build());
        }

        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "patientAge", age,
            "hasScreening", hasScreening,
            "screeningDate", screeningDate != null ? screeningDate : "None",
            "screeningResult", screeningResult,
            "screeningPositive", screeningPositive,
            "hasChlamydiaHistory", hasChlamydiaDiagnosis
        ));

        resultBuilder.evidence(java.util.Map.of(
            "screeningCompleted", hasScreening,
            "screeningNegative", hasScreening && !screeningPositive,
            "needsTreatment", screeningPositive
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

        // Must be age 16-24 (sexually active age range)
        Integer age = getPatientAge(patient);
        if (age == null || age < 16 || age > 24) {
            return false;
        }

        // Must have indicators of sexual activity
        // Check for pregnancy, contraception, STI history, or gynecologic conditions
        JsonNode sexualActivityIndicators = getConditions(tenantId, patientId,
            String.join(",", SEXUAL_ACTIVITY_INDICATORS));

        if (!getEntries(sexualActivityIndicators).isEmpty()) {
            return true;
        }

        // Also check for contraceptive medications
        JsonNode contraceptives = getMedicationRequests(tenantId, patientId,
            "1358,1359,1549", null); // RxNorm codes for common contraceptives

        return !getEntries(contraceptives).isEmpty();
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
