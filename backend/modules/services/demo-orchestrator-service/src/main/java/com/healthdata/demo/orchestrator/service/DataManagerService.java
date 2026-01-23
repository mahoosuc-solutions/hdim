package com.healthdata.demo.orchestrator.service;

import com.healthdata.demo.orchestrator.integration.DevOpsAgentClient;
import com.healthdata.demo.orchestrator.model.FhirValidationResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataManagerService {
    private final DevOpsAgentClient devopsAgent;

    @Value("${hdim.demo.tenant-id:demo-tenant}")
    private String demoTenantId;

    @Value("${hdim.services.gateway.url:http://gateway-service:8001}")
    private String gatewayUrl;

    @Value("${hdim.services.demo-seeding.url:http://demo-seeding-service:8098}")
    private String demoSeedingServiceUrl;

    private final WebClient webClient = WebClient.builder()
        .defaultHeader("X-Tenant-ID", demoTenantId)
        .build();

    /**
     * Seed base demo data for demo environments.
     *
     * Generates realistic patient data via demo-seeding-service:
     * - 100 synthetic patients with demographics and clinical history
     * - 40% care gap rate (realistic for HEDIS measures)
     * - Full FHIR resource generation (medications, observations, encounters, procedures)
     *
     * This method delegates to demo-seeding-service's comprehensive seeding infrastructure
     * which includes progress tracking, cancellation support, and configurable persistence.
     */
    public void seedBaseData() {
        log.info("Seeding base demo data...");
        devopsAgent.publishLog("INFO", "Seeding base demo data...", "SEED");

        try {
            // Step 1: Initialize scenarios and templates
            devopsAgent.publishLog("INFO", "Initializing demo scenarios...", "SEED");
            initializeScenarios();

            // Step 2: Generate patient cohort
            devopsAgent.publishLog("INFO", "Generating patient cohort...", "SEED");
            GenerationResult result = generatePatientCohort(100, demoTenantId, 40);

            if (result.isSuccess()) {
                String summary = String.format(
                    "Base data seeding completed: %d patients, %d care gaps, %d medications, %d observations, %d encounters, %d procedures (took %d ms)",
                    result.getPatientCount(),
                    result.getCareGapCount(),
                    result.getMedicationCount(),
                    result.getObservationCount(),
                    result.getEncounterCount(),
                    result.getProcedureCount(),
                    result.getGenerationTimeMs()
                );
                log.info(summary);
                devopsAgent.publishLog("INFO", summary, "SEED");
            } else {
                String errorMsg = "Base data seeding failed: " + result.getErrorMessage();
                log.error(errorMsg);
                devopsAgent.publishLog("ERROR", errorMsg, "SEED");
            }

        } catch (Exception e) {
            String errorMsg = "Fatal error during base data seeding: " + e.getMessage();
            log.error(errorMsg, e);
            devopsAgent.publishLog("ERROR", errorMsg, "SEED");
        }
    }

    /**
     * Initialize demo scenarios and patient templates.
     *
     * Calls demo-seeding-service to set up:
     * - HEDIS Evaluation scenario (500 patients)
     * - Patient Journey scenario (250 patients)
     * - Risk Stratification scenario (1000 patients)
     * - Multi-Tenant scenario (1500 patients)
     */
    private void initializeScenarios() {
        try {
            WebClient seedingClient = WebClient.builder()
                .baseUrl(demoSeedingServiceUrl)
                .defaultHeader("X-Tenant-ID", demoTenantId)
                .build();

            String response = seedingClient.post()
                .uri("/demo/api/v1/demo/initialize")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();

            log.debug("Initialize scenarios response: {}", response);

        } catch (Exception e) {
            log.warn("Failed to initialize scenarios: {}", e.getMessage());
            // Non-fatal - continue with patient generation
        }
    }

    /**
     * Generate patient cohort via demo-seeding-service.
     *
     * @param count number of patients to generate
     * @param tenantId tenant ID for multi-tenant isolation
     * @param careGapPercentage percentage of patients with care gaps (0-100)
     * @return generation result with counts and timing
     */
    private GenerationResult generatePatientCohort(int count, String tenantId, int careGapPercentage) {
        try {
            WebClient seedingClient = WebClient.builder()
                .baseUrl(demoSeedingServiceUrl)
                .defaultHeader("X-Tenant-ID", tenantId)
                .build();

            String requestBody = String.format(
                "{\"count\": %d, \"tenantId\": \"%s\", \"careGapPercentage\": %d}",
                count, tenantId, careGapPercentage
            );

            GenerationResult result = seedingClient.post()
                .uri("/demo/api/v1/demo/patients/generate")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(GenerationResult.class)
                .timeout(Duration.ofMinutes(5))
                .block();

            return result != null ? result : GenerationResult.failure("No response from demo-seeding-service");

        } catch (Exception e) {
            log.error("Failed to generate patient cohort: {}", e.getMessage(), e);
            return GenerationResult.failure(e.getMessage());
        }
    }

    /**
     * Clear all demo tenant data
     *
     * Cascades delete operations across all HDIM services for the demo tenant.
     * Uses tenant-based filtering to ensure only demo data is cleared (not production).
     * Maintains audit trail of all clearing operations via DevOps agent logs.
     *
     * Safety Features:
     * - Only clears data for configured demo tenant ID
     * - Uses soft deletes where possible to preserve audit trail
     * - Logs all operations for compliance
     * - Circuit breaker: Continues even if individual service calls fail
     *
     * @return ClearDataResult with summary of operations
     */
    public ClearDataResult clearAllData() {
        log.info("Clearing all demo data for tenant: {}", demoTenantId);
        devopsAgent.publishLog("INFO",
            String.format("Starting data clearing for demo tenant: %s", demoTenantId),
            "CLEAR");

        List<String> successfulClears = new ArrayList<>();
        List<String> failedClears = new ArrayList<>();
        int totalEntitiesCleared = 0;

        try {
            // 1. Clear patients (cascades to related data)
            devopsAgent.publishLog("INFO", "Clearing patient data...", "CLEAR");
            try {
                int patientCount = clearServiceData("patient-service", "/api/v1/patients/demo/clear");
                successfulClears.add("Patients: " + patientCount);
                totalEntitiesCleared += patientCount;
                devopsAgent.publishLog("INFO",
                    String.format("Cleared %d patients", patientCount),
                    "CLEAR");
            } catch (Exception e) {
                log.error("Failed to clear patient data: {}", e.getMessage());
                failedClears.add("Patients: " + e.getMessage());
                devopsAgent.publishLog("WARN",
                    "Failed to clear patient data: " + e.getMessage(),
                    "CLEAR");
            }

            // 2. Clear care gaps
            devopsAgent.publishLog("INFO", "Clearing care gap data...", "CLEAR");
            try {
                int careGapCount = clearServiceData("care-gap-service", "/api/v1/care-gaps/demo/clear");
                successfulClears.add("Care Gaps: " + careGapCount);
                totalEntitiesCleared += careGapCount;
                devopsAgent.publishLog("INFO",
                    String.format("Cleared %d care gaps", careGapCount),
                    "CLEAR");
            } catch (Exception e) {
                log.error("Failed to clear care gap data: {}", e.getMessage());
                failedClears.add("Care Gaps: " + e.getMessage());
                devopsAgent.publishLog("WARN",
                    "Failed to clear care gap data: " + e.getMessage(),
                    "CLEAR");
            }

            // 3. Clear quality measure evaluations
            devopsAgent.publishLog("INFO", "Clearing quality measure evaluations...", "CLEAR");
            try {
                int evaluationCount = clearServiceData("quality-measure-service", "/api/v1/evaluations/demo/clear");
                successfulClears.add("Evaluations: " + evaluationCount);
                totalEntitiesCleared += evaluationCount;
                devopsAgent.publishLog("INFO",
                    String.format("Cleared %d evaluations", evaluationCount),
                    "CLEAR");
            } catch (Exception e) {
                log.error("Failed to clear evaluation data: {}", e.getMessage());
                failedClears.add("Evaluations: " + e.getMessage());
                devopsAgent.publishLog("WARN",
                    "Failed to clear evaluation data: " + e.getMessage(),
                    "CLEAR");
            }

            // 4. Clear FHIR resources
            devopsAgent.publishLog("INFO", "Clearing FHIR resources...", "CLEAR");
            try {
                int fhirCount = clearServiceData("fhir-service", "/api/v1/fhir/demo/clear");
                successfulClears.add("FHIR Resources: " + fhirCount);
                totalEntitiesCleared += fhirCount;
                devopsAgent.publishLog("INFO",
                    String.format("Cleared %d FHIR resources", fhirCount),
                    "CLEAR");
            } catch (Exception e) {
                log.error("Failed to clear FHIR data: {}", e.getMessage());
                failedClears.add("FHIR Resources: " + e.getMessage());
                devopsAgent.publishLog("WARN",
                    "Failed to clear FHIR data: " + e.getMessage(),
                    "CLEAR");
            }

            // 5. Clear predictions
            devopsAgent.publishLog("INFO", "Clearing predictive analytics data...", "CLEAR");
            try {
                int predictionCount = clearServiceData("predictive-analytics-service", "/api/v1/predictions/demo/clear");
                successfulClears.add("Predictions: " + predictionCount);
                totalEntitiesCleared += predictionCount;
                devopsAgent.publishLog("INFO",
                    String.format("Cleared %d predictions", predictionCount),
                    "CLEAR");
            } catch (Exception e) {
                log.error("Failed to clear prediction data: {}", e.getMessage());
                failedClears.add("Predictions: " + e.getMessage());
                devopsAgent.publishLog("WARN",
                    "Failed to clear prediction data: " + e.getMessage(),
                    "CLEAR");
            }

            // Summary
            String summary = String.format(
                "Data clearing completed: %d entities cleared, %d successful operations, %d failed operations",
                totalEntitiesCleared, successfulClears.size(), failedClears.size()
            );

            log.info(summary);
            devopsAgent.publishLog(failedClears.isEmpty() ? "INFO" : "WARN", summary, "CLEAR");

            return ClearDataResult.builder()
                .success(true)
                .totalEntitiesCleared(totalEntitiesCleared)
                .successfulOperations(successfulClears)
                .failedOperations(failedClears)
                .message(summary)
                .build();

        } catch (Exception e) {
            String errorMsg = "Fatal error during data clearing: " + e.getMessage();
            log.error(errorMsg, e);
            devopsAgent.publishLog("ERROR", errorMsg, "CLEAR");

            return ClearDataResult.builder()
                .success(false)
                .totalEntitiesCleared(totalEntitiesCleared)
                .successfulOperations(successfulClears)
                .failedOperations(failedClears)
                .message(errorMsg)
                .build();
        }
    }

    /**
     * Call service clear endpoint via gateway
     *
     * @param serviceName the service name (for logging)
     * @param endpoint the clear endpoint path
     * @return count of entities cleared
     */
    private int clearServiceData(String serviceName, String endpoint) {
        try {
            String fullUrl = gatewayUrl + endpoint;
            log.debug("Calling {} clear endpoint: {}", serviceName, fullUrl);

            Integer count = webClient.delete()
                .uri(fullUrl)
                .header("X-Tenant-ID", demoTenantId)
                .retrieve()
                .bodyToMono(Integer.class)
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(e -> {
                    log.warn("Service {} returned error, assuming 0 cleared: {}",
                        serviceName, e.getMessage());
                    return Mono.just(0);
                })
                .block();

            return count != null ? count : 0;

        } catch (Exception e) {
            log.error("Failed to clear data for service {}: {}", serviceName, e.getMessage());
            throw new RuntimeException("Failed to clear " + serviceName + " data", e);
        }
    }

    /**
     * DTO for patient generation results from demo-seeding-service.
     */
    private static class GenerationResult {
        private String tenantId;
        private int patientCount;
        private int careGapCount;
        private int medicationCount;
        private int observationCount;
        private int encounterCount;
        private int procedureCount;
        private long generationTimeMs;
        private boolean success;
        private String errorMessage;

        // Getters and setters
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }

        public int getPatientCount() { return patientCount; }
        public void setPatientCount(int patientCount) { this.patientCount = patientCount; }

        public int getCareGapCount() { return careGapCount; }
        public void setCareGapCount(int careGapCount) { this.careGapCount = careGapCount; }

        public int getMedicationCount() { return medicationCount; }
        public void setMedicationCount(int medicationCount) { this.medicationCount = medicationCount; }

        public int getObservationCount() { return observationCount; }
        public void setObservationCount(int observationCount) { this.observationCount = observationCount; }

        public int getEncounterCount() { return encounterCount; }
        public void setEncounterCount(int encounterCount) { this.encounterCount = encounterCount; }

        public int getProcedureCount() { return procedureCount; }
        public void setProcedureCount(int procedureCount) { this.procedureCount = procedureCount; }

        public long getGenerationTimeMs() { return generationTimeMs; }
        public void setGenerationTimeMs(long generationTimeMs) { this.generationTimeMs = generationTimeMs; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public static GenerationResult failure(String errorMessage) {
            GenerationResult result = new GenerationResult();
            result.setSuccess(false);
            result.setErrorMessage(errorMessage);
            return result;
        }
    }

    /**
     * Result DTO for clear data operation
     */
    @lombok.Data
    @lombok.Builder
    public static class ClearDataResult {
        private boolean success;
        private int totalEntitiesCleared;
        private List<String> successfulOperations;
        private List<String> failedOperations;
        private String message;
    }

    public FhirValidationResultDto validateFhirData() {
        log.info("Validating FHIR demo data authenticity...");
        devopsAgent.publishLog("INFO", "Validating FHIR demo data...", "VALIDATION");

        FhirValidationResultDto result = devopsAgent.validateFhirDemoData();

        if (result != null) {
            String status = result.getOverallStatus();
            devopsAgent.updateStatus("FHIR_VALIDATION", status,
                java.util.Map.of(
                    "totalChecks", result.getTotalChecks(),
                    "passedChecks", result.getPassedChecks(),
                    "failedChecks", result.getFailedChecks(),
                    "warningChecks", result.getWarningChecks()
                ));

            devopsAgent.publishLog(status.equals("PASS") ? "INFO" : "WARN",
                String.format("FHIR validation: %s (%d passed, %d failed, %d warnings)",
                    status, result.getPassedChecks(), result.getFailedChecks(),
                    result.getWarningChecks()),
                "VALIDATION");
        } else {
            devopsAgent.publishLog("ERROR", "FHIR validation failed - could not retrieve results", "VALIDATION");
        }

        return result;
    }
}
