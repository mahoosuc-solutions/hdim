package com.healthdata.priorauth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.priorauth.client.PayerApiClient;
import com.healthdata.priorauth.persistence.PayerEndpointEntity;
import com.healthdata.priorauth.persistence.PayerEndpointRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.*;

/**
 * Service for Provider Access API functionality.
 *
 * Implements CMS Provider Access API requirements, allowing providers to retrieve
 * patient data (claims, coverage, clinical data) from payers via FHIR APIs.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProviderAccessService {

    private final PayerEndpointRepository payerEndpointRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final com.healthdata.priorauth.client.PayerTokenService tokenService;

    private static final String FHIR_JSON_CONTENT_TYPE = "application/fhir+json";

    /**
     * Get patient claims from payer.
     */
    @CircuitBreaker(name = "providerAccess", fallbackMethod = "getClaimsFallback")
    public Page<Map<String, Object>> getPatientClaims(String tenantId, UUID patientId,
                                                       String payerId, LocalDate startDate,
                                                       LocalDate endDate, String claimType,
                                                       Pageable pageable) {
        log.info("Fetching claims for patient {} from payer {}", patientId, payerId);

        PayerEndpointEntity endpoint = getPayerEndpoint(payerId);

        UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl(endpoint.getProviderAccessEndpointUrl())
            .pathSegment("Claim")
            .queryParam("patient", "Patient/" + patientId);

        if (startDate != null) {
            builder.queryParam("created", "ge" + startDate);
        }
        if (endDate != null) {
            builder.queryParam("created", "le" + endDate);
        }
        if (claimType != null) {
            builder.queryParam("use", claimType);
        }
        builder.queryParam("_count", pageable.getPageSize());
        builder.queryParam("_offset", pageable.getOffset());

        String url = builder.toUriString();

        try {
            HttpHeaders headers = buildHeaders(endpoint);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            return parseBundleResponse(response.getBody(), pageable);
        } catch (Exception e) {
            log.error("Failed to fetch claims: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch claims from payer", e);
        }
    }

    /**
     * Get claim details by ID.
     */
    @CircuitBreaker(name = "providerAccess")
    public Map<String, Object> getClaimDetails(String tenantId, String claimId, String payerId) {
        PayerEndpointEntity endpoint = getPayerEndpoint(payerId);

        String url = endpoint.getProviderAccessEndpointUrl() + "/Claim/" + claimId;

        try {
            HttpHeaders headers = buildHeaders(endpoint);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            return parseResourceResponse(response.getBody());
        } catch (Exception e) {
            log.error("Failed to fetch claim details: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch claim details from payer", e);
        }
    }

    /**
     * Get patient coverage information.
     */
    @CircuitBreaker(name = "providerAccess", fallbackMethod = "getCoverageFallback")
    public List<Map<String, Object>> getPatientCoverage(String tenantId, UUID patientId,
                                                         String payerId) {
        log.info("Fetching coverage for patient {} from payer {}", patientId, payerId);

        PayerEndpointEntity endpoint = getPayerEndpoint(payerId);

        String url = UriComponentsBuilder
            .fromHttpUrl(endpoint.getProviderAccessEndpointUrl())
            .pathSegment("Coverage")
            .queryParam("beneficiary", "Patient/" + patientId)
            .queryParam("status", "active")
            .toUriString();

        try {
            HttpHeaders headers = buildHeaders(endpoint);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            return parseBundleToList(response.getBody());
        } catch (Exception e) {
            log.error("Failed to fetch coverage: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch coverage from payer", e);
        }
    }

    /**
     * Get patient clinical data from payer.
     */
    @CircuitBreaker(name = "providerAccess")
    public Map<String, Object> getPatientClinicalData(String tenantId, UUID patientId,
                                                       String payerId, List<String> resourceTypes) {
        log.info("Fetching clinical data for patient {} from payer {}", patientId, payerId);

        PayerEndpointEntity endpoint = getPayerEndpoint(payerId);

        Map<String, Object> clinicalData = new HashMap<>();

        // Default resource types if not specified
        if (resourceTypes == null || resourceTypes.isEmpty()) {
            resourceTypes = List.of("Condition", "MedicationRequest", "Observation", "Procedure");
        }

        for (String resourceType : resourceTypes) {
            try {
                String url = UriComponentsBuilder
                    .fromHttpUrl(endpoint.getProviderAccessEndpointUrl())
                    .pathSegment(resourceType)
                    .queryParam("patient", "Patient/" + patientId)
                    .queryParam("_count", 100)
                    .toUriString();

                HttpHeaders headers = buildHeaders(endpoint);
                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

                List<Map<String, Object>> resources = parseBundleToList(response.getBody());
                clinicalData.put(resourceType.toLowerCase() + "s", resources);

            } catch (Exception e) {
                log.warn("Failed to fetch {}: {}", resourceType, e.getMessage());
                clinicalData.put(resourceType.toLowerCase() + "s", List.of());
            }
        }

        return clinicalData;
    }

    /**
     * Get prior authorization history from payer.
     */
    @CircuitBreaker(name = "providerAccess")
    public List<Map<String, Object>> getPriorAuthHistory(String tenantId, UUID patientId,
                                                          String payerId, LocalDate startDate,
                                                          LocalDate endDate) {
        log.info("Fetching PA history for patient {} from payer {}", patientId, payerId);

        PayerEndpointEntity endpoint = getPayerEndpoint(payerId);

        UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl(endpoint.getProviderAccessEndpointUrl())
            .pathSegment("Claim")
            .queryParam("patient", "Patient/" + patientId)
            .queryParam("use", "preauthorization");

        if (startDate != null) {
            builder.queryParam("created", "ge" + startDate);
        }
        if (endDate != null) {
            builder.queryParam("created", "le" + endDate);
        }

        try {
            HttpHeaders headers = buildHeaders(endpoint);
            ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(), HttpMethod.GET, new HttpEntity<>(headers), String.class);

            return parseBundleToList(response.getBody());
        } catch (Exception e) {
            log.error("Failed to fetch PA history: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch PA history from payer", e);
        }
    }

    /**
     * Get Explanation of Benefits.
     */
    @CircuitBreaker(name = "providerAccess")
    public Page<Map<String, Object>> getExplanationOfBenefits(String tenantId, UUID patientId,
                                                               String payerId, LocalDate startDate,
                                                               LocalDate endDate, Pageable pageable) {
        log.info("Fetching EOB for patient {} from payer {}", patientId, payerId);

        PayerEndpointEntity endpoint = getPayerEndpoint(payerId);

        UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl(endpoint.getProviderAccessEndpointUrl())
            .pathSegment("ExplanationOfBenefit")
            .queryParam("patient", "Patient/" + patientId)
            .queryParam("_count", pageable.getPageSize())
            .queryParam("_offset", pageable.getOffset());

        if (startDate != null) {
            builder.queryParam("created", "ge" + startDate);
        }
        if (endDate != null) {
            builder.queryParam("created", "le" + endDate);
        }

        try {
            HttpHeaders headers = buildHeaders(endpoint);
            ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(), HttpMethod.GET, new HttpEntity<>(headers), String.class);

            return parseBundleResponse(response.getBody(), pageable);
        } catch (Exception e) {
            log.error("Failed to fetch EOB: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch EOB from payer", e);
        }
    }

    /**
     * Check service coverage eligibility.
     */
    @CircuitBreaker(name = "providerAccess")
    public Map<String, Object> checkEligibility(String tenantId, UUID patientId,
                                                 String payerId, String serviceCode,
                                                 LocalDate serviceDate) {
        log.info("Checking eligibility for patient {} with payer {}", patientId, payerId);

        PayerEndpointEntity endpoint = getPayerEndpoint(payerId);

        // Build CoverageEligibilityRequest
        Map<String, Object> eligibilityRequest = new HashMap<>();
        eligibilityRequest.put("resourceType", "CoverageEligibilityRequest");
        eligibilityRequest.put("status", "active");
        eligibilityRequest.put("purpose", List.of("benefits", "validation"));
        eligibilityRequest.put("patient", Map.of("reference", "Patient/" + patientId));
        eligibilityRequest.put("created", LocalDate.now().toString());
        eligibilityRequest.put("insurer", Map.of("identifier", Map.of("value", payerId)));

        if (serviceCode != null) {
            eligibilityRequest.put("item", List.of(Map.of(
                "productOrService", Map.of("coding", List.of(Map.of("code", serviceCode)))
            )));
        }

        try {
            HttpHeaders headers = buildHeaders(endpoint);
            headers.setContentType(MediaType.parseMediaType(FHIR_JSON_CONTENT_TYPE));

            String requestBody = objectMapper.writeValueAsString(eligibilityRequest);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            String url = endpoint.getProviderAccessEndpointUrl() + "/CoverageEligibilityRequest/$submit";

            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, request, String.class);

            return parseResourceResponse(response.getBody());
        } catch (Exception e) {
            log.error("Failed to check eligibility: {}", e.getMessage());
            throw new RuntimeException("Failed to check eligibility with payer", e);
        }
    }

    // Helper methods

    private PayerEndpointEntity getPayerEndpoint(String payerId) {
        return payerEndpointRepository.findByPayerId(payerId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown payer: " + payerId));
    }

    private HttpHeaders buildHeaders(PayerEndpointEntity endpoint) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", FHIR_JSON_CONTENT_TYPE);

        // Add authentication token
        String token = tokenService.getAccessToken(endpoint);
        headers.setBearerAuth(token);

        if (endpoint.getAdditionalHeaders() != null) {
            endpoint.getAdditionalHeaders().forEach(headers::set);
        }

        return headers;
    }

    @SuppressWarnings("unchecked")
    private Page<Map<String, Object>> parseBundleResponse(String body, Pageable pageable) {
        try {
            Map<String, Object> bundle = objectMapper.readValue(body, Map.class);
            List<Map<String, Object>> entries = new ArrayList<>();

            if (bundle.containsKey("entry")) {
                List<Map<String, Object>> entryList = (List<Map<String, Object>>) bundle.get("entry");
                for (Map<String, Object> entry : entryList) {
                    if (entry.containsKey("resource")) {
                        entries.add((Map<String, Object>) entry.get("resource"));
                    }
                }
            }

            int total = bundle.containsKey("total") ? (Integer) bundle.get("total") : entries.size();

            return new PageImpl<>(entries, pageable, total);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse bundle response", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseBundleToList(String body) {
        try {
            Map<String, Object> bundle = objectMapper.readValue(body, Map.class);
            List<Map<String, Object>> entries = new ArrayList<>();

            if (bundle.containsKey("entry")) {
                List<Map<String, Object>> entryList = (List<Map<String, Object>>) bundle.get("entry");
                for (Map<String, Object> entry : entryList) {
                    if (entry.containsKey("resource")) {
                        entries.add((Map<String, Object>) entry.get("resource"));
                    }
                }
            }

            return entries;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse bundle response", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResourceResponse(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse resource response", e);
        }
    }

    // Fallback methods
    @SuppressWarnings("unused")
    private Page<Map<String, Object>> getClaimsFallback(String tenantId, UUID patientId,
                                                         String payerId, LocalDate startDate,
                                                         LocalDate endDate, String claimType,
                                                         Pageable pageable, Throwable t) {
        log.warn("Claims fallback triggered: {}", t.getMessage());
        return Page.empty(pageable);
    }

    @SuppressWarnings("unused")
    private List<Map<String, Object>> getCoverageFallback(String tenantId, UUID patientId,
                                                           String payerId, Throwable t) {
        log.warn("Coverage fallback triggered: {}", t.getMessage());
        return List.of();
    }
}
