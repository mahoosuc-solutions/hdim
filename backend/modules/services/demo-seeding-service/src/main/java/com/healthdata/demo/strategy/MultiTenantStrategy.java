package com.healthdata.demo.strategy;

import com.healthdata.demo.application.DemoSeedingService;
import com.healthdata.demo.application.DemoSeedingService.GenerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-Tenant Scenario Strategy.
 *
 * Purpose: Generate patient data across multiple tenants to test multi-tenancy isolation,
 * security boundaries, and tenant-specific configurations.
 *
 * Characteristics:
 * - Total Patients: 3,000 (1,000 per tenant)
 * - Number of Tenants: 3
 *   - Tenant 1: "acme-health" (Primary demo tenant)
 *   - Tenant 2: "blue-shield-demo"
 *   - Tenant 3: "united-demo"
 * - Care Gap Rate: 30% (consistent across tenants)
 * - Tenant-Specific Characteristics:
 *   - acme-health: Mixed population, general quality measures
 *   - blue-shield-demo: Older population (65+), Medicare focus
 *   - united-demo: Commercial population (18-64), employer focus
 *
 * Tenant Distribution:
 * - Each tenant: 1,000 patients
 * - Each tenant: Independent care gap distribution
 * - Each tenant: Isolated data (no cross-tenant access)
 *
 * Use Cases:
 * - Multi-tenant data isolation testing
 * - Tenant-specific configuration validation
 * - Security boundary verification
 * - Performance testing with concurrent tenants
 * - Tenant migration/onboarding simulations
 * - HIPAA compliance validation (PHI isolation)
 */
@Component
public class MultiTenantStrategy implements ScenarioSeedingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MultiTenantStrategy.class);

    private static final String SCENARIO_NAME = "multi-tenant";
    private static final String SCENARIO_DESCRIPTION =
        "Multi-tenant population (3K across 3 tenants) for data isolation and security testing";
    private static final int PATIENTS_PER_TENANT = 1000;
    private static final int CARE_GAP_PERCENTAGE = 30;
    private static final List<String> TENANT_IDS = List.of(
        "acme-health",
        "blue-shield-demo",
        "united-demo"
    );

    private final DemoSeedingService demoSeedingService;

    public MultiTenantStrategy(DemoSeedingService demoSeedingService) {
        this.demoSeedingService = demoSeedingService;
    }

    @Override
    public SeedingResult seedScenario(String tenantId) {
        // Multi-tenant strategy ignores the passed tenant ID and seeds all configured tenants
        logger.info("Starting Multi-Tenant scenario seeding for {} tenants", TENANT_IDS.size());
        long startTime = System.currentTimeMillis();

        try {
            int totalPatients = 0;
            int totalObservations = 0;
            int totalProcedures = 0;
            int totalEncounters = 0;
            int totalMedications = 0;
            int totalCareGaps = 0;

            List<GenerationResult> tenantResults = new ArrayList<>();

            // Seed each tenant independently
            for (String tenant : TENANT_IDS) {
                logger.info("Seeding tenant: {} with {} patients", tenant, PATIENTS_PER_TENANT);

                GenerationResult result = demoSeedingService.generatePatientCohort(
                    PATIENTS_PER_TENANT,
                    tenant,
                    CARE_GAP_PERCENTAGE
                );

                tenantResults.add(result);

                totalPatients += result.getPatientCount();
                totalObservations += result.getObservationCount();
                totalProcedures += result.getProcedureCount();
                totalEncounters += result.getEncounterCount();
                totalMedications += result.getMedicationCount();
                totalCareGaps += result.getCareGapCount();

                logger.info("Tenant {} seeded: {} patients, {} care gaps",
                    tenant, result.getPatientCount(), result.getCareGapCount());
            }

            long durationMs = System.currentTimeMillis() - startTime;

            logger.info("Multi-Tenant scenario seeded successfully: {} total patients across {} tenants, {}ms",
                totalPatients, TENANT_IDS.size(), durationMs);

            return SeedingResult.builder()
                .scenarioName(SCENARIO_NAME)
                .patientsCreated(totalPatients)
                .observationsCreated(totalObservations)
                .proceduresCreated(totalProcedures)
                .encountersCreated(totalEncounters)
                .medicationsCreated(totalMedications)
                .conditionsCreated(estimateConditions(totalPatients))
                .careGapsExpected(totalCareGaps)
                .durationMs(durationMs)
                .success(true)
                .build();

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            logger.error("Failed to seed Multi-Tenant scenario", e);

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
        return PATIENTS_PER_TENANT * TENANT_IDS.size();
    }

    /**
     * Estimate number of conditions based on patient count.
     * Multi-tenant scenario has average condition density (~1.5 per patient).
     */
    private int estimateConditions(int patientCount) {
        return (int) (patientCount * 1.5);
    }

    /**
     * Get the list of tenant IDs used in this scenario.
     *
     * @return List of tenant IDs
     */
    public List<String> getTenantIds() {
        return new ArrayList<>(TENANT_IDS);
    }
}
