package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * KED - Kidney Health Evaluation for Patients with Diabetes (HEDIS)
 *
 * Evaluates whether diabetic patients received annual kidney health screening:
 * - eGFR (estimated Glomerular Filtration Rate) test
 * - uACR (urine Albumin-Creatinine Ratio) test
 *
 * Both tests required for numerator compliance.
 */
@Component
public class KEDMeasure extends AbstractHedisMeasure {

    private static final List<String> DIABETES_CODES = Arrays.asList(
        "44054006",  // Type 2 diabetes mellitus (SNOMED)
        "46635009",  // Type 1 diabetes mellitus (SNOMED)
        "73211009",  // Diabetes mellitus (SNOMED)
        "199223000", // Pre-existing diabetes mellitus (SNOMED)
        "237599002", // Insulin-dependent diabetes mellitus (SNOMED)
        "237618001"  // Non-insulin-dependent diabetes mellitus (SNOMED)
    );

    private static final List<String> EGFR_CODES = Arrays.asList(
        "33914-3",   // LOINC - Glomerular filtration rate/1.73 sq M.predicted
        "48642-3",   // LOINC - Glomerular filtration rate/1.73 sq M.predicted [Volume Rate/Area] in Serum or Plasma by Creatinine-based formula (MDRD)
        "48643-1",   // LOINC - Glomerular filtration rate/1.73 sq M.predicted [Volume Rate/Area] in Serum or Plasma by Creatinine-based formula (CKD-EPI)
        "50044-7",   // LOINC - Glomerular filtration rate/1.73 sq M.predicted among females [Volume Rate/Area]
        "50210-4",   // LOINC - Glomerular filtration rate/1.73 sq M.predicted among non-blacks [Volume Rate/Area]
        "62238-1",   // LOINC - Glomerular filtration rate/1.73 sq M.predicted [Volume Rate/Area] in Serum, Plasma or Blood
        "77147-7"    // LOINC - Glomerular filtration rate/1.73 sq M.predicted [Volume Rate/Area] in Serum, Plasma or Blood by Creatinine-based formula (CKD-EPI 2021)
    );

    private static final List<String> SERUM_CREATININE_CODES = Arrays.asList(
        "2160-0",    // LOINC - Creatinine [Mass/volume] in Serum or Plasma
        "38483-4",   // LOINC - Creatinine [Mass/volume] in Blood
        "14682-9"    // LOINC - Creatinine [Moles/volume] in Serum or Plasma
    );

    private static final List<String> UACR_CODES = Arrays.asList(
        "9318-7",    // LOINC - Albumin/Creatinine [Mass Ratio] in Urine
        "14958-3",   // LOINC - Albumin/Creatinine [Moles/volume] in Urine
        "13705-9",   // LOINC - Albumin/Creatinine [Mass Ratio] in Urine by Test strip
        "30000-4",   // LOINC - Albumin/Creatinine [Ratio] in Urine
        "89998-8"    // LOINC - Albumin/Creatinine [Mass Ratio] in Urine by calculation
    );

    private static final List<String> URINE_ALBUMIN_CODES = Arrays.asList(
        "1754-1",    // LOINC - Albumin [Mass/volume] in Urine
        "2862-1",    // LOINC - Albumin [Mass/time] in 24 hour Urine
        "14957-5"    // LOINC - Microalbumin [Mass/volume] in Urine
    );

    private static final List<String> URINE_PROTEIN_CODES = Arrays.asList(
        "2888-6",    // LOINC - Protein [Mass/volume] in Urine
        "2889-4",    // LOINC - Protein [Mass/time] in 24 hour Urine
        "20454-5"    // LOINC - Protein [Presence] in Urine by Test strip
    );

    @Override
    public String getMeasureId() {
        return "KED";
    }

    @Override
    public String getMeasureName() {
        return "Kidney Health Evaluation for Diabetes";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'KED-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating KED measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 18-75 with diabetes)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for kidney function testing in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();

        // Check for eGFR (or serum creatinine which can be used to calculate eGFR)
        JsonNode egfrTests = getObservations(tenantId, patientId,
            String.join(",", EGFR_CODES), dateFilter);
        boolean hasEGFR = !getEntries(egfrTests).isEmpty();

        // If no direct eGFR, check for serum creatinine (can calculate eGFR from this)
        if (!hasEGFR) {
            JsonNode creatinineTests = getObservations(tenantId, patientId,
                String.join(",", SERUM_CREATININE_CODES), dateFilter);
            hasEGFR = !getEntries(creatinineTests).isEmpty();
        }

        String egfrTestDate = null;
        Double egfrValue = null;
        if (hasEGFR) {
            List<JsonNode> egfrEntries = getEntries(egfrTests);
            if (!egfrEntries.isEmpty()) {
                JsonNode mostRecentEGFR = egfrEntries.get(0);
                egfrTestDate = getEffectiveDate(mostRecentEGFR);

                // Extract eGFR value
                try {
                    if (mostRecentEGFR.has("valueQuantity")) {
                        JsonNode valueQuantity = mostRecentEGFR.get("valueQuantity");
                        if (valueQuantity.has("value")) {
                            egfrValue = valueQuantity.get("value").asDouble();
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Could not extract eGFR value: {}", e.getMessage());
                }
            }
        }

        // Check for uACR (urine albumin-creatinine ratio)
        JsonNode uacrTests = getObservations(tenantId, patientId,
            String.join(",", UACR_CODES), dateFilter);
        boolean hasUACR = !getEntries(uacrTests).isEmpty();

        // If no uACR, check for urine albumin or urine protein (alternatives)
        if (!hasUACR) {
            JsonNode urineAlbuminTests = getObservations(tenantId, patientId,
                String.join(",", URINE_ALBUMIN_CODES), dateFilter);
            hasUACR = !getEntries(urineAlbuminTests).isEmpty();

            if (!hasUACR) {
                JsonNode urineProteinTests = getObservations(tenantId, patientId,
                    String.join(",", URINE_PROTEIN_CODES), dateFilter);
                hasUACR = !getEntries(urineProteinTests).isEmpty();
            }
        }

        String uacrTestDate = null;
        Double uacrValue = null;
        if (hasUACR) {
            List<JsonNode> uacrEntries = getEntries(uacrTests);
            if (!uacrEntries.isEmpty()) {
                JsonNode mostRecentUACR = uacrEntries.get(0);
                uacrTestDate = getEffectiveDate(mostRecentUACR);

                // Extract uACR value
                try {
                    if (mostRecentUACR.has("valueQuantity")) {
                        JsonNode valueQuantity = mostRecentUACR.get("valueQuantity");
                        if (valueQuantity.has("value")) {
                            uacrValue = valueQuantity.get("value").asDouble();
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Could not extract uACR value: {}", e.getMessage());
                }
            }
        }

        // Calculate compliance
        int componentsCompleted = 0;
        if (hasEGFR) componentsCompleted++;
        if (hasUACR) componentsCompleted++;

        boolean hasCompleteKidneyEvaluation = hasEGFR && hasUACR;
        double complianceRate = componentsCompleted / 2.0;

        resultBuilder.inNumerator(hasCompleteKidneyEvaluation);
        resultBuilder.complianceRate(complianceRate);
        resultBuilder.score(complianceRate * 100);

        // Assess kidney function status
        String kidneyStatus = "Unknown";
        if (egfrValue != null) {
            if (egfrValue >= 90) {
                kidneyStatus = "Normal kidney function (eGFR ≥90)";
            } else if (egfrValue >= 60) {
                kidneyStatus = "Mildly decreased function (eGFR 60-89)";
            } else if (egfrValue >= 45) {
                kidneyStatus = "Mild to moderate decrease (eGFR 45-59) - CKD Stage 3a";
            } else if (egfrValue >= 30) {
                kidneyStatus = "Moderate to severe decrease (eGFR 30-44) - CKD Stage 3b";
            } else if (egfrValue >= 15) {
                kidneyStatus = "Severe decrease (eGFR 15-29) - CKD Stage 4";
            } else {
                kidneyStatus = "Kidney failure (eGFR <15) - CKD Stage 5";
            }
        }

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();
        if (!hasEGFR) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_EGFR_TEST")
                .description("No eGFR or serum creatinine test in last 12 months for diabetic patient")
                .recommendedAction("Order serum creatinine with eGFR calculation - annual kidney function monitoring required for diabetes")
                .priority("high")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        } else if (egfrValue != null && egfrValue < 60) {
            String priority = egfrValue < 30 ? "high" : "medium";
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("DECREASED_KIDNEY_FUNCTION")
                .description(String.format("Decreased kidney function detected (eGFR %.1f)", egfrValue))
                .recommendedAction("Consider nephrology referral, ACE inhibitor/ARB therapy, and more frequent monitoring")
                .priority(priority)
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }

        if (!hasUACR) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_UACR_TEST")
                .description("No urine albumin-creatinine ratio (uACR) test in last 12 months")
                .recommendedAction("Order uACR test - early detection of diabetic nephropathy requires annual urine albumin screening")
                .priority("high")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        } else if (uacrValue != null && uacrValue >= 30) {
            String albuminuriaLevel = uacrValue >= 300 ? "Macroalbuminuria" : "Microalbuminuria";
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("ELEVATED_URINE_ALBUMIN")
                .description(String.format("%s detected (uACR %.1f mg/g, normal <30)", albuminuriaLevel, uacrValue))
                .recommendedAction("Intensify diabetes management, optimize ACE inhibitor/ARB therapy, consider nephrology referral")
                .priority("high")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }
        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "hasEGFR", hasEGFR,
            "egfrTestDate", egfrTestDate != null ? egfrTestDate : "Not available",
            "egfrValue", egfrValue != null ? egfrValue : "Not available",
            "kidneyStatus", kidneyStatus,
            "hasUACR", hasUACR,
            "uacrTestDate", uacrTestDate != null ? uacrTestDate : "Not available",
            "uacrValue", uacrValue != null ? uacrValue : "Not available",
            "hasCompleteKidneyEvaluation", hasCompleteKidneyEvaluation
        ));

        resultBuilder.evidence(java.util.Map.of(
            "egfrTested", hasEGFR,
            "uacrTested", hasUACR,
            "completeEvaluation", hasCompleteKidneyEvaluation
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 18 || age > 75) {
            return false;
        }

        // Must have diabetes diagnosis
        JsonNode diabetesConditions = getConditions(tenantId, patientId,
            String.join(",", DIABETES_CODES));

        return !getEntries(diabetesConditions).isEmpty();
    }
}
