package com.healthdata.clinicalworkflow.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * FHIR Service Client
 *
 * Provides access to FHIR R4 resource persistence via the FHIR Service.
 * Implements circuit breaker pattern for resilience.
 *
 * HIPAA Compliance:
 * - All requests enforce multi-tenant isolation via X-Tenant-ID header
 * - FHIR resources containing PHI must be transmitted securely
 * - Audit trail maintained by FHIR Service
 */
@Component
@Slf4j
public class FhirServiceClient {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser();

    private final RestTemplate restTemplate;
    private final String fhirServiceUrl;

    public FhirServiceClient(
            RestTemplate restTemplate,
            @Value("${fhir.service.url:http://localhost:8085}") String fhirServiceUrl) {
        this.restTemplate = restTemplate;
        this.fhirServiceUrl = fhirServiceUrl;
    }

    /**
     * Create FHIR Observation resource
     *
     * Persists an Observation resource to the FHIR service.
     * Used for vital signs, lab results, and other clinical observations.
     *
     * Circuit Breaker:
     * - Fallback: Returns null if FHIR service unavailable
     * - Calling code should handle null gracefully
     *
     * @param observation the FHIR R4 Observation resource
     * @param tenantId the tenant ID (HIPAA §164.312(d))
     * @param userId the user ID performing the action
     * @return created Observation with server-assigned ID, or null if failed
     */
    @CircuitBreaker(name = "fhir-service", fallbackMethod = "createObservationFallback")
    public Observation createObservation(Observation observation, String tenantId, String userId) {
        try {
            log.debug("Creating FHIR Observation for patient {} in tenant {}",
                    observation.getSubject().getReference(), tenantId);

            String url = String.format("%s/Observation", fhirServiceUrl);
            String observationJson = JSON_PARSER.encodeResourceToString(observation);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("application/fhir+json"));
            headers.set("X-Tenant-ID", tenantId);
            headers.set("X-User-ID", userId);

            HttpEntity<String> request = new HttpEntity<>(observationJson, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Observation created = (Observation) JSON_PARSER.parseResource(response.getBody());
                log.info("Successfully created FHIR Observation {} for patient {} in tenant {}",
                        created.getIdElement().getIdPart(),
                        observation.getSubject().getReference(),
                        tenantId);
                return created;
            }

            log.warn("FHIR service returned non-success status: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            log.error("Error creating FHIR Observation for patient {}: {}",
                    observation.getSubject().getReference(), e.getMessage());
            throw e; // Circuit breaker will catch and invoke fallback
        }
    }

    /**
     * Fallback method when FHIR service is unavailable
     *
     * Returns null to indicate Observation could not be created.
     * Calling code should handle null gracefully (e.g., log warning).
     *
     * @param observation the FHIR Observation resource
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param throwable the exception that triggered fallback
     * @return null (observation not created)
     */
    private Observation createObservationFallback(
            Observation observation, String tenantId, String userId, Throwable throwable) {
        log.warn("FHIR service unavailable for creating Observation for patient {} in tenant {}: {}",
                observation.getSubject().getReference(), tenantId, throwable.getMessage());
        return null;
    }
}
