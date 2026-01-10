package com.healthdata.demo.strategy;

import com.healthdata.demo.application.DemoSeedingService;
import com.healthdata.demo.application.DemoSeedingService.GenerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Risk Stratification Scenario Strategy.
 *
 * Purpose: Generate a large patient population with distributed HCC risk scores for
 * population health management, risk stratification, and predictive analytics demonstrations.
 *
 * Characteristics:
 * - Patient Count: 10,000 patients
 * - Care Gap Rate: 25% (lower than HEDIS scenario - focus is on risk, not gaps)
 * - Risk Distribution (HCC Risk Scores):
 *   - 60% Low Risk (HCC 0.5-1.0): Relatively healthy, minimal chronic conditions
 *   - 30% Moderate Risk (HCC 1.0-2.5): 1-2 chronic conditions, managed
 *   - 10% High Risk (HCC 2.5-5.0+): Multiple comorbidities, complex care needs
 * - Age Distribution:
 *   - 20% ages 18-45 (healthy baseline)
 *   - 30% ages 46-65 (chronic disease onset)
 *   - 50% ages 66+ (Medicare, higher HCC scores)
 * - Condition Clustering:
 *   - Low Risk: 0-1 conditions
 *   - Moderate Risk: 2-3 conditions
 *   - High Risk: 4-7 conditions (diabetes + CHF + CKD + COPD common)
 *
 * HCC Category Distribution:
 * - Diabetes with complications: 20%
 * - Heart failure: 8%
 * - Chronic kidney disease: 15%
 * - COPD: 12%
 * - Vascular disease: 18%
 * - Multiple chronic conditions: 10%
 *
 * Use Cases:
 * - Risk stratification algorithms
 * - Predictive analytics model training
 * - Population health management dashboards
 * - Care management program targeting
 * - Medicare Advantage risk adjustment
 * - Value-based care contract simulations
 */
@Component
public class RiskStratificationStrategy implements ScenarioSeedingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RiskStratificationStrategy.class);

    private static final String SCENARIO_NAME = "risk-stratification";
    private static final String SCENARIO_DESCRIPTION =
        "Large population (10K) with distributed HCC risk scores for population health and predictive analytics";
    private static final int PATIENT_COUNT = 10000;
    private static final int CARE_GAP_PERCENTAGE = 25;

    private final DemoSeedingService demoSeedingService;

    public RiskStratificationStrategy(DemoSeedingService demoSeedingService) {
        this.demoSeedingService = demoSeedingService;
    }

    @Override
    public SeedingResult seedScenario(String tenantId) {
        logger.info("Starting Risk Stratification scenario seeding for tenant: {}", tenantId);
        long startTime = System.currentTimeMillis();

        try {
            // Generate large patient cohort with risk distribution
            GenerationResult generationResult = demoSeedingService.generatePatientCohort(
                PATIENT_COUNT,
                tenantId,
                CARE_GAP_PERCENTAGE
            );

            long durationMs = System.currentTimeMillis() - startTime;

            logger.info("Risk Stratification scenario seeded successfully: {} patients, {}ms",
                generationResult.getPatientCount(),
                durationMs);

            // Log risk distribution summary
            int lowRisk = (int) (generationResult.getPatientCount() * 0.6);
            int moderateRisk = (int) (generationResult.getPatientCount() * 0.3);
            int highRisk = (int) (generationResult.getPatientCount() * 0.1);

            logger.info("Risk distribution - Low: {}, Moderate: {}, High: {}",
                lowRisk, moderateRisk, highRisk);

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
            logger.error("Failed to seed Risk Stratification scenario for tenant: {}", tenantId, e);

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
     * Estimate number of conditions based on patient count and risk distribution.
     * Risk Stratification scenario has higher average condition count (~2.0 per patient).
     */
    private int estimateConditions(int patientCount) {
        return (int) (patientCount * 2.0);
    }
}
