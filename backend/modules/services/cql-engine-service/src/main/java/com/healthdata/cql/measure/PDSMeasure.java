package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * PDS - Postpartum Depression Screening and Follow-Up (HEDIS)
 *
 * Evaluates whether women who delivered a live birth were screened for
 * postpartum depression using a standardized tool.
 *
 * Two components:
 * - Screening: Depression screening within 12 weeks postpartum
 * - Follow-up: If positive screen, follow-up or treatment documented
 */
@Component
public class PDSMeasure extends AbstractHedisMeasure {

    private static final List<String> DELIVERY_CODES = Arrays.asList(
        "177184002", // SNOMED - Normal delivery (procedure)
        "236974004", // SNOMED - Delivery of live birth
        "3950001",   // SNOMED - Birth
        "236973005", // SNOMED - Delivery procedure
        "289258003", // SNOMED - Finding of delivery
        "33552000",  // SNOMED - Vaginal delivery
        "11466000"   // SNOMED - Cesarean section
    );

    private static final List<String> DEPRESSION_SCREENING_CODES = Arrays.asList(
        "44261-6",   // LOINC - Patient Health Questionnaire 9 item (PHQ-9)
        "55758-7",   // LOINC - Patient Health Questionnaire 2 item (PHQ-2)
        "89209-1",   // LOINC - Edinburgh Postnatal Depression Scale (EPDS)
        "73831-0",   // LOINC - Generalized anxiety disorder 7 item (GAD-7)
        "89204-2"    // LOINC - Postpartum Depression Screening Scale (PDSS)
    );

    private static final List<String> POSTPARTUM_DEPRESSION_CODES = Arrays.asList(
        "191589009", // SNOMED - Postpartum depression
        "428216004", // SNOMED - Postnatal depression
        "309465005", // SNOMED - Postpartum mood disturbance
        "15183003"   // SNOMED - Postpartum psychosis
    );

    private static final List<String> MENTAL_HEALTH_FOLLOW_UP_CODES = Arrays.asList(
        "76168009",  // SNOMED - Psychiatric diagnostic evaluation
        "313234004", // SNOMED - Mental health counseling
        "225337009", // SNOMED - Psychotherapy
        "390906007", // SNOMED - Follow-up encounter
        "185317003"  // SNOMED - Telephone encounter
    );

    private static final int POSTPARTUM_SCREENING_WINDOW_WEEKS = 12;

    @Override
    public String getMeasureId() {
        return "PDS";
    }

    @Override
    public String getMeasureName() {
        return "Postpartum Depression Screening and Follow-Up";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'PDS-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating PDS measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be female with delivery in last 12 months)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Find delivery date
        String deliveryDateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode deliveries = getEncounters(tenantId, patientId,
            String.join(",", DELIVERY_CODES), deliveryDateFilter);
        List<JsonNode> deliveryEntries = getEntries(deliveries);

        if (deliveryEntries.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No delivery found in last 12 months")
                .build();
        }

        // Get delivery date
        JsonNode mostRecentDelivery = deliveryEntries.get(0);
        String deliveryDateStr = getEffectiveDate(mostRecentDelivery);
        LocalDate deliveryDate = LocalDate.parse(deliveryDateStr);

        // Calculate screening window (0-12 weeks postpartum)
        LocalDate screeningWindowEnd = deliveryDate.plusWeeks(POSTPARTUM_SCREENING_WINDOW_WEEKS);

        // Check for depression screening within window
        String screeningDateFilter = "ge" + deliveryDate.toString() +
                                    "&date=le" + screeningWindowEnd.toString();
        JsonNode depressionScreenings = getObservations(tenantId, patientId,
            String.join(",", DEPRESSION_SCREENING_CODES), screeningDateFilter);
        List<JsonNode> screeningEntries = getEntries(depressionScreenings);

        boolean hasScreening = !screeningEntries.isEmpty();
        String screeningDate = hasScreening ? getEffectiveDate(screeningEntries.get(0)) : null;

        // Extract screening result if available
        Integer screeningScore = null;
        boolean screeningPositive = false;

        if (hasScreening) {
            JsonNode screening = screeningEntries.get(0);
            try {
                if (screening.has("valueQuantity")) {
                    JsonNode valueQuantity = screening.get("valueQuantity");
                    if (valueQuantity.has("value")) {
                        screeningScore = valueQuantity.get("value").asInt();
                        // PHQ-9 ≥10 = moderate-severe depression
                        // PHQ-2 ≥3 = positive screen
                        // EPDS ≥10 = likely depression
                        screeningPositive = screeningScore >= 10;
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not extract screening score: {}", e.getMessage());
            }
        }

        // If positive screen, check for follow-up
        boolean hasFollowUp = false;
        if (hasScreening && screeningPositive) {
            // Check for mental health follow-up or treatment
            JsonNode followUps = getEncounters(tenantId, patientId,
                String.join(",", MENTAL_HEALTH_FOLLOW_UP_CODES), screeningDateFilter);
            hasFollowUp = !getEntries(followUps).isEmpty();

            // Also check for postpartum depression diagnosis (indicates clinical recognition)
            JsonNode ppdDiagnosis = getConditions(tenantId, patientId,
                String.join(",", POSTPARTUM_DEPRESSION_CODES));
            hasFollowUp = hasFollowUp || !getEntries(ppdDiagnosis).isEmpty();
        }

        // Calculate compliance
        int componentsCompleted = 0;
        if (hasScreening) componentsCompleted++;
        if (!screeningPositive || hasFollowUp) componentsCompleted++;  // Follow-up only required if positive

        double complianceRate = componentsCompleted / 2.0;

        // For numerator: screening completed (and follow-up if positive)
        boolean meetsGoal = hasScreening && (!screeningPositive || hasFollowUp);

        resultBuilder.inNumerator(meetsGoal);
        resultBuilder.complianceRate(complianceRate);
        resultBuilder.score(complianceRate * 100);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!hasScreening) {
            boolean isOverdue = LocalDate.now().isAfter(screeningWindowEnd);
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_POSTPARTUM_DEPRESSION_SCREENING")
                .description(String.format("No postpartum depression screening within 12 weeks of delivery (%s)", deliveryDateStr))
                .recommendedAction("Screen for postpartum depression using validated tool (EPDS, PHQ-9) at postpartum visit")
                .priority("high")
                .dueDate(isOverdue ? LocalDate.now().plusWeeks(1) : screeningWindowEnd)
                .build());
        } else if (screeningPositive && !hasFollowUp) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("POSITIVE_PPD_SCREEN_NO_FOLLOWUP")
                .description(String.format("Positive postpartum depression screen (score %d) without documented follow-up",
                    screeningScore != null ? screeningScore : 0))
                .recommendedAction("Provide mental health referral, follow-up appointment, or initiate treatment for postpartum depression")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(1))
                .build());
        }

        resultBuilder.careGaps(careGaps);

        String screeningResult = screeningScore != null ?
            (screeningPositive ? String.format("Positive (score %d)", screeningScore) :
                               String.format("Negative (score %d)", screeningScore)) :
            "Unknown";

        resultBuilder.details(java.util.Map.of(
            "deliveryDate", deliveryDateStr,
            "screeningWindowEnd", screeningWindowEnd.toString(),
            "hasScreening", hasScreening,
            "screeningDate", screeningDate != null ? screeningDate : "None",
            "screeningScore", screeningScore != null ? screeningScore : "Not available",
            "screeningResult", screeningResult,
            "screeningPositive", screeningPositive,
            "hasFollowUp", hasFollowUp,
            "meetsGoal", meetsGoal
        ));

        resultBuilder.evidence(java.util.Map.of(
            "screeningCompleted", hasScreening,
            "screeningPositive", screeningPositive,
            "followUpProvided", screeningPositive ? hasFollowUp : "Not required"
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        // Must be female
        String gender = getPatientGender(patient);
        if (!"female".equalsIgnoreCase(gender)) {
            return false;
        }

        // Must be reproductive age (typically 12-50)
        Integer age = getPatientAge(patient);
        if (age == null || age < 12 || age > 50) {
            return false;
        }

        // Must have delivery in last 12 months
        String deliveryDateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode deliveries = getEncounters(tenantId, patientId,
            String.join(",", DELIVERY_CODES), deliveryDateFilter);

        return !getEntries(deliveries).isEmpty();
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
