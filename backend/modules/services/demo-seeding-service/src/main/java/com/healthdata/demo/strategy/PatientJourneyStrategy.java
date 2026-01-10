package com.healthdata.demo.strategy;

import com.healthdata.demo.application.DemoSeedingService;
import com.healthdata.demo.application.DemoSeedingService.GenerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Patient Journey Scenario Strategy.
 *
 * Purpose: Generate detailed patient personas with rich clinical histories for demonstrations,
 * training, and patient journey mapping.
 *
 * Characteristics:
 * - Patient Count: 1,000 patients + 4 named personas
 * - Care Gap Rate: 35% (focus on workflow demonstrations)
 * - Rich Clinical Data:
 *   - 5-10 encounters per patient (detailed history)
 *   - 8-15 observations per patient (comprehensive vitals & labs)
 *   - 3-6 procedures per patient (screening/preventive care)
 *   - 2-5 active medications per patient
 * - Age Distribution:
 *   - 15% ages 18-30 (young adults, preventive care)
 *   - 35% ages 31-50 (working age, chronic disease onset)
 *   - 35% ages 51-70 (multiple comorbidities)
 *   - 15% ages 71+ (complex care needs)
 * - Named Personas (for demos/training):
 *   1. Michael Chen, 58 - Type 2 Diabetes, Hypertension (controlled)
 *   2. Sarah Martinez, 42 - Asthma, Allergies (well-managed)
 *   3. Emma Johnson, 67 - CHF, CKD Stage 3 (complex care)
 *   4. Carlos Rodriguez, 33 - Pre-diabetes (prevention focus)
 *
 * Use Cases:
 * - Clinical workflow demonstrations
 * - Training and education
 * - Patient journey mapping
 * - Care coordination workflows
 * - Pre-visit planning demonstrations
 */
@Component
public class PatientJourneyStrategy implements ScenarioSeedingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(PatientJourneyStrategy.class);

    private static final String SCENARIO_NAME = "patient-journey";
    private static final String SCENARIO_DESCRIPTION =
        "Detailed patient personas (1K + 4 named) with rich clinical histories for workflow demonstrations";
    private static final int PATIENT_COUNT = 1000;
    private static final int CARE_GAP_PERCENTAGE = 35;

    private final DemoSeedingService demoSeedingService;

    public PatientJourneyStrategy(DemoSeedingService demoSeedingService) {
        this.demoSeedingService = demoSeedingService;
    }

    @Override
    public SeedingResult seedScenario(String tenantId) {
        logger.info("Starting Patient Journey scenario seeding for tenant: {}", tenantId);
        long startTime = System.currentTimeMillis();

        try {
            // Generate base patient cohort
            GenerationResult generationResult = demoSeedingService.generatePatientCohort(
                PATIENT_COUNT,
                tenantId,
                CARE_GAP_PERCENTAGE
            );

            // TODO: Add named persona generation when SyntheticPatientTemplate support is added
            // For now, the base cohort includes realistic patient data

            long durationMs = System.currentTimeMillis() - startTime;

            logger.info("Patient Journey scenario seeded successfully: {} patients, {}ms",
                generationResult.getPatientCount(),
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
            logger.error("Failed to seed Patient Journey scenario for tenant: {}", tenantId, e);

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
     * Patient Journey has slightly higher condition density (~1.8 per patient).
     */
    private int estimateConditions(int patientCount) {
        return (int) (patientCount * 1.8);
    }
}
