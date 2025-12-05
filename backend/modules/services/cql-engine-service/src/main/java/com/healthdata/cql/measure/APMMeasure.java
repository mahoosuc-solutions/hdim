package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * APM - Metabolic Monitoring for Children and Adolescents on Antipsychotics (HEDIS)
 *
 * Evaluates whether patients on antipsychotics received metabolic monitoring:
 * - Blood glucose testing
 * - Cholesterol testing
 * - Weight assessment
 *
 * Three rates tracked:
 * 1. Blood glucose AND cholesterol testing
 * 2. Blood glucose testing only
 * 3. Cholesterol testing only
 */
@Component
public class APMMeasure extends AbstractHedisMeasure {

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
        "61381",     // RxNorm - Iloperidone
        "142439",    // RxNorm - Lurasidone
        "41996",     // RxNorm - Olanzapine
        "784649",    // RxNorm - Paliperidone
        "35636",     // RxNorm - Quetiapine
        "35827",     // RxNorm - Risperidone
        "258337",    // RxNorm - Ziprasidone
        "1040028"    // RxNorm - Brexpiprazole
    );

    private static final List<String> GLUCOSE_TEST_CODES = Arrays.asList(
        "2339-0",    // LOINC - Glucose [Mass/volume] in Blood
        "2345-7",    // LOINC - Glucose [Mass/volume] in Serum or Plasma
        "41653-7",   // LOINC - Glucose [Mass/volume] in Venous blood
        "1558-6",    // LOINC - Fasting glucose [Mass/volume] in Serum or Plasma
        "4548-4",    // LOINC - Hemoglobin A1c/Hemoglobin.total in Blood
        "17856-6",   // LOINC - Hemoglobin A1c/Hemoglobin.total in Blood by HPLC
        "59261-8"    // LOINC - Hemoglobin A1c/Hemoglobin.total in Blood by calculation
    );

    private static final List<String> CHOLESTEROL_TEST_CODES = Arrays.asList(
        "2093-3",    // LOINC - Cholesterol [Mass/volume] in Serum or Plasma
        "14647-2",   // LOINC - Cholesterol [Moles/volume] in Serum or Plasma
        "18262-6",   // LOINC - LDL Cholesterol [Mass/volume] in Serum or Plasma
        "13457-7",   // LOINC - LDL Cholesterol [Mass/volume] in Serum or Plasma by calculation
        "2085-9",    // LOINC - HDL Cholesterol [Mass/volume] in Serum or Plasma
        "2571-8"     // LOINC - Triglyceride [Mass/volume] in Serum or Plasma
    );

    private static final List<String> WEIGHT_CODES = Arrays.asList(
        "29463-7",   // LOINC - Body weight
        "3141-9",    // LOINC - Body weight Measured
        "8350-1"     // LOINC - Body weight [Mass]
    );

    private static final List<String> BMI_CODES = Arrays.asList(
        "39156-5",   // LOINC - Body mass index (BMI) [Ratio]
        "59574-4",   // LOINC - Body mass index (BMI) [Percentile]
        "89270-3"    // LOINC - Body mass index (BMI) [Calculated]
    );

    @Override
    public String getMeasureId() {
        return "APM";
    }

    @Override
    public String getMeasureName() {
        return "Metabolic Monitoring for Antipsychotics";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'APM-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating APM measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 1-17 with new antipsychotic prescription in last 12 months)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Get antipsychotic medication start date (most recent prescription in last 12 months)
        String medDateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode antipsychoticMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", ANTIPSYCHOTIC_MEDICATION_CODES), medDateFilter);
        List<JsonNode> medEntries = getEntries(antipsychoticMeds);

        if (medEntries.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No antipsychotic medications found in measurement period")
                .build();
        }

        // Calculate monitoring window (within 12 months of medication start)
        String medStartDate = getEffectiveDate(medEntries.get(0));
        LocalDate monitoringStart = LocalDate.parse(medStartDate);
        LocalDate monitoringEnd = monitoringStart.plusMonths(12);
        if (monitoringEnd.isAfter(LocalDate.now())) {
            monitoringEnd = LocalDate.now();
        }

        String monitoringFilter = "ge" + monitoringStart.toString() + "&date=le" + monitoringEnd.toString();

        // Check for glucose testing
        JsonNode glucoseTests = getObservations(tenantId, patientId,
            String.join(",", GLUCOSE_TEST_CODES), monitoringFilter);
        boolean hasGlucoseTest = !getEntries(glucoseTests).isEmpty();
        String glucoseTestDate = hasGlucoseTest ? getEffectiveDate(getEntries(glucoseTests).get(0)) : null;

        // Check for cholesterol testing
        JsonNode cholesterolTests = getObservations(tenantId, patientId,
            String.join(",", CHOLESTEROL_TEST_CODES), monitoringFilter);
        boolean hasCholesterolTest = !getEntries(cholesterolTests).isEmpty();
        String cholesterolTestDate = hasCholesterolTest ? getEffectiveDate(getEntries(cholesterolTests).get(0)) : null;

        // Check for weight assessment
        JsonNode weightObs = getObservations(tenantId, patientId,
            String.join(",", WEIGHT_CODES), monitoringFilter);
        JsonNode bmiObs = getObservations(tenantId, patientId,
            String.join(",", BMI_CODES), monitoringFilter);
        boolean hasWeightAssessment = !getEntries(weightObs).isEmpty() || !getEntries(bmiObs).isEmpty();

        // Calculate compliance
        int componentsCompleted = 0;
        if (hasGlucoseTest) componentsCompleted++;
        if (hasCholesterolTest) componentsCompleted++;
        if (hasWeightAssessment) componentsCompleted++;

        boolean hasCompleteMetabolicMonitoring = hasGlucoseTest && hasCholesterolTest;
        double complianceRate = componentsCompleted / 3.0;

        resultBuilder.inNumerator(hasCompleteMetabolicMonitoring); // Primary rate: glucose AND cholesterol
        resultBuilder.complianceRate(complianceRate);
        resultBuilder.score(complianceRate * 100);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();
        if (!hasGlucoseTest) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_GLUCOSE_MONITORING")
                .description("No glucose or HbA1c testing within 12 months of antipsychotic initiation")
                .recommendedAction("Order fasting glucose or HbA1c test - antipsychotics increase diabetes risk")
                .priority("high")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }

        if (!hasCholesterolTest) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_LIPID_MONITORING")
                .description("No lipid panel within 12 months of antipsychotic initiation")
                .recommendedAction("Order lipid panel (total cholesterol, LDL, HDL, triglycerides)")
                .priority("high")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }

        if (!hasWeightAssessment) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_WEIGHT_MONITORING")
                .description("No weight or BMI assessment within 12 months of antipsychotic initiation")
                .recommendedAction("Document weight and BMI - antipsychotics cause metabolic changes and weight gain")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(2))
                .build());
        }
        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "medicationStartDate", medStartDate,
            "hasGlucoseTest", hasGlucoseTest,
            "glucoseTestDate", glucoseTestDate != null ? glucoseTestDate : "Not available",
            "hasCholesterolTest", hasCholesterolTest,
            "cholesterolTestDate", cholesterolTestDate != null ? cholesterolTestDate : "Not available",
            "hasWeightAssessment", hasWeightAssessment,
            "hasCompleteMetabolicMonitoring", hasCompleteMetabolicMonitoring
        ));

        resultBuilder.evidence(java.util.Map.of(
            "glucoseMonitoring", hasGlucoseTest,
            "lipidMonitoring", hasCholesterolTest,
            "weightMonitoring", hasWeightAssessment,
            "completeMonitoring", hasCompleteMetabolicMonitoring
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 1 || age > 17) {
            return false;
        }

        // Must have antipsychotic medication prescribed in last 12 months
        String medDateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode antipsychoticMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", ANTIPSYCHOTIC_MEDICATION_CODES), medDateFilter);

        return !getEntries(antipsychoticMeds).isEmpty();
    }
}
