package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * COA - Care for Older Adults (HEDIS)
 *
 * Evaluates whether older adults (65+) received comprehensive geriatric care
 * assessments in key functional and health domains.
 *
 * Three components:
 * 1. Medication Review - Annual review and reconciliation of all medications
 * 2. Functional Status Assessment - ADLs, mobility, fall risk
 * 3. Pain Screening - Assessment and management of chronic pain
 *
 * Comprehensive geriatric assessment improves outcomes and prevents adverse events.
 */
@Component
public class COAMeasure extends AbstractHedisMeasure {

    private static final List<String> MEDICATION_REVIEW_CODES = Arrays.asList(
        "428191000124101", // SNOMED - Documentation of current medications
        "182777000",       // SNOMED - Medication review
        "430193006",       // SNOMED - Medication reconciliation
        "314530008",       // SNOMED - Drug therapy review
        "11291-0"          // LOINC - Medication list
    );

    private static final List<String> FUNCTIONAL_ASSESSMENT_CODES = Arrays.asList(
        // ADL assessments
        "83239000",  // SNOMED - Activities of daily living assessment
        "273529006", // SNOMED - Functional assessment
        "225388002", // SNOMED - Walking ability
        "284545001", // SNOMED - Ability to perform personal care
        // Fall risk
        "129839007", // SNOMED - At risk for falls
        "225367005", // SNOMED - Falls assessment
        "52552008",  // SNOMED - Falls risk assessment
        // Mobility
        "225369008", // SNOMED - Mobility assessment
        "386584001"  // SNOMED - Gait assessment
    );

    private static final List<String> PAIN_SCREENING_CODES = Arrays.asList(
        "38208-5",   // LOINC - Pain severity - 0-10 numeric rating scale
        "72514-3",   // LOINC - Pain severity - Wong-Baker FACES Pain Rating Scale
        "38221-8",   // LOINC - Pain assessment
        "38208-5",   // LOINC - Pain severity [Score] - Reported
        "80316-3",   // LOINC - Pain assessment panel
        "444661007", // SNOMED - Pain assessment
        "225908003"  // SNOMED - Pain management
    );

    private static final List<String> HIGH_RISK_MEDICATIONS_CODES = Arrays.asList(
        // Benzodiazepines (Beers Criteria - avoid in elderly)
        "2356",      // RxNorm - Alprazolam
        "3016",      // RxNorm - Diazepam
        "6470",      // RxNorm - Lorazepam
        // Anticholinergics
        "3248",      // RxNorm - Diphenhydramine
        "8076",      // RxNorm - Oxybutynin
        // NSAIDs (chronic use)
        "5640",      // RxNorm - Ibuprofen
        "7052"       // RxNorm - Naproxen
    );

    @Override
    public String getMeasureId() {
        return "COA";
    }

    @Override
    public String getMeasureName() {
        return "Care for Older Adults";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'COA-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating COA measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 65+)")
                .build();
        }

        resultBuilder.inDenominator(true);

        JsonNode patient = getPatientData(tenantId, patientId);
        Integer age = getPatientAge(patient);

        // Check for each component in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusYears(1).toString();

        // Component 1: Medication Review
        JsonNode medReviews = getObservations(tenantId, patientId,
            String.join(",", MEDICATION_REVIEW_CODES), dateFilter);
        boolean hasMedicationReview = !getEntries(medReviews).isEmpty();
        String medReviewDate = hasMedicationReview ? getEffectiveDate(getEntries(medReviews).get(0)) : null;

        // Component 2: Functional Assessment
        JsonNode functionalAssessments = getObservations(tenantId, patientId,
            String.join(",", FUNCTIONAL_ASSESSMENT_CODES), dateFilter);
        boolean hasFunctionalAssessment = !getEntries(functionalAssessments).isEmpty();
        String functionalAssessmentDate = hasFunctionalAssessment ?
            getEffectiveDate(getEntries(functionalAssessments).get(0)) : null;

        // Component 3: Pain Screening
        JsonNode painScreenings = getObservations(tenantId, patientId,
            String.join(",", PAIN_SCREENING_CODES), dateFilter);
        boolean hasPainScreening = !getEntries(painScreenings).isEmpty();
        String painScreeningDate = hasPainScreening ? getEffectiveDate(getEntries(painScreenings).get(0)) : null;

        // Extract pain score if available
        Integer painScore = null;
        if (hasPainScreening) {
            try {
                JsonNode screening = getEntries(painScreenings).get(0);
                if (screening.has("valueQuantity")) {
                    painScore = screening.get("valueQuantity").get("value").asInt();
                }
            } catch (Exception e) {
                logger.debug("Could not extract pain score: {}", e.getMessage());
            }
        }

        // Check for high-risk medications (Beers Criteria)
        JsonNode highRiskMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", HIGH_RISK_MEDICATIONS_CODES), dateFilter);
        boolean hasHighRiskMeds = !getEntries(highRiskMeds).isEmpty();

        // Calculate compliance based on 3 components
        int componentsCompleted = 0;
        if (hasMedicationReview) componentsCompleted++;
        if (hasFunctionalAssessment) componentsCompleted++;
        if (hasPainScreening) componentsCompleted++;

        double complianceRate = componentsCompleted / 3.0;
        boolean meetsGoal = componentsCompleted == 3;

        resultBuilder.inNumerator(meetsGoal);
        resultBuilder.complianceRate(complianceRate);
        resultBuilder.score(complianceRate * 100);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!hasMedicationReview) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_MEDICATION_REVIEW")
                .description("Older adult (age 65+) without annual medication review")
                .recommendedAction("Conduct comprehensive medication review; assess for polypharmacy, drug interactions, and Beers Criteria medications")
                .priority("high")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }

        if (!hasFunctionalAssessment) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_FUNCTIONAL_ASSESSMENT")
                .description("No functional status or fall risk assessment in last year")
                .recommendedAction("Assess ADLs, IADLs, mobility, and fall risk; screen for frailty; consider physical therapy referral if indicated")
                .priority("high")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }

        if (!hasPainScreening) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_PAIN_ASSESSMENT")
                .description("No pain screening documented in last year")
                .recommendedAction("Screen for pain using numeric rating scale; assess impact on function; optimize pain management")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(2))
                .build());
        }

        if (hasHighRiskMeds) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("POTENTIALLY_INAPPROPRIATE_MEDICATIONS")
                .description("Potentially inappropriate medications per Beers Criteria (benzodiazepines, anticholinergics, chronic NSAIDs)")
                .recommendedAction("Review Beers Criteria; consider alternatives; taper/discontinue if possible; assess fall risk")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());
        }

        if (painScore != null && painScore >= 7) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("UNCONTROLLED_PAIN")
                .description(String.format("Severe pain (score %d/10) in older adult", painScore))
                .recommendedAction("Reassess pain management; consider multimodal approach (acetaminophen, topicals, PT); avoid chronic opioids if possible")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());
        }

        resultBuilder.careGaps(careGaps);

        String painStatus = "Not assessed";
        if (painScore != null) {
            if (painScore <= 3) {
                painStatus = String.format("Mild pain (%d/10)", painScore);
            } else if (painScore <= 6) {
                painStatus = String.format("Moderate pain (%d/10)", painScore);
            } else {
                painStatus = String.format("Severe pain (%d/10)", painScore);
            }
        }

        resultBuilder.details(java.util.Map.of(
            "patientAge", age,
            "hasMedicationReview", hasMedicationReview,
            "medReviewDate", medReviewDate != null ? medReviewDate : "None",
            "hasFunctionalAssessment", hasFunctionalAssessment,
            "functionalAssessmentDate", functionalAssessmentDate != null ? functionalAssessmentDate : "None",
            "hasPainScreening", hasPainScreening,
            "painScreeningDate", painScreeningDate != null ? painScreeningDate : "None",
            "painScore", painStatus,
            "hasHighRiskMeds", hasHighRiskMeds,
            "componentsCompleted", String.format("%d/3", componentsCompleted)
        ));

        resultBuilder.evidence(java.util.Map.of(
            "medicationReviewCompleted", hasMedicationReview,
            "functionalAssessmentCompleted", hasFunctionalAssessment,
            "painScreeningCompleted", hasPainScreening,
            "comprehensiveCareProvided", meetsGoal
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        // Must be age 65+ (older adult)
        Integer age = getPatientAge(patient);
        if (age == null || age < 65) {
            return false;
        }

        return true;
    }
}
