package com.healthdata.ingestion.client;

import ca.uhn.fhir.context.FhirContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for interacting with FHIR Service.
 *
 * Handles:
 * - Persisting patient bundles via POST /fhir/Bundle
 * - Counting patients for validation via GET /fhir/Patient?_summary=count
 * - Deleting all patients for tenant reset via DELETE /fhir/Patient
 *
 * Uses HAPI FHIR JSON parser for serialization (not Jackson) to avoid
 * serialization conflicts with FHIR R4 model classes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FhirIngestionClient {

    private final RestTemplate restTemplate;
    private final FhirContext fhirContext = FhirContext.forR4();

    @Value("${fhir.service.url:http://fhir-service:8085/fhir}")
    private String fhirServiceUrl;

    /**
     * Persist a FHIR bundle to the FHIR service by POSTing individual resources.
     *
     * The FHIR service doesn't support transaction Bundles, so we extract
     * each resource from the bundle and POST them individually to their
     * respective resource endpoints (Patient, Condition, Observation, etc.).
     *
     * Uses HAPI FHIR's JSON parser to serialize resources (not Jackson)
     * to avoid serialization conflicts with FHIR R4 model internal structure.
     *
     * @param bundle   Patient bundle containing multiple FHIR resources
     * @param tenantId Tenant context
     */
    public void persistBundle(Bundle bundle, String tenantId) {
        if (bundle == null || bundle.getEntry().isEmpty()) {
            log.warn("Empty bundle - nothing to persist");
            return;
        }

        int successCount = 0;
        int errorCount = 0;

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() == null) {
                continue;
            }

            try {
                persistResource(entry.getResource(), tenantId);
                successCount++;
            } catch (Exception e) {
                errorCount++;
                log.error("Failed to persist {} resource: {}",
                         entry.getResource().fhirType(), e.getMessage());
                // Continue with other resources instead of failing entire bundle
            }
        }

        log.info("Persisted bundle for tenant {}: {} succeeded, {} failed",
                tenantId, successCount, errorCount);

        if (errorCount > 0 && successCount == 0) {
            throw new RuntimeException("Failed to persist any resources from bundle");
        }
    }

    /**
     * Persist a single FHIR resource to the appropriate endpoint.
     *
     * @param resource FHIR resource to persist
     * @param tenantId Tenant context
     */
    private void persistResource(org.hl7.fhir.r4.model.Resource resource, String tenantId) {
        String resourceType = resource.fhirType();
        String url = fhirServiceUrl + "/" + resourceType;

        // Use HAPI FHIR's JSON parser instead of Jackson
        String jsonBody = fhirContext.newJsonParser()
            .setPrettyPrint(false)
            .encodeResourceToString(resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/fhir+json"));
        headers.set("X-Tenant-ID", tenantId);

        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

        restTemplate.postForObject(url, request, String.class);
        log.debug("Persisted {} resource to FHIR service", resourceType);
    }

    /**
     * Count total patients for a tenant (validation).
     *
     * @param tenantId Tenant to count patients for
     * @return Patient count
     */
    public int countPatients(String tenantId) {
        String url = fhirServiceUrl + "/Patient?_summary=count";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId);

        try {
            HttpEntity<Void> request = new HttpEntity<>(headers);
            var response = restTemplate.exchange(url, HttpMethod.GET, request, Bundle.class);

            if (response.getBody() != null && response.getBody().getTotal() > 0) {
                return response.getBody().getTotal();
            }

            return 0;
        } catch (Exception e) {
            log.error("Failed to count patients in FHIR service: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Delete all patients for a tenant (reset operation).
     *
     * WARNING: Destructive operation - use only in demo environments.
     *
     * @param tenantId Tenant to delete patients for
     */
    public void deleteAllPatients(String tenantId) {
        String url = fhirServiceUrl + "/Patient";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId);

        try {
            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);

            log.info("Deleted all patients for tenant: {}", tenantId);
        } catch (Exception e) {
            log.error("Failed to delete patients from FHIR service: {}", e.getMessage(), e);
            throw new RuntimeException("FHIR deletion failed", e);
        }
    }
}
