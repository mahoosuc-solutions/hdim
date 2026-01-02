package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * SMC - Cardiovascular Monitoring for People with Cardiovascular Disease and Schizophrenia (HEDIS)
 *
 * Evaluates whether patients with schizophrenia/schizoaffective disorder and cardiovascular disease
 * received annual LDL-C screening.
 */
@Component
public class SMCMeasure extends AbstractHedisMeasure {

    private static final List<String> SCHIZOPHRENIA_CODES = Arrays.asList(
        "16990005",  // Schizophrenia (SNOMED)
        "58214004",  // Schizophreniform disorder (SNOMED)
        "26025008",  // Schizoaffective disorder (SNOMED)
        "191618008"  // Psychotic disorder (SNOMED)
    );

    private static final List<String> CVD_CODES = Arrays.asList(
        "53741008",  // Coronary arteriosclerosis (SNOMED)
        "414545008", // Ischemic heart disease (SNOMED)
        "22298006",  // Myocardial infarction (SNOMED)
        "230690007", // Cerebrovascular accident (SNOMED)
        "413838009", // Chronic ischemic heart disease (SNOMED)
        "38341003",  // Hypertension (SNOMED)
        "399957001", // Peripheral vascular disease (SNOMED)
        "71642004"   // Cerebrovascular disease (SNOMED)
    );

    private static final List<String> LDL_TEST_CODES = Arrays.asList(
        "18262-6",   // LDL Cholesterol [Mass/volume] in Serum or Plasma (LOINC)
        "13457-7",   // LDL Cholesterol [Mass/volume] in Serum or Plasma by calculation (LOINC)
        "2089-1",    // LDL Cholesterol [Mass/volume] in Serum or Plasma by Direct assay (LOINC)
        "18261-8"    // LDL Cholesterol [Mass/volume] in Blood (LOINC)
    );

    @Override
    public String getMeasureId() {
        return "SMC";
    }

    @Override
    public String getMeasureName() {
        return "Cardiovascular Monitoring for People with CVD and Schizophrenia";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'SMC-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating SMC measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 18-64 with schizophrenia and CVD)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for LDL-C test in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode ldlTests = getObservations(tenantId, patientId,
            String.join(",", LDL_TEST_CODES), dateFilter);
        List<JsonNode> ldlEntries = getEntries(ldlTests);

        boolean hasLDLTest = !ldlEntries.isEmpty();

        resultBuilder.inNumerator(hasLDLTest);
        resultBuilder.complianceRate(hasLDLTest ? 1.0 : 0.0);
        resultBuilder.score(hasLDLTest ? 100.0 : 0.0);

        if (hasLDLTest) {
            JsonNode mostRecentLDL = ldlEntries.get(0);
            String ldlDate = getEffectiveDate(mostRecentLDL);

            // Try to extract LDL value
            Double ldlValue = null;
            try {
                if (mostRecentLDL.has("valueQuantity")) {
                    JsonNode valueQuantity = mostRecentLDL.get("valueQuantity");
                    if (valueQuantity.has("value")) {
                        ldlValue = valueQuantity.get("value").asDouble();
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not extract LDL value: {}", e.getMessage());
            }

            // Assess LDL control
            String ldlAssessment = "Not available";
            if (ldlValue != null) {
                if (ldlValue < 70) {
                    ldlAssessment = "Optimal (<70 mg/dL)";
                } else if (ldlValue < 100) {
                    ldlAssessment = "Near optimal (70-100 mg/dL)";
                } else if (ldlValue < 130) {
                    ldlAssessment = "Borderline high (100-130 mg/dL)";
                } else {
                    ldlAssessment = "High (≥130 mg/dL)";
                }
            }

            resultBuilder.evidence(java.util.Map.of(
                "ldlTestDate", ldlDate,
                "ldlValue", ldlValue != null ? ldlValue : "Not available",
                "ldlAssessment", ldlAssessment,
                "totalTests", ldlEntries.size()
            ));

            resultBuilder.details(java.util.Map.of(
                "ldlMonitoringComplete", true,
                "lastTestDate", ldlDate,
                "ldlValue", ldlValue != null ? ldlValue : "Not available",
                "ldlAssessment", ldlAssessment
            ));

            // If LDL is high, add care gap for management
            if (ldlValue != null && ldlValue >= 130) {
                resultBuilder.careGaps(List.of(
                    MeasureResult.CareGap.builder()
                        .gapType("ELEVATED_LDL_CHOLESTEROL")
                        .description(String.format("LDL cholesterol elevated at %.1f mg/dL (target <100 mg/dL for CVD)", ldlValue))
                        .recommendedAction("Consider statin therapy intensification or initiation per CVD guidelines")
                        .priority("high")
                        .dueDate(LocalDate.now().plusMonths(1))
                        .build()
                ));
            }
        } else {
            // Care gap - no LDL monitoring
            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("MISSING_LDL_MONITORING")
                    .description("No LDL cholesterol monitoring in last 12 months for patient with CVD and schizophrenia")
                    .recommendedAction("Order LDL cholesterol test - annual monitoring required for CVD management")
                    .priority("high")
                    .dueDate(LocalDate.now().plusMonths(1))
                    .build()
            ));

            resultBuilder.details(java.util.Map.of(
                "ldlMonitoringComplete", false
            ));
        }

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 18 || age > 64) {
            return false;
        }

        // Must have schizophrenia or schizoaffective disorder
        JsonNode schizophreniaConditions = getConditions(tenantId, patientId,
            String.join(",", SCHIZOPHRENIA_CODES));
        if (getEntries(schizophreniaConditions).isEmpty()) {
            return false;
        }

        // Must have cardiovascular disease
        JsonNode cvdConditions = getConditions(tenantId, patientId,
            String.join(",", CVD_CODES));
        return !getEntries(cvdConditions).isEmpty();
    }
}
