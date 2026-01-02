package com.healthdata.priorauth.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.priorauth.persistence.PayerEndpointEntity;
import com.healthdata.priorauth.persistence.PayerEndpointRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Claim;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Client for interacting with Payer Prior Authorization APIs.
 *
 * Implements Da Vinci PAS specification for FHIR-based PA submission.
 * Supports multiple authentication methods (OAuth2, SMART on FHIR, API Key).
 * Includes circuit breaker and retry patterns for resilience.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PayerApiClient {

    private final PayerEndpointRepository payerEndpointRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PayerTokenService tokenService;

    private static final String FHIR_JSON_CONTENT_TYPE = "application/fhir+json";

    /**
     * Submit a prior authorization request to a payer.
     *
     * @param payerId The payer identifier
     * @param claimBundle The FHIR Bundle containing the Claim
     * @return The ClaimResponse from the payer
     */
    @CircuitBreaker(name = "payerApi", fallbackMethod = "submitPaFallback")
    @Retry(name = "payerApi")
    public PayerResponse submitPriorAuthRequest(String payerId, Bundle claimBundle) {
        log.info("Submitting PA request to payer: {}", payerId);

        PayerEndpointEntity endpoint = payerEndpointRepository.findByPayerId(payerId)
            .orElseThrow(() -> new PayerNotFoundException("Payer not found: " + payerId));

        if (!endpoint.getIsActive()) {
            throw new PayerUnavailableException("Payer endpoint is not active: " + payerId);
        }

        try {
            HttpHeaders headers = buildHeaders(endpoint);
            String requestBody = serializeBundle(claimBundle);

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            String url = endpoint.getPaFhirBaseUrl() + "/Claim/$submit";
            log.debug("Submitting PA to URL: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );

            return parseResponse(response, payerId);

        } catch (Exception e) {
            log.error("Failed to submit PA request to payer {}: {}", payerId, e.getMessage());
            throw new PayerApiException("Failed to submit PA request: " + e.getMessage(), e);
        }
    }

    /**
     * Check the status of a prior authorization request.
     *
     * @param payerId The payer identifier
     * @param paRequestId The PA request identifier
     * @return Current status from the payer
     */
    @CircuitBreaker(name = "payerApi", fallbackMethod = "checkStatusFallback")
    @Retry(name = "payerApi")
    public PayerResponse checkPaStatus(String payerId, String paRequestId) {
        log.info("Checking PA status for request {} with payer {}", paRequestId, payerId);

        PayerEndpointEntity endpoint = payerEndpointRepository.findByPayerId(payerId)
            .orElseThrow(() -> new PayerNotFoundException("Payer not found: " + payerId));

        try {
            HttpHeaders headers = buildHeaders(endpoint);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            String url = endpoint.getPaFhirBaseUrl() + "/ClaimResponse?identifier=" + paRequestId;

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                String.class
            );

            return parseResponse(response, payerId);

        } catch (Exception e) {
            log.error("Failed to check PA status: {}", e.getMessage());
            throw new PayerApiException("Failed to check PA status: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel a prior authorization request.
     *
     * @param payerId The payer identifier
     * @param paRequestId The PA request identifier
     * @return Cancellation response
     */
    @CircuitBreaker(name = "payerApi")
    public PayerResponse cancelPriorAuthRequest(String payerId, String paRequestId) {
        log.info("Cancelling PA request {} with payer {}", paRequestId, payerId);

        PayerEndpointEntity endpoint = payerEndpointRepository.findByPayerId(payerId)
            .orElseThrow(() -> new PayerNotFoundException("Payer not found: " + payerId));

        try {
            HttpHeaders headers = buildHeaders(endpoint);

            // Build cancellation claim
            Claim cancellationClaim = new Claim();
            cancellationClaim.addIdentifier()
                .setSystem("urn:healthdata:prior-auth")
                .setValue(paRequestId);
            cancellationClaim.setStatus(Claim.ClaimStatus.CANCELLED);

            String requestBody = serializeResource(cancellationClaim);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            String url = endpoint.getPaFhirBaseUrl() + "/Claim/$cancel";

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );

            return parseResponse(response, payerId);

        } catch (Exception e) {
            log.error("Failed to cancel PA request: {}", e.getMessage());
            throw new PayerApiException("Failed to cancel PA request: " + e.getMessage(), e);
        }
    }

    /**
     * Perform health check on payer endpoint.
     */
    public PayerEndpointEntity.HealthStatus healthCheck(String payerId) {
        PayerEndpointEntity endpoint = payerEndpointRepository.findByPayerId(payerId)
            .orElse(null);

        if (endpoint == null) {
            return PayerEndpointEntity.HealthStatus.UNKNOWN;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = endpoint.getPaFhirBaseUrl() + "/metadata";

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );

            PayerEndpointEntity.HealthStatus status = response.getStatusCode().is2xxSuccessful()
                ? PayerEndpointEntity.HealthStatus.HEALTHY
                : PayerEndpointEntity.HealthStatus.DEGRADED;

            // Update health status
            endpoint.setHealthStatus(status);
            endpoint.setLastHealthCheck(LocalDateTime.now());
            payerEndpointRepository.save(endpoint);

            return status;

        } catch (Exception e) {
            log.warn("Health check failed for payer {}: {}", payerId, e.getMessage());
            endpoint.setHealthStatus(PayerEndpointEntity.HealthStatus.UNHEALTHY);
            endpoint.setLastHealthCheck(LocalDateTime.now());
            payerEndpointRepository.save(endpoint);
            return PayerEndpointEntity.HealthStatus.UNHEALTHY;
        }
    }

    private HttpHeaders buildHeaders(PayerEndpointEntity endpoint) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(FHIR_JSON_CONTENT_TYPE));
        headers.set("Accept", FHIR_JSON_CONTENT_TYPE);

        // Add authentication
        switch (endpoint.getAuthType()) {
            case OAUTH2_CLIENT_CREDENTIALS, OAUTH2_AUTHORIZATION_CODE, SMART_ON_FHIR -> {
                String token = tokenService.getAccessToken(endpoint);
                headers.setBearerAuth(token);
            }
            case API_KEY -> headers.set("X-API-Key", endpoint.getClientSecret());
            case BASIC_AUTH -> {
                String auth = endpoint.getClientId() + ":" + endpoint.getClientSecret();
                headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString(auth.getBytes()));
            }
            case MUTUAL_TLS -> {
                // mTLS is handled at the RestTemplate/connection level
            }
        }

        // Add any additional headers
        if (endpoint.getAdditionalHeaders() != null) {
            endpoint.getAdditionalHeaders().forEach(headers::set);
        }

        return headers;
    }

    private String serializeBundle(Bundle bundle) {
        try {
            ca.uhn.fhir.context.FhirContext ctx = ca.uhn.fhir.context.FhirContext.forR4();
            return ctx.newJsonParser().encodeResourceToString(bundle);
        } catch (Exception e) {
            throw new PayerApiException("Failed to serialize FHIR Bundle", e);
        }
    }

    private String serializeResource(org.hl7.fhir.r4.model.Resource resource) {
        try {
            ca.uhn.fhir.context.FhirContext ctx = ca.uhn.fhir.context.FhirContext.forR4();
            return ctx.newJsonParser().encodeResourceToString(resource);
        } catch (Exception e) {
            throw new PayerApiException("Failed to serialize FHIR Resource", e);
        }
    }

    private PayerResponse parseResponse(ResponseEntity<String> response, String payerId) {
        PayerResponse payerResponse = new PayerResponse();
        payerResponse.setPayerId(payerId);
        payerResponse.setHttpStatus(response.getStatusCode().value());
        payerResponse.setSuccess(response.getStatusCode().is2xxSuccessful());

        if (response.getBody() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                payerResponse.setResponseData(responseMap);

                // Extract tracking ID if present
                if (responseMap.containsKey("identifier")) {
                    @SuppressWarnings("unchecked")
                    var identifiers = (java.util.List<Map<String, Object>>) responseMap.get("identifier");
                    if (!identifiers.isEmpty()) {
                        payerResponse.setTrackingId((String) identifiers.get(0).get("value"));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse response body: {}", e.getMessage());
                payerResponse.setRawResponse(response.getBody());
            }
        }

        return payerResponse;
    }

    // Fallback methods for circuit breaker
    @SuppressWarnings("unused")
    private PayerResponse submitPaFallback(String payerId, Bundle claimBundle, Throwable t) {
        log.warn("Fallback triggered for PA submission to payer {}: {}", payerId, t.getMessage());
        PayerResponse response = new PayerResponse();
        response.setPayerId(payerId);
        response.setSuccess(false);
        response.setErrorMessage("Payer API unavailable: " + t.getMessage());
        return response;
    }

    @SuppressWarnings("unused")
    private PayerResponse checkStatusFallback(String payerId, String paRequestId, Throwable t) {
        log.warn("Fallback triggered for status check: {}", t.getMessage());
        PayerResponse response = new PayerResponse();
        response.setPayerId(payerId);
        response.setSuccess(false);
        response.setErrorMessage("Unable to check status: " + t.getMessage());
        return response;
    }

    /**
     * Response wrapper for payer API calls.
     */
    @lombok.Data
    public static class PayerResponse {
        private String payerId;
        private boolean success;
        private int httpStatus;
        private String trackingId;
        private Map<String, Object> responseData;
        private String rawResponse;
        private String errorMessage;
    }

    // Custom exceptions
    public static class PayerNotFoundException extends RuntimeException {
        public PayerNotFoundException(String message) { super(message); }
    }

    public static class PayerUnavailableException extends RuntimeException {
        public PayerUnavailableException(String message) { super(message); }
    }

    public static class PayerApiException extends RuntimeException {
        public PayerApiException(String message) { super(message); }
        public PayerApiException(String message, Throwable cause) { super(message, cause); }
    }
}
