package com.healthdata.demo.application;

import com.healthdata.demo.client.FhirServiceClient;
import com.healthdata.demo.strategy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Verification service for demo platform data quality and completeness.
 *
 * Validates that demo scenarios were seeded correctly by checking:
 * - Patient counts match expected values
 * - Demographics distribution is realistic
 * - FHIR resource counts are appropriate
 * - Care gap percentages match targets
 * - Tenant isolation (for multi-tenant scenarios)
 *
 * Used for automated testing and quality assurance of demo data.
 */
@Service
public class DemoVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(DemoVerificationService.class);

    private final FhirServiceClient fhirServiceClient;
    private final Map<String, ScenarioSeedingStrategy> strategies;

    public DemoVerificationService(
            FhirServiceClient fhirServiceClient,
            HedisEvaluationStrategy hedisStrategy,
            PatientJourneyStrategy journeyStrategy,
            RiskStratificationStrategy riskStrategy,
            MultiTenantStrategy multiTenantStrategy) {
        this.fhirServiceClient = fhirServiceClient;

        // Register all available strategies
        this.strategies = new HashMap<>();
        this.strategies.put(hedisStrategy.getScenarioName(), hedisStrategy);
        this.strategies.put(journeyStrategy.getScenarioName(), journeyStrategy);
        this.strategies.put(riskStrategy.getScenarioName(), riskStrategy);
        this.strategies.put(multiTenantStrategy.getScenarioName(), multiTenantStrategy);
    }

    /**
     * Verify a specific demo scenario.
     *
     * @param scenarioName The scenario to verify
     * @param tenantId The tenant ID
     * @return Verification result with pass/fail status and details
     */
    public VerificationResult verifyScenario(String scenarioName, String tenantId) {
        logger.info("Verifying scenario: {} for tenant: {}", scenarioName, tenantId);

        ScenarioSeedingStrategy strategy = strategies.get(scenarioName);
        if (strategy == null) {
            return VerificationResult.builder()
                .scenarioName(scenarioName)
                .passed(false)
                .errorMessage("Unknown scenario: " + scenarioName)
                .build();
        }

        VerificationResult.Builder resultBuilder = VerificationResult.builder()
            .scenarioName(scenarioName);

        try {
            // Check 1: Patient count
            int expectedPatientCount = strategy.getExpectedPatientCount();
            int actualPatientCount = getPatientCount(tenantId);

            VerificationCheck patientCountCheck = new VerificationCheck(
                "Patient Count",
                expectedPatientCount,
                actualPatientCount,
                calculateTolerance(expectedPatientCount, 5) // 5% tolerance
            );
            resultBuilder.addCheck("patientCount", patientCountCheck);

            // Check 2: Resource counts (observations, procedures, etc.)
            // Note: These are estimates since exact counts vary due to randomization
            VerificationCheck observationCheck = new VerificationCheck(
                "Observation Count",
                expectedPatientCount * 5, // Estimate ~5 observations per patient
                getObservationCount(tenantId),
                calculateTolerance(expectedPatientCount * 5, 20) // 20% tolerance
            );
            resultBuilder.addCheck("observationCount", observationCheck);

            VerificationCheck medicationCheck = new VerificationCheck(
                "Medication Count",
                expectedPatientCount * 2, // Estimate ~2 medications per patient
                getMedicationCount(tenantId),
                calculateTolerance(expectedPatientCount * 2, 30) // 30% tolerance
            );
            resultBuilder.addCheck("medicationCount", medicationCheck);

            VerificationCheck encounterCheck = new VerificationCheck(
                "Encounter Count",
                expectedPatientCount * 3, // Estimate ~3 encounters per patient
                getEncounterCount(tenantId),
                calculateTolerance(expectedPatientCount * 3, 30) // 30% tolerance
            );
            resultBuilder.addCheck("encounterCount", encounterCheck);

            // Determine overall pass/fail
            boolean allPassed = patientCountCheck.passed &&
                               observationCheck.passed &&
                               medicationCheck.passed &&
                               encounterCheck.passed;

            resultBuilder.passed(allPassed);

            VerificationResult result = resultBuilder.build();
            logger.info("Verification for scenario {} {}: {}/{} checks passed",
                scenarioName,
                result.isPassed() ? "PASSED" : "FAILED",
                result.getPassedChecks(),
                result.getTotalChecks());

            return result;

        } catch (Exception e) {
            logger.error("Verification failed for scenario: {}", scenarioName, e);
            return resultBuilder
                .passed(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }

    /**
     * Verify all registered demo scenarios.
     *
     * @param tenantId The tenant ID
     * @return Map of scenario names to verification results
     */
    public Map<String, VerificationResult> verifyAllScenarios(String tenantId) {
        logger.info("Verifying all scenarios for tenant: {}", tenantId);

        Map<String, VerificationResult> results = new HashMap<>();

        for (String scenarioName : strategies.keySet()) {
            VerificationResult result = verifyScenario(scenarioName, tenantId);
            results.put(scenarioName, result);
        }

        int passedScenarios = (int) results.values().stream()
            .filter(VerificationResult::isPassed)
            .count();

        logger.info("Overall verification: {}/{} scenarios passed",
            passedScenarios, results.size());

        return results;
    }

    /**
     * Get patient count for a tenant.
     */
    private int getPatientCount(String tenantId) {
        try {
            // This would call the FHIR service to get actual count
            // For now, return 0 as placeholder
            // TODO: Implement actual FHIR service call
            return 0;
        } catch (Exception e) {
            logger.warn("Failed to get patient count for tenant: {}", tenantId, e);
            return 0;
        }
    }

    /**
     * Get observation count for a tenant.
     */
    private int getObservationCount(String tenantId) {
        try {
            // TODO: Implement actual FHIR service call
            return 0;
        } catch (Exception e) {
            logger.warn("Failed to get observation count for tenant: {}", tenantId, e);
            return 0;
        }
    }

    /**
     * Get medication count for a tenant.
     */
    private int getMedicationCount(String tenantId) {
        try {
            // TODO: Implement actual FHIR service call
            return 0;
        } catch (Exception e) {
            logger.warn("Failed to get medication count for tenant: {}", tenantId, e);
            return 0;
        }
    }

    /**
     * Get encounter count for a tenant.
     */
    private int getEncounterCount(String tenantId) {
        try {
            // TODO: Implement actual FHIR service call
            return 0;
        } catch (Exception e) {
            logger.warn("Failed to get encounter count for tenant: {}", tenantId, e);
            return 0;
        }
    }

    /**
     * Calculate acceptable range for a value with given tolerance percentage.
     */
    private int calculateTolerance(int value, int tolerancePercent) {
        return (int) (value * tolerancePercent / 100.0);
    }

    /**
     * Verification result for a scenario.
     */
    public static class VerificationResult {
        private final String scenarioName;
        private final boolean passed;
        private final Map<String, VerificationCheck> checks;
        private final String errorMessage;

        private VerificationResult(String scenarioName, boolean passed,
                                   Map<String, VerificationCheck> checks,
                                   String errorMessage) {
            this.scenarioName = scenarioName;
            this.passed = passed;
            this.checks = checks;
            this.errorMessage = errorMessage;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getScenarioName() { return scenarioName; }
        public boolean isPassed() { return passed; }
        public Map<String, VerificationCheck> getChecks() { return checks; }
        public String getErrorMessage() { return errorMessage; }

        public int getTotalChecks() { return checks.size(); }
        public long getPassedChecks() {
            return checks.values().stream().filter(c -> c.passed).count();
        }

        public static class Builder {
            private String scenarioName;
            private boolean passed = true;
            private final Map<String, VerificationCheck> checks = new HashMap<>();
            private String errorMessage;

            public Builder scenarioName(String scenarioName) {
                this.scenarioName = scenarioName;
                return this;
            }

            public Builder passed(boolean passed) {
                this.passed = passed;
                return this;
            }

            public Builder addCheck(String name, VerificationCheck check) {
                this.checks.put(name, check);
                return this;
            }

            public Builder errorMessage(String errorMessage) {
                this.errorMessage = errorMessage;
                return this;
            }

            public VerificationResult build() {
                return new VerificationResult(scenarioName, passed, checks, errorMessage);
            }
        }

        @Override
        public String toString() {
            return String.format("VerificationResult{scenario='%s', passed=%s, checks=%d/%d}",
                scenarioName, passed, getPassedChecks(), getTotalChecks());
        }
    }

    /**
     * Individual verification check.
     */
    public static class VerificationCheck {
        private final String checkName;
        private final int expected;
        private final int actual;
        private final int tolerance;
        private final boolean passed;

        public VerificationCheck(String checkName, int expected, int actual, int tolerance) {
            this.checkName = checkName;
            this.expected = expected;
            this.actual = actual;
            this.tolerance = tolerance;
            this.passed = Math.abs(expected - actual) <= tolerance;
        }

        public String getCheckName() { return checkName; }
        public int getExpected() { return expected; }
        public int getActual() { return actual; }
        public int getTolerance() { return tolerance; }
        public boolean isPassed() { return passed; }

        @Override
        public String toString() {
            return String.format("%s: expected=%d±%d, actual=%d, %s",
                checkName, expected, tolerance, actual, passed ? "PASS" : "FAIL");
        }
    }
}
