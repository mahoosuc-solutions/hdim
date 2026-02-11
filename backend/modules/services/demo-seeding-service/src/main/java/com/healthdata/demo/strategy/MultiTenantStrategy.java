package com.healthdata.demo.strategy;

import com.healthdata.demo.application.DemoSeedingService;
import com.healthdata.demo.application.DemoSeedingService.GenerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private static final int DEFAULT_PATIENTS_PER_TENANT = 200;
    private static final int DEFAULT_CARE_GAP_PERCENTAGE = 30;
    private final DemoSeedingService demoSeedingService;
    private final int patientsPerTenant;
    private final int careGapPercentage;
    private final List<String> tenantIds;

    public MultiTenantStrategy(
            DemoSeedingService demoSeedingService,
            @Value("${demo.multi-tenant.patients-per-tenant:" + DEFAULT_PATIENTS_PER_TENANT + "}") int patientsPerTenant,
            @Value("${demo.multi-tenant.care-gap-percentage:" + DEFAULT_CARE_GAP_PERCENTAGE + "}") int careGapPercentage,
            @Value("#{'${demo.multi-tenant.tenant-ids:acme-health,blue-shield-demo,united-demo}'.split(',')}")
            List<String> tenantIds) {
        this.demoSeedingService = demoSeedingService;
        this.patientsPerTenant = patientsPerTenant;
        this.careGapPercentage = careGapPercentage;
        this.tenantIds = sanitizeTenantIds(tenantIds);
    }

    @Override
    public SeedingResult seedScenario(String tenantId) {
        return seedScenarioWithOverrides(null, null);
    }

    public SeedingResult seedScenarioWithOverrides(Integer patientsPerTenantOverride, Integer careGapPercentageOverride) {
        int resolvedPatientsPerTenant = patientsPerTenantOverride != null ? patientsPerTenantOverride : patientsPerTenant;
        int resolvedCareGapPercentage = careGapPercentageOverride != null ? careGapPercentageOverride : careGapPercentage;

        if (resolvedPatientsPerTenant <= 0) {
            return SeedingResult.builder()
                .scenarioName(SCENARIO_NAME)
                .success(false)
                .errorMessage("patientsPerTenant must be greater than zero")
                .build();
        }

        if (resolvedCareGapPercentage < 0 || resolvedCareGapPercentage > 100) {
            return SeedingResult.builder()
                .scenarioName(SCENARIO_NAME)
                .success(false)
                .errorMessage("careGapPercentage must be between 0 and 100")
                .build();
        }

        // Multi-tenant strategy ignores the passed tenant ID and seeds all configured tenants
        logger.info("Starting Multi-Tenant scenario seeding for {} tenants", tenantIds.size());
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
            for (String tenant : tenantIds) {
                logger.info("Seeding tenant: {} with {} patients", tenant, resolvedPatientsPerTenant);

                GenerationResult result = demoSeedingService.generatePatientCohort(
                    resolvedPatientsPerTenant,
                    tenant,
                    resolvedCareGapPercentage
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
                totalPatients, tenantIds.size(), durationMs);

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
        return patientsPerTenant * tenantIds.size();
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
        return new ArrayList<>(tenantIds);
    }

    private List<String> sanitizeTenantIds(List<String> tenantIds) {
        if (tenantIds == null) {
            return List.of();
        }
        return tenantIds.stream()
            .map(String::trim)
            .filter(id -> !id.isEmpty())
            .distinct()
            .collect(Collectors.toList());
    }
}
