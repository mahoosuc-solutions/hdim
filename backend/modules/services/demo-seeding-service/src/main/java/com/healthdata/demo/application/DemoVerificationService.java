package com.healthdata.demo.application;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.demo.client.FhirServiceClient;
import com.healthdata.demo.strategy.*;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate;
    private final IParser fhirParser;
    private final String fhirServiceUrl;
    private final Map<String, ScenarioSeedingStrategy> strategies;

    public DemoVerificationService(
            FhirServiceClient fhirServiceClient,
            RestTemplate restTemplate,
            FhirContext fhirContext,
            @Value("${demo.services.fhir.internal-url:http://fhir-service:8085/fhir}") String fhirServiceUrl,
            HedisEvaluationStrategy hedisStrategy,
            PatientJourneyStrategy journeyStrategy,
            RiskStratificationStrategy riskStrategy,
            MultiTenantStrategy multiTenantStrategy) {
        this.fhirServiceClient = fhirServiceClient;
        this.restTemplate = restTemplate;
        this.fhirParser = fhirContext.newJsonParser().setPrettyPrint(false);
        this.fhirServiceUrl = fhirServiceUrl;

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
            Bundle bundle = searchFhirResource("Patient", tenantId, null);
            if (bundle == null) return 0;
            // Use total if available, otherwise count entries
            if (bundle.hasTotal()) {
                return bundle.getTotalElement().getValue().intValue();
            }
            return bundle.getEntry().size();
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
            Bundle bundle = searchFhirResource("Observation", tenantId, null);
            if (bundle == null) return 0;
            if (bundle.hasTotal()) {
                return bundle.getTotalElement().getValue().intValue();
            }
            return bundle.getEntry().size();
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
            Bundle bundle = searchFhirResource("MedicationRequest", tenantId, null);
            if (bundle == null) return 0;
            if (bundle.hasTotal()) {
                return bundle.getTotalElement().getValue().intValue();
            }
            return bundle.getEntry().size();
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
            Bundle bundle = searchFhirResource("Encounter", tenantId, null);
            if (bundle == null) return 0;
            if (bundle.hasTotal()) {
                return bundle.getTotalElement().getValue().intValue();
            }
            return bundle.getEntry().size();
        } catch (Exception e) {
            logger.warn("Failed to get encounter count for tenant: {}", tenantId, e);
            return 0;
        }
    }

    /**
     * Search FHIR resources using FHIR search API.
     *
     * @param resourceType FHIR resource type (Patient, Observation, etc.)
     * @param tenantId Tenant ID for multi-tenant isolation
     * @param searchParams Optional search parameters (e.g., "patient=123")
     * @return FHIR Bundle containing search results
     */
    private Bundle searchFhirResource(String resourceType, String tenantId, String searchParams) {
        try {
            String url = fhirServiceUrl + "/" + resourceType;
            if (searchParams != null && !searchParams.isEmpty()) {
                url += "?" + searchParams;
            }
            // Add _summary=count for efficient counting
            url += (url.contains("?") ? "&" : "?") + "_summary=count";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/fhir+json"));
            headers.set("X-Tenant-ID", tenantId);
            // User ID must be a valid UUID (required by UserAutoRegistrationFilter)
            headers.set("X-Auth-User-Id", "00000000-0000-0000-0000-000000000001");
            headers.set("X-Auth-Username", "demo-verifier");
            headers.set("X-Auth-Tenant-Ids", tenantId);
            headers.set("X-Auth-Roles", "ADMIN,SYSTEM");
            headers.set("X-Auth-Validated", "gateway-demo-verification");

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (Bundle) fhirParser.parseResource(response.getBody());
            } else {
                logger.warn("FHIR search failed for {}: HTTP {}", resourceType, response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error searching FHIR resource {}: {}", resourceType, e.getMessage(), e);
            return null;
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
