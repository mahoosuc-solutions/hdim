package com.healthdata.ingestion.client;

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
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FhirIngestionClient {

    private final RestTemplate restTemplate;

    @Value("${fhir.service.url:http://fhir-service:8085/fhir}")
    private String fhirServiceUrl;

    /**
     * Persist a FHIR bundle to the FHIR service.
     *
     * @param bundle   Patient bundle to persist
     * @param tenantId Tenant context
     */
    public void persistBundle(Bundle bundle, String tenantId) {
        String url = fhirServiceUrl + "/Bundle";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/fhir+json"));
        headers.set("X-Tenant-ID", tenantId);

        HttpEntity<Bundle> request = new HttpEntity<>(bundle, headers);

        try {
            restTemplate.postForObject(url, request, Bundle.class);
            log.debug("Persisted bundle to FHIR service for tenant: {}", tenantId);
        } catch (Exception e) {
            log.error("Failed to persist bundle to FHIR service: {}", e.getMessage(), e);
            throw new RuntimeException("FHIR persistence failed", e);
        }
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
