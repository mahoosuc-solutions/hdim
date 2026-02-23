package com.healthdata.demo.client;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Client for communicating with the Quality Measure Service.
 *
 * Used during demo seeding to:
 * 1. Seed HEDIS measure definitions
 * 2. Generate evaluation results for demo patients
 */
@Component
@Slf4j
public class QualityMeasureServiceClient {

    private final RestTemplate restTemplate;

    @Value("${quality-measure.service.url:http://quality-measure-service:8087/quality-measure}")
    private String qualityMeasureServiceUrl;

    @Value("${gateway.auth.signing-secret:dev-signing-secret-for-local-testing-only-minimum-32-chars}")
    private String signingSecret;

    // Standard HEDIS measures to evaluate
    private static final List<String> HEDIS_MEASURES = Arrays.asList(
        "CDC",  // Comprehensive Diabetes Care
        "BCS",  // Breast Cancer Screening
        "COL",  // Colorectal Cancer Screening
        "CBP",  // Controlling Blood Pressure
        "CCS",  // Cervical Cancer Screening
        "SPC"   // Statin Therapy
    );

    public QualityMeasureServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Check if the quality measure service is available.
     */
    public boolean isServiceAvailable() {
        try {
            String healthUrl = qualityMeasureServiceUrl + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Quality Measure service not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Seed HEDIS measure definitions for a tenant.
     *
     * @param tenantId Tenant to seed measures for
     * @return Number of measures seeded
     */
    public int seedMeasureDefinitions(String tenantId) {
        try {
            String url = qualityMeasureServiceUrl + "/api/v1/measures/seed";
            HttpHeaders headers = createHeaders(tenantId);

            ResponseEntity<SeedResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(headers), SeedResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Seeded {} HEDIS measure definitions for tenant: {}",
                    response.getBody().getSeededCount(), tenantId);
                return response.getBody().getSeededCount();
            }
        } catch (Exception e) {
            log.warn("Failed to seed measure definitions: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * Generate evaluation results for a batch of patients.
     *
     * @param patientBundle Bundle containing patients
     * @param tenantId Tenant ID
     * @param targetCompliance Target compliance rate (0-100)
     * @return Number of results generated
     */
    public int generateEvaluationResults(Bundle patientBundle, String tenantId, int targetCompliance) {
        int resultsGenerated = 0;

        List<String> patientIds = extractPatientIds(patientBundle);
        log.info("Generating evaluation results for {} patients, target compliance: {}%",
            patientIds.size(), targetCompliance);

        // Process patients in batches of 50
        int batchSize = 50;
        for (int i = 0; i < patientIds.size(); i += batchSize) {
            List<String> batch = patientIds.subList(i, Math.min(i + batchSize, patientIds.size()));
            int batchResults = evaluateBatch(batch, tenantId, targetCompliance);
            resultsGenerated += batchResults;

            if ((i + batchSize) % 200 == 0) {
                log.info("Processed {}/{} patients ({} results)",
                    Math.min(i + batchSize, patientIds.size()), patientIds.size(), resultsGenerated);
            }
        }

        log.info("Generated {} evaluation results for {} patients",
            resultsGenerated, patientIds.size());
        return resultsGenerated;
    }

    /**
     * Evaluate measures for a single patient using local calculation.
     *
     * @param patientId Patient UUID
     * @param tenantId Tenant ID
     * @return Number of measures evaluated
     */
    public int evaluatePatient(UUID patientId, String tenantId) {
        int evaluated = 0;

        for (String measureId : HEDIS_MEASURES) {
            try {
                String url = qualityMeasureServiceUrl + "/api/v1/evaluate/local" +
                    "?patientId=" + patientId + "&measureId=" + measureId;

                HttpHeaders headers = createHeaders(tenantId);
                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(headers), String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    evaluated++;
                }
            } catch (Exception e) {
                log.debug("Measure {} evaluation failed for patient {}: {}",
                    measureId, patientId, e.getMessage());
            }
        }

        return evaluated;
    }

    /**
     * Generate evaluation results using real CQL evaluation first, with mock fallback.
     * Tries the CQL engine endpoint which runs actual HEDIS measure logic.
     * Falls back to mock random results if CQL engine is unavailable.
     */
    public int generateDemoResults(Bundle patientBundle, String tenantId, int careGapPercentage) {
        List<String> patientIds = extractPatientIds(patientBundle);

        // Try real CQL evaluation first
        int cqlResults = evaluateWithCql(patientIds, tenantId);
        if (cqlResults > 0) {
            return cqlResults;
        }

        // Fallback: mock results (Random-based) if CQL engine is unavailable
        log.warn("CQL evaluation unavailable, falling back to mock results for tenant: {}", tenantId);
        return generateMockResults(patientIds, tenantId, careGapPercentage);
    }

    /**
     * Evaluate patients using real CQL engine via quality-measure-service.
     * Returns 0 if the CQL endpoint is unavailable (triggers mock fallback).
     */
    private int evaluateWithCql(List<String> patientIds, String tenantId) {
        try {
            String url = qualityMeasureServiceUrl + "/api/v1/demo/evaluate-cql";
            HttpHeaders headers = createHeaders(tenantId);
            headers.setContentType(MediaType.APPLICATION_JSON);

            DemoResultsRequest request = new DemoResultsRequest();
            request.setPatientIds(patientIds);
            request.setCareGapPercentage(0); // Not used by CQL endpoint
            request.setMeasureIds(HEDIS_MEASURES);

            log.info("Evaluating {} patients with real CQL engine for tenant: {}",
                patientIds.size(), tenantId);

            ResponseEntity<DemoResultsResponse> response = restTemplate.exchange(
                url, HttpMethod.POST,
                new HttpEntity<>(request, headers),
                DemoResultsResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                int results = response.getBody().getResultsGenerated();
                log.info("CQL evaluation complete: {} results ({} compliant, {} non-compliant) for tenant: {}",
                    results, response.getBody().getCompliantCount(),
                    response.getBody().getNonCompliantCount(), tenantId);
                return results;
            }
        } catch (Exception e) {
            log.warn("CQL evaluation endpoint unavailable: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * Generate mock evaluation results using Random (original behavior).
     * Used as fallback when CQL engine is unavailable.
     */
    private int generateMockResults(List<String> patientIds, String tenantId, int careGapPercentage) {
        try {
            String url = qualityMeasureServiceUrl + "/api/v1/demo/generate-results";
            HttpHeaders headers = createHeaders(tenantId);
            headers.setContentType(MediaType.APPLICATION_JSON);

            DemoResultsRequest request = new DemoResultsRequest();
            request.setPatientIds(patientIds);
            request.setCareGapPercentage(careGapPercentage);
            request.setMeasureIds(HEDIS_MEASURES);

            ResponseEntity<DemoResultsResponse> response = restTemplate.exchange(
                url, HttpMethod.POST,
                new HttpEntity<>(request, headers),
                DemoResultsResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Generated {} mock evaluation results for tenant: {}",
                    response.getBody().getResultsGenerated(), tenantId);
                return response.getBody().getResultsGenerated();
            }
        } catch (Exception e) {
            log.warn("Mock results endpoint also failed, trying individual evaluation: {}",
                e.getMessage());
            return evaluatePatientsIndividually(
                bundleFromPatientIds(patientIds), tenantId);
        }
        return 0;
    }

    /**
     * Create a minimal Bundle from patient ID strings (for fallback paths).
     */
    private Bundle bundleFromPatientIds(List<String> patientIds) {
        Bundle bundle = new Bundle();
        for (String id : patientIds) {
            Patient patient = new Patient();
            patient.setId(id);
            bundle.addEntry().setResource(patient);
        }
        return bundle;
    }

    /**
     * Fallback: Evaluate patients individually using local measure calculation.
     */
    private int evaluatePatientsIndividually(Bundle patientBundle, String tenantId) {
        int resultsGenerated = 0;
        List<String> patientIds = extractPatientIds(patientBundle);

        for (String patientIdStr : patientIds) {
            try {
                UUID patientId = UUID.fromString(patientIdStr);
                int evaluated = evaluatePatientLocally(patientId, tenantId);
                resultsGenerated += evaluated;
            } catch (Exception e) {
                log.debug("Failed to evaluate patient {}: {}", patientIdStr, e.getMessage());
            }
        }

        return resultsGenerated;
    }

    /**
     * Evaluate a patient using local measure calculators.
     */
    private int evaluatePatientLocally(UUID patientId, String tenantId) {
        int evaluated = 0;

        for (String measureId : HEDIS_MEASURES) {
            try {
                String url = qualityMeasureServiceUrl + "/evaluate/local" +
                    "?patientId=" + patientId + "&measureId=" + measureId;

                HttpHeaders headers = createHeaders(tenantId);
                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(headers), String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    evaluated++;
                }
            } catch (Exception e) {
                // Individual measure failures are expected, not all patients eligible for all measures
            }
        }

        return evaluated;
    }

    private int evaluateBatch(List<String> patientIds, String tenantId, int targetCompliance) {
        try {
            String url = qualityMeasureServiceUrl + "/api/v1/evaluate/batch";
            HttpHeaders headers = createHeaders(tenantId);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = new HashMap<>();
            request.put("patientIds", patientIds);
            request.put("measureIds", HEDIS_MEASURES);
            request.put("targetCompliance", targetCompliance);

            ResponseEntity<BatchResponse> response = restTemplate.exchange(
                url, HttpMethod.POST,
                new HttpEntity<>(request, headers),
                BatchResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getResultsGenerated();
            }
        } catch (Exception e) {
            log.debug("Batch evaluation failed, evaluating individually: {}", e.getMessage());
            // Fall back to individual evaluation
            int results = 0;
            for (String patientId : patientIds) {
                try {
                    results += evaluatePatientLocally(UUID.fromString(patientId), tenantId);
                } catch (Exception ex) {
                    // Skip invalid patient IDs
                }
            }
            return results;
        }

        return 0;
    }

    private List<String> extractPatientIds(Bundle bundle) {
        List<String> patientIds = new ArrayList<>();

        if (bundle != null && bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.getResource() instanceof Patient patient) {
                    String id = patient.getId();
                    // Handle "Patient/uuid" format
                    if (id != null && id.contains("/")) {
                        id = id.substring(id.lastIndexOf("/") + 1);
                    }
                    if (id != null && !id.isEmpty()) {
                        patientIds.add(id);
                    }
                }
            }
        }

        return patientIds;
    }

    private HttpHeaders createHeaders(String tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-Tenant-ID", tenantId);
        headers.set("X-Auth-User-Id", "00000000-0000-0000-0000-000000000001");
        headers.set("X-Auth-Username", "demo-seeding-service");
        headers.set("X-Auth-Tenant-Ids", tenantId);
        headers.set("X-Auth-Roles", "ADMIN,EVALUATOR,SUPER_ADMIN");
        headers.set("X-Auth-Validated", "gateway-dev-mode");
        return headers;
    }

    // Response DTOs
    public static class SeedResponse {
        private int seededCount;
        public int getSeededCount() { return seededCount; }
        public void setSeededCount(int seededCount) { this.seededCount = seededCount; }
    }

    public static class BatchResponse {
        private int resultsGenerated;
        public int getResultsGenerated() { return resultsGenerated; }
        public void setResultsGenerated(int resultsGenerated) { this.resultsGenerated = resultsGenerated; }
    }

    public static class DemoResultsRequest {
        private List<String> patientIds;
        private int careGapPercentage;
        private List<String> measureIds;

        public List<String> getPatientIds() { return patientIds; }
        public void setPatientIds(List<String> patientIds) { this.patientIds = patientIds; }
        public int getCareGapPercentage() { return careGapPercentage; }
        public void setCareGapPercentage(int careGapPercentage) { this.careGapPercentage = careGapPercentage; }
        public List<String> getMeasureIds() { return measureIds; }
        public void setMeasureIds(List<String> measureIds) { this.measureIds = measureIds; }
    }

    public static class DemoResultsResponse {
        private int resultsGenerated;
        private int compliantCount;
        private int nonCompliantCount;

        public int getResultsGenerated() { return resultsGenerated; }
        public void setResultsGenerated(int resultsGenerated) { this.resultsGenerated = resultsGenerated; }
        public int getCompliantCount() { return compliantCount; }
        public void setCompliantCount(int compliantCount) { this.compliantCount = compliantCount; }
        public int getNonCompliantCount() { return nonCompliantCount; }
        public void setNonCompliantCount(int nonCompliantCount) { this.nonCompliantCount = nonCompliantCount; }
    }
}
