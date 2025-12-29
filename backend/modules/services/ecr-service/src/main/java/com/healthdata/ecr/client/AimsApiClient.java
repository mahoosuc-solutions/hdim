package com.healthdata.ecr.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * AIMS (APHL Informatics Messaging Services) API Client
 *
 * Handles communication with the AIMS platform for:
 * - Submitting eICR (electronic Initial Case Reports)
 * - Retrieving Reportability Responses (RR)
 * - Checking submission status
 *
 * AIMS is operated by APHL (Association of Public Health Laboratories)
 * and serves as the central hub for electronic case reporting to
 * state and local public health agencies.
 *
 * @see <a href="https://ecr.aimsplatform.org/">AIMS Platform</a>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AimsApiClient {

    private final RestTemplate restTemplate;
    private final FhirContext fhirContext;

    @Value("${ecr.aims.enabled:false}")
    private boolean aimsEnabled;

    @Value("${ecr.aims.base-url:https://ecr.aimsplatform.org/api}")
    private String baseUrl;

    @Value("${ecr.aims.token-url:https://ecr.aimsplatform.org/oauth/token}")
    private String tokenUrl;

    @Value("${ecr.aims.client-id:}")
    private String clientId;

    @Value("${ecr.aims.client-secret:}")
    private String clientSecret;

    @Value("${ecr.aims.submission-endpoint:/eicr/submit}")
    private String submissionEndpoint;

    @Value("${ecr.aims.status-endpoint:/eicr/status}")
    private String statusEndpoint;

    // OAuth2 token cache
    private volatile String accessToken;
    private volatile long tokenExpiresAt;
    private final Object tokenLock = new Object();

    // FHIR JSON parser for bundle serialization
    private IParser fhirJsonParser;

    @PostConstruct
    public void init() {
        this.fhirJsonParser = fhirContext.newJsonParser();
        this.fhirJsonParser.setPrettyPrint(false);
        this.fhirJsonParser.setSuppressNarratives(true);
        log.info("AimsApiClient initialized with FHIR JSON parser");
    }

    /**
     * Submit an eICR FHIR Bundle to AIMS platform.
     *
     * @param eicrBundle The eICR FHIR Bundle to submit
     * @return Submission result with tracking ID
     */
    @Retry(name = "aims-api")
    @CircuitBreaker(name = "aims-api", fallbackMethod = "submitEicrFallback")
    public SubmissionResult submitEicr(Bundle eicrBundle) {
        log.info("Submitting eICR to AIMS platform");

        if (!aimsEnabled) {
            log.info("AIMS integration disabled, simulating submission");
            return simulateSubmission(eicrBundle);
        }

        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Convert Bundle to JSON string
            String bundleJson = serializeBundle(eicrBundle);

            HttpEntity<String> request = new HttpEntity<>(bundleJson, headers);

            ResponseEntity<AimsSubmissionResponse> response = restTemplate.exchange(
                baseUrl + submissionEndpoint,
                HttpMethod.POST,
                request,
                AimsSubmissionResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                AimsSubmissionResponse body = response.getBody();
                log.info("eICR submitted successfully, tracking ID: {}", body.getTrackingId());

                return SubmissionResult.builder()
                    .success(true)
                    .trackingId(body.getTrackingId())
                    .message("Successfully submitted to AIMS")
                    .build();
            } else {
                log.error("AIMS submission failed with status: {}", response.getStatusCode());
                throw new AimsApiException("AIMS submission failed: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error submitting eICR to AIMS: {}", e.getMessage(), e);
            throw new AimsApiException("Failed to submit eICR: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when AIMS submission fails after retries.
     */
    public SubmissionResult submitEicrFallback(Bundle eicrBundle, Throwable t) {
        log.warn("AIMS submission fallback triggered: {}", t.getMessage());

        return SubmissionResult.builder()
            .success(false)
            .trackingId(null)
            .message("AIMS submission failed: " + t.getMessage())
            .build();
    }

    /**
     * Retrieve Reportability Response for a submitted eICR.
     *
     * @param trackingId AIMS tracking ID from submission
     * @return Reportability Response if available
     */
    @Retry(name = "aims-api")
    @CircuitBreaker(name = "aims-api", fallbackMethod = "getRRFallback")
    public ReportabilityResponse getReportabilityResponse(String trackingId) {
        log.debug("Checking RR for tracking ID: {}", trackingId);

        if (!aimsEnabled) {
            log.debug("AIMS integration disabled, simulating RR check");
            return simulateRRCheck(trackingId);
        }

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<AimsRRResponse> response = restTemplate.exchange(
                baseUrl + statusEndpoint + "/" + trackingId,
                HttpMethod.GET,
                request,
                AimsRRResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                AimsRRResponse body = response.getBody();

                return ReportabilityResponse.builder()
                    .trackingId(trackingId)
                    .status(body.getReportabilityStatus())
                    .jurisdiction(body.getJurisdiction())
                    .responseJson(body.getRrDetails())
                    .build();
            }

            return null;

        } catch (Exception e) {
            log.warn("Error retrieving RR from AIMS: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fallback for RR retrieval failures.
     */
    public ReportabilityResponse getRRFallback(String trackingId, Throwable t) {
        log.debug("RR retrieval fallback for {}: {}", trackingId, t.getMessage());
        return null;
    }

    /**
     * Check submission status without full RR details.
     */
    public SubmissionStatus checkSubmissionStatus(String trackingId) {
        if (!aimsEnabled) {
            return SubmissionStatus.builder()
                .trackingId(trackingId)
                .status("SIMULATED")
                .build();
        }

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + statusEndpoint + "/" + trackingId + "/status",
                HttpMethod.GET,
                request,
                Map.class
            );

            if (response.getBody() != null) {
                return SubmissionStatus.builder()
                    .trackingId(trackingId)
                    .status((String) response.getBody().get("status"))
                    .build();
            }

        } catch (Exception e) {
            log.warn("Error checking status: {}", e.getMessage());
        }

        return SubmissionStatus.builder()
            .trackingId(trackingId)
            .status("UNKNOWN")
            .build();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        // Add OAuth2 Bearer token if credentials are configured
        if (clientId != null && !clientId.isEmpty()) {
            String token = getAccessToken();
            if (token != null) {
                headers.setBearerAuth(token);
            } else {
                // Fallback to client credentials in header if token acquisition fails
                log.warn("OAuth2 token acquisition failed, falling back to header auth");
                headers.set("X-Client-ID", clientId);
                headers.set("X-Client-Secret", clientSecret);
            }
        }

        return headers;
    }

    /**
     * Get or refresh OAuth2 access token using client credentials grant.
     * Thread-safe with token caching and automatic refresh.
     */
    private String getAccessToken() {
        // Check if current token is still valid (with 60s buffer)
        if (accessToken != null && System.currentTimeMillis() < tokenExpiresAt - 60000) {
            return accessToken;
        }

        synchronized (tokenLock) {
            // Double-check after acquiring lock
            if (accessToken != null && System.currentTimeMillis() < tokenExpiresAt - 60000) {
                return accessToken;
            }

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                headers.setBasicAuth(clientId, clientSecret);

                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("grant_type", "client_credentials");
                body.add("scope", "eicr:submit eicr:status");

                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

                ResponseEntity<TokenResponse> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    TokenResponse.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    TokenResponse tokenResponse = response.getBody();
                    accessToken = tokenResponse.getAccessToken();
                    // Calculate expiry (default 1 hour if not specified)
                    long expiresIn = tokenResponse.getExpiresIn() != null
                        ? tokenResponse.getExpiresIn()
                        : 3600;
                    tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000);

                    log.info("OAuth2 token acquired, expires in {} seconds", expiresIn);
                    return accessToken;
                }
            } catch (Exception e) {
                log.error("Failed to acquire OAuth2 token: {}", e.getMessage());
            }

            return null;
        }
    }

    /**
     * Serialize FHIR Bundle to JSON string using HAPI FHIR parser.
     * Produces valid FHIR R4 JSON representation.
     */
    private String serializeBundle(Bundle bundle) {
        try {
            return fhirJsonParser.encodeResourceToString(bundle);
        } catch (Exception e) {
            log.error("Failed to serialize FHIR Bundle: {}", e.getMessage());
            throw new AimsApiException("Failed to serialize eICR bundle: " + e.getMessage(), e);
        }
    }

    // Simulation methods for development/testing

    private SubmissionResult simulateSubmission(Bundle eicrBundle) {
        String trackingId = "SIM-" + System.currentTimeMillis();
        log.info("Simulated AIMS submission, tracking ID: {}", trackingId);

        return SubmissionResult.builder()
            .success(true)
            .trackingId(trackingId)
            .message("Simulated submission (AIMS disabled)")
            .build();
    }

    private ReportabilityResponse simulateRRCheck(String trackingId) {
        // Simulate 50% chance of having RR ready
        if (System.currentTimeMillis() % 2 == 0) {
            return ReportabilityResponse.builder()
                .trackingId(trackingId)
                .status("REPORTABLE")
                .jurisdiction("MA-DPH")
                .responseJson(Map.of(
                    "determination", "REPORTABLE",
                    "jurisdiction", "Massachusetts Department of Public Health",
                    "condition", "Simulated Condition"
                ))
                .build();
        }
        return null;
    }

    // Response DTOs

    @lombok.Data
    @lombok.Builder
    public static class SubmissionResult {
        private boolean success;
        private String trackingId;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    public static class ReportabilityResponse {
        private String trackingId;
        private String status;
        private String jurisdiction;
        private Map<String, Object> responseJson;
    }

    @lombok.Data
    @lombok.Builder
    public static class SubmissionStatus {
        private String trackingId;
        private String status;
    }

    // AIMS API Response structures

    @lombok.Data
    private static class AimsSubmissionResponse {
        private String trackingId;
        private String status;
        private String message;
    }

    @lombok.Data
    private static class AimsRRResponse {
        private String trackingId;
        private String reportabilityStatus;
        private String jurisdiction;
        private Map<String, Object> rrDetails;
    }

    /**
     * OAuth2 token response DTO.
     */
    @lombok.Data
    private static class TokenResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("access_token")
        private String accessToken;

        @com.fasterxml.jackson.annotation.JsonProperty("token_type")
        private String tokenType;

        @com.fasterxml.jackson.annotation.JsonProperty("expires_in")
        private Long expiresIn;

        @com.fasterxml.jackson.annotation.JsonProperty("scope")
        private String scope;
    }

    // Custom exception
    public static class AimsApiException extends RuntimeException {
        public AimsApiException(String message) {
            super(message);
        }

        public AimsApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
