package com.healthdata.demo.strategy;

import com.healthdata.demo.application.DemoSeedingService;
import com.healthdata.demo.application.DemoSeedingService.GenerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * HEDIS Evaluation Scenario Strategy.
 *
 * Purpose: Generate a large patient population optimized for quality measure evaluation and reporting.
 *
 * Characteristics:
 * - Patient Count: 5,000 patients
 * - Care Gap Rate: 28% (1,400 patients with gaps)
 * - Age Distribution:
 *   - 25% ages 18-40 (younger adults)
 *   - 40% ages 41-65 (middle-aged, chronic disease prevalence)
 *   - 35% ages 66+ (Medicare, multiple comorbidities)
 * - Gender Distribution: 52% Female, 48% Male
 * - Condition Prevalence:
 *   - Diabetes: 25% (1,250 patients)
 *   - Hypertension: 40% (2,000 patients)
 *   - COPD: 8% (400 patients)
 *   - CHF: 5% (250 patients)
 *   - CKD: 10% (500 patients)
 *
 * Quality Measures Tested:
 * - BCS (Breast Cancer Screening)
 * - COL (Colorectal Cancer Screening)
 * - CBP (Controlling Blood Pressure)
 * - CDC (Comprehensive Diabetes Care)
 * - EED (Eye Exam for Diabetics)
 * - KED (Kidney Health Evaluation for Diabetics)
 *
 * Use Cases:
 * - Quality measure evaluation at scale
 * - Care gap identification and reporting
 * - Star ratings simulation
 * - Provider performance dashboards
 */
@Component
public class HedisEvaluationStrategy implements ScenarioSeedingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(HedisEvaluationStrategy.class);

    private static final String SCENARIO_NAME = "hedis-evaluation";
    private static final String SCENARIO_DESCRIPTION =
        "Large patient population (5K) optimized for HEDIS quality measure evaluation with 28% care gap rate";
    private static final int PATIENT_COUNT = 5000;
    private static final int CARE_GAP_PERCENTAGE = 28;

    private final DemoSeedingService demoSeedingService;

    public HedisEvaluationStrategy(DemoSeedingService demoSeedingService) {
        this.demoSeedingService = demoSeedingService;
    }

    @Override
    public SeedingResult seedScenario(String tenantId) {
        logger.info("Starting HEDIS Evaluation scenario seeding for tenant: {}", tenantId);
        long startTime = System.currentTimeMillis();

        try {
            // Generate patient cohort with HEDIS-optimized parameters
            GenerationResult generationResult = demoSeedingService.generatePatientCohort(
                PATIENT_COUNT,
                tenantId,
                CARE_GAP_PERCENTAGE
            );

            long durationMs = System.currentTimeMillis() - startTime;

            logger.info("HEDIS Evaluation scenario seeded successfully: {} patients, {} care gaps, {}ms",
                generationResult.getPatientCount(),
                generationResult.getCareGapCount(),
                durationMs);

            return SeedingResult.builder()
                .scenarioName(SCENARIO_NAME)
                .patientsCreated(generationResult.getPatientCount())
                .observationsCreated(generationResult.getObservationCount())
                .proceduresCreated(generationResult.getProcedureCount())
                .encountersCreated(generationResult.getEncounterCount())
                .medicationsCreated(generationResult.getMedicationCount())
                .conditionsCreated(estimateConditions(generationResult.getPatientCount()))
                .careGapsExpected(generationResult.getCareGapCount())
                .durationMs(durationMs)
                .success(true)
                .build();

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            logger.error("Failed to seed HEDIS Evaluation scenario for tenant: {}", tenantId, e);

            return SeedingResult.builder()
                .scenarioName(SCENARIO_NAME)
                .durationMs(durationMs)
                .success(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }

    @Override
    public String getScenarioName() {
        return SCENARIO_NAME;
    }

    @Override
    public String getScenarioDescription() {
        return SCENARIO_DESCRIPTION;
    }

    @Override
    public int getExpectedPatientCount() {
        return PATIENT_COUNT;
    }

    /**
     * Estimate number of conditions based on patient count.
     * Average ~1.5 chronic conditions per patient.
     */
    private int estimateConditions(int patientCount) {
        return (int) (patientCount * 1.5);
    }
}
