package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * SSD - Diabetes Screening for People with Schizophrenia or Bipolar Disorder Who Are Using Antipsychotic Medications (HEDIS)
 *
 * Evaluates whether patients with schizophrenia/bipolar disorder on antipsychotics
 * received diabetes screening (glucose or HbA1c test) in the measurement year.
 */
@Component
public class SSDMeasure extends AbstractHedisMeasure {

    private static final List<String> SCHIZOPHRENIA_BIPOLAR_CODES = Arrays.asList(
        "16990005",  // Schizophrenia (SNOMED)
        "58214004",  // Schizophreniform disorder (SNOMED)
        "26025008",  // Schizoaffective disorder (SNOMED)
        "191618008", // Psychotic disorder (SNOMED)
        "371596008", // Bipolar I disorder (SNOMED)
        "191620000", // Bipolar II disorder (SNOMED)
        "13746004"   // Bipolar disorder (SNOMED)
    );

    private static final List<String> ANTIPSYCHOTIC_MEDICATION_CODES = Arrays.asList(
        // Typical Antipsychotics
        "3033",      // RxNorm - Chlorpromazine
        "4091",      // RxNorm - Fluphenazine
        "5093",      // RxNorm - Haloperidol
        "8076",      // RxNorm - Perphenazine
        // Atypical Antipsychotics
        "46303",     // RxNorm - Aripiprazole
        "115698",    // RxNorm - Asenapine
        "89013",     // RxNorm - Clozapine
        "41996",     // RxNorm - Olanzapine
        "784649",    // RxNorm - Paliperidone
        "35636",     // RxNorm - Quetiapine
        "35827",     // RxNorm - Risperidone
        "258337"     // RxNorm - Ziprasidone
    );

    private static final List<String> DIABETES_SCREENING_CODES = Arrays.asList(
        // Glucose Tests
        "2339-0",    // LOINC - Glucose [Mass/volume] in Blood
        "2345-7",    // LOINC - Glucose [Mass/volume] in Serum or Plasma
        "41653-7",   // LOINC - Glucose [Mass/volume] in Venous blood
        "1558-6",    // LOINC - Fasting glucose [Mass/volume] in Serum or Plasma
        // HbA1c Tests
        "4548-4",    // LOINC - Hemoglobin A1c/Hemoglobin.total in Blood
        "17856-6",   // LOINC - Hemoglobin A1c/Hemoglobin.total in Blood by HPLC
        "59261-8"    // LOINC - Hemoglobin A1c/Hemoglobin.total in Blood by calculation
    );

    @Override
    public String getMeasureId() {
        return "SSD";
    }

    @Override
    public String getMeasureName() {
        return "Diabetes Screening for People with Schizophrenia or Bipolar Disorder on Antipsychotics";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'SSD-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating SSD measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 18-64 with schizophrenia/bipolar disorder on antipsychotics)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for diabetes screening in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode diabetesScreening = getObservations(tenantId, patientId,
            String.join(",", DIABETES_SCREENING_CODES), dateFilter);
        List<JsonNode> screeningEntries = getEntries(diabetesScreening);

        boolean hasScreening = !screeningEntries.isEmpty();

        resultBuilder.inNumerator(hasScreening);
        resultBuilder.complianceRate(hasScreening ? 1.0 : 0.0);
        resultBuilder.score(hasScreening ? 100.0 : 0.0);

        if (hasScreening) {
            JsonNode mostRecentScreening = screeningEntries.get(0);
            String screeningDate = getEffectiveDate(mostRecentScreening);

            // Try to extract glucose or HbA1c value
            Double screeningValue = null;
            String screeningType = "Diabetes Screening";

            try {
                if (mostRecentScreening.has("valueQuantity")) {
                    JsonNode valueQuantity = mostRecentScreening.get("valueQuantity");
                    if (valueQuantity.has("value")) {
                        screeningValue = valueQuantity.get("value").asDouble();

                        // Determine type based on value range
                        if (screeningValue < 20) {
                            screeningType = "HbA1c";
                        } else {
                            screeningType = "Glucose";
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not extract screening value: {}", e.getMessage());
            }

            resultBuilder.evidence(java.util.Map.of(
                "screeningDate", screeningDate,
                "screeningType", screeningType,
                "screeningValue", screeningValue != null ? screeningValue : "Not available",
                "totalScreenings", screeningEntries.size()
            ));

            resultBuilder.details(java.util.Map.of(
                "diabetesScreeningComplete", true,
                "lastScreeningDate", screeningDate,
                "screeningType", screeningType
            ));
        } else {
            // Care gap - no diabetes screening
            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("MISSING_DIABETES_SCREENING")
                    .description("No diabetes screening (glucose or HbA1c) in last 12 months for patient on antipsychotic medication")
                    .recommendedAction("Order fasting glucose or HbA1c test - antipsychotic medications increase diabetes risk")
                    .priority("high")
                    .dueDate(LocalDate.now().plusMonths(1))
                    .build()
            ));

            resultBuilder.details(java.util.Map.of(
                "diabetesScreeningComplete", false
            ));
        }

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 18 || age > 64) {
            return false;
        }

        // Must have schizophrenia or bipolar disorder diagnosis
        JsonNode mentalHealthConditions = getConditions(tenantId, patientId,
            String.join(",", SCHIZOPHRENIA_BIPOLAR_CODES));
        if (getEntries(mentalHealthConditions).isEmpty()) {
            return false;
        }

        // Must be on antipsychotic medication (any fill in last 12 months)
        String medDateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode antipsychoticMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", ANTIPSYCHOTIC_MEDICATION_CODES), medDateFilter);

        return !getEntries(antipsychoticMeds).isEmpty();
    }
}
