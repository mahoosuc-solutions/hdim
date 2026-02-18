package com.healthdata.cms.client;

import com.healthdata.cms.auth.OAuth2Manager;
import com.healthdata.cms.exception.CmsApiException;
import com.healthdata.cms.model.CmsApiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * DPC (Data at Point of Care) Client
 *
 * Handles real-time point-of-care queries for Medicare claims during patient encounters.
 *
 * Features:
 * - Real-time claim queries (<500ms latency target)
 * - Individual patient/beneficiary lookups
 * - FHIR REST API interface
 * - OAuth2 + Provider authentication
 *
 * Documentation: https://dpc.cms.gov/api/v1
 */
@Slf4j
@Component
public class DpcClient {

    private static final String API_VERSION = "/api/v1";
    private static final String FHIR_CONTENT_TYPE = "application/fhir+json";

    private final OAuth2Manager oauth2Manager;
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final boolean isSandbox;

    public DpcClient(OAuth2Manager oauth2Manager,
                     @Qualifier("cmsRestTemplate") RestTemplate restTemplate,
                     @Value("${cms.dpc.base-url:https://sandbox.dpc.cms.gov}") String baseUrl,
                     @Value("${cms.sandbox-mode:true}") boolean isSandbox) {
        this.oauth2Manager = oauth2Manager;
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.isSandbox = isSandbox;
        log.info("DPC Client initialized with base URL: {} (sandbox: {})", baseUrl, isSandbox);
    }

    /**
     * Get the authorization header with OAuth2 token
     */
    private HttpHeaders createHeaders() {
        String token = oauth2Manager.getAccessToken(CmsApiProvider.DPC);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.parseMediaType(FHIR_CONTENT_TYPE));
        headers.set("Accept", FHIR_CONTENT_TYPE);
        return headers;
    }

    /**
     * Get FHIR Patient resource by ID
     * GET /api/v1/Patient/{id}
     *
     * Returns complete Medicare patient demographics and coverage
     *
     * @param patientId The DPC patient/beneficiary ID
     * @return FHIR Patient resource as JSON string
     * @throws CmsApiException if retrieval fails
     */
    public String getPatient(String patientId) {
        log.info("Fetching Patient resource from DPC for patient: {}", patientId);

        String url = buildUrl("/Patient/" + patientId);
        return executeGetRequest(url, "Patient", patientId);
    }

    /**
     * Search for patients by organization
     * GET /api/v1/Patient?_lastUpdated=gt{date}
     *
     * Returns all patients for the authenticated organization
     *
     * @param lastUpdatedAfter Optional: Only return patients updated after this date (ISO-8601)
     * @param count Maximum number of results per page
     * @return FHIR Bundle of Patient resources as JSON string
     * @throws CmsApiException if retrieval fails
     */
    public String searchPatients(String lastUpdatedAfter, int count) {
        log.info("Searching patients from DPC (lastUpdatedAfter: {}, count: {})", lastUpdatedAfter, count);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + API_VERSION + "/Patient")
            .queryParam("_count", count);

        if (lastUpdatedAfter != null && !lastUpdatedAfter.isEmpty()) {
            builder.queryParam("_lastUpdated", "gt" + lastUpdatedAfter);
        }

        String url = builder.toUriString();
        return executeGetRequest(url, "Patient search", null);
    }

    /**
     * Get FHIR ExplanationOfBenefit (EOB) resources for a patient
     * GET /api/v1/ExplanationOfBenefit?patient={patientId}
     *
     * Returns all Medicare claims (Part A, B, D) for the patient
     *
     * @param patientId The DPC patient/beneficiary ID
     * @return FHIR Bundle of EOB resources as JSON string
     * @throws CmsApiException if retrieval fails
     */
    public String getExplanationOfBenefits(String patientId) {
        log.info("Fetching ExplanationOfBenefit resources from DPC for patient: {}", patientId);

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + API_VERSION + "/ExplanationOfBenefit")
            .queryParam("patient", patientId)
            .toUriString();

        return executeGetRequest(url, "ExplanationOfBenefit", patientId);
    }

    /**
     * Get FHIR ExplanationOfBenefit resources with date filter
     * GET /api/v1/ExplanationOfBenefit?patient={patientId}&_lastUpdated=gt{date}
     *
     * @param patientId The DPC patient/beneficiary ID
     * @param lastUpdatedAfter Only return EOBs updated after this date (ISO-8601)
     * @return FHIR Bundle of EOB resources as JSON string
     * @throws CmsApiException if retrieval fails
     */
    public String getExplanationOfBenefits(String patientId, String lastUpdatedAfter) {
        log.info("Fetching EOB resources from DPC for patient: {} (after: {})", patientId, lastUpdatedAfter);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + API_VERSION + "/ExplanationOfBenefit")
            .queryParam("patient", patientId);

        if (lastUpdatedAfter != null && !lastUpdatedAfter.isEmpty()) {
            builder.queryParam("_lastUpdated", "gt" + lastUpdatedAfter);
        }

        return executeGetRequest(builder.toUriString(), "ExplanationOfBenefit", patientId);
    }

    /**
     * Get FHIR Coverage resources for a patient
     * GET /api/v1/Coverage?beneficiary={patientId}
     *
     * Returns Medicare coverage information
     *
     * @param patientId The DPC patient/beneficiary ID
     * @return FHIR Bundle of Coverage resources as JSON string
     * @throws CmsApiException if retrieval fails
     */
    public String getCoverage(String patientId) {
        log.info("Fetching Coverage resources from DPC for patient: {}", patientId);

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + API_VERSION + "/Coverage")
            .queryParam("beneficiary", patientId)
            .toUriString();

        return executeGetRequest(url, "Coverage", patientId);
    }

    /**
     * Get FHIR Condition resources for a patient
     * GET /api/v1/Condition?patient={patientId}
     *
     * Returns all diagnoses from Medicare claims
     *
     * @param patientId The DPC patient/beneficiary ID
     * @return FHIR Bundle of Condition resources as JSON string
     * @throws CmsApiException if retrieval fails
     */
    public String getConditions(String patientId) {
        log.info("Fetching Condition resources from DPC for patient: {}", patientId);

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + API_VERSION + "/Condition")
            .queryParam("patient", patientId)
            .toUriString();

        return executeGetRequest(url, "Condition", patientId);
    }

    /**
     * Get FHIR Procedure resources for a patient
     * GET /api/v1/Procedure?patient={patientId}
     *
     * Returns all procedures from Medicare claims
     *
     * @param patientId The DPC patient/beneficiary ID
     * @return FHIR Bundle of Procedure resources as JSON string
     * @throws CmsApiException if retrieval fails
     */
    public String getProcedures(String patientId) {
        log.info("Fetching Procedure resources from DPC for patient: {}", patientId);

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + API_VERSION + "/Procedure")
            .queryParam("patient", patientId)
            .toUriString();

        return executeGetRequest(url, "Procedure", patientId);
    }

    /**
     * Get FHIR MedicationRequest resources for a patient
     * GET /api/v1/MedicationRequest?patient={patientId}
     *
     * Returns all medications from Medicare Part D claims
     *
     * @param patientId The DPC patient/beneficiary ID
     * @return FHIR Bundle of MedicationRequest resources as JSON string
     * @throws CmsApiException if retrieval fails
     */
    public String getMedicationRequests(String patientId) {
        log.info("Fetching MedicationRequest resources from DPC for patient: {}", patientId);

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + API_VERSION + "/MedicationRequest")
            .queryParam("patient", patientId)
            .toUriString();

        return executeGetRequest(url, "MedicationRequest", patientId);
    }

    /**
     * Get FHIR Observation resources for a patient
     * GET /api/v1/Observation?patient={patientId}
     *
     * Returns all lab results and observations
     *
     * @param patientId The DPC patient/beneficiary ID
     * @return FHIR Bundle of Observation resources as JSON string
     * @throws CmsApiException if retrieval fails
     */
    public String getObservations(String patientId) {
        log.info("Fetching Observation resources from DPC for patient: {}", patientId);

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + API_VERSION + "/Observation")
            .queryParam("patient", patientId)
            .toUriString();

        return executeGetRequest(url, "Observation", patientId);
    }

    /**
     * Get all clinical data for a patient using $everything operation
     * GET /api/v1/Patient/{id}/$everything
     *
     * Returns a FHIR Bundle containing all data for the patient
     *
     * @param patientId The DPC patient/beneficiary ID
     * @return FHIR Bundle with all patient data as JSON string
     * @throws CmsApiException if retrieval fails
     */
    public String getPatientEverything(String patientId) {
        log.info("Fetching all clinical data ($everything) for patient: {}", patientId);

        String url = buildUrl("/Patient/" + patientId + "/$everything");
        return executeGetRequest(url, "$everything", patientId);
    }

    /**
     * Check health of DPC API
     * GET /api/v1/metadata (or specific health endpoint)
     *
     * @return Health status object
     */
    public DpcHealthStatus getHealthStatus() {
        log.info("Checking DPC API health status");

        String url = buildUrl("/metadata");

        try {
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("DPC API is healthy");
                return new DpcHealthStatus("healthy", "1.0", baseUrl);
            } else {
                log.warn("DPC API returned non-success status: {}", response.getStatusCode());
                return new DpcHealthStatus("degraded", "unknown", baseUrl);
            }
        } catch (RestClientException e) {
            log.error("DPC API health check failed: {}", e.getMessage());
            return new DpcHealthStatus("unhealthy", "unknown", baseUrl);
        }
    }

    /**
     * Execute a GET request to DPC API
     */
    private String executeGetRequest(String url, String resourceType, String patientId) {
        try {
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());

            log.debug("Executing DPC GET request: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully retrieved {} from DPC{}", resourceType,
                    patientId != null ? " for patient: " + patientId : "");
                return response.getBody();
            }

            throw new CmsApiException("Empty response from DPC for " + resourceType);

        } catch (HttpClientErrorException e) {
            String message = String.format("DPC client error retrieving %s%s: %s - %s",
                resourceType,
                patientId != null ? " for patient " + patientId : "",
                e.getStatusCode(),
                e.getResponseBodyAsString());
            log.error(message);
            throw new CmsApiException(message, e);

        } catch (HttpServerErrorException e) {
            String message = String.format("DPC server error retrieving %s%s: %s",
                resourceType,
                patientId != null ? " for patient " + patientId : "",
                e.getStatusCode());
            log.error(message);
            throw new CmsApiException(message, e);

        } catch (RestClientException e) {
            String message = String.format("Network error retrieving %s from DPC%s: %s",
                resourceType,
                patientId != null ? " for patient " + patientId : "",
                e.getMessage());
            log.error(message);
            throw new CmsApiException(message, e);
        }
    }

    private String buildUrl(String path) {
        return baseUrl + API_VERSION + path;
    }

    // ============ DTOs ============

    public static class DpcHealthStatus {
        private final String status;
        private final String version;
        private final String endpoint;

        public DpcHealthStatus(String status, String version, String endpoint) {
            this.status = status;
            this.version = version;
            this.endpoint = endpoint;
        }

        public String getStatus() { return status; }
        public String getVersion() { return version; }
        public String getEndpoint() { return endpoint; }

        public boolean isHealthy() {
            return "healthy".equalsIgnoreCase(status);
        }
    }
}
