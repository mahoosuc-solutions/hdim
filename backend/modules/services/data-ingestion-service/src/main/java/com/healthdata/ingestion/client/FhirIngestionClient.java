package com.healthdata.ingestion.client;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import ca.uhn.fhir.context.FhirContext;

/**
 * Client for persisting FHIR bundles to the FHIR service.
 */
@Component
@Slf4j
public class FhirIngestionClient {

    private final WebClient webClient;
    private final FhirContext fhirContext;

    public FhirIngestionClient(@Value("${ingestion.fhir-service-url}") String fhirServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(fhirServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/fhir+json")
                .build();
        this.fhirContext = FhirContext.forR4();
    }

    /**
     * Persist patient bundle to FHIR service.
     *
     * @param bundle Patient FHIR bundle
     * @param tenantId Tenant identifier
     */
    public void persistBundle(Bundle bundle, String tenantId) {
        String bundleJson = fhirContext.newJsonParser().encodeResourceToString(bundle);

        try {
            String response = webClient.post()
                    .uri("/Bundle")
                    .header("X-Tenant-ID", tenantId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(bundleJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("FHIR bundle persisted successfully for tenant: {}", tenantId);
        } catch (Exception e) {
            log.error("Failed to persist FHIR bundle for tenant: {}", tenantId, e);
            throw new RuntimeException("FHIR persistence failed", e);
        }
    }

    /**
     * Count patient resources in the FHIR service.
     *
     * @param tenantId Tenant identifier
     * @return Total patient count
     */
    public int countPatients(String tenantId) {
        try {
            String response = webClient.get()
                    .uri("/Patient?_summary=count")
                    .header("X-Tenant-ID", tenantId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            Bundle bundle = (Bundle) fhirContext.newJsonParser().parseResource(response);
            return bundle.getTotal();
        } catch (Exception e) {
            log.error("Failed to count patients for tenant: {}", tenantId, e);
            throw new RuntimeException("FHIR patient count failed", e);
        }
    }
}
