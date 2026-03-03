package com.healthdata.cms.client;

import ca.uhn.fhir.context.FhirContext;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Persists FHIR Bundles from CMS BCDA bulk exports to the FHIR store.
 *
 * <p>Follows the same WebClient pattern as FhirIngestionClient in
 * data-ingestion-service and CdrFhirPersistenceService in cdr-processor-service.</p>
 */
@Slf4j
@Component
public class CmsFhirPersistenceClient {

    private final WebClient webClient;
    private final FhirContext fhirContext;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CmsFhirPersistenceClient(
            @Value("${cms.fhir-service-url:http://localhost:8085/fhir}") String fhirServiceUrl,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.webClient = WebClient.builder()
                .baseUrl(fhirServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/fhir+json")
                .build();
        this.fhirContext = FhirContext.forR4();
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Persist a FHIR Bundle to the FHIR store.
     *
     * @param bundle FHIR Bundle to persist
     * @param tenantId Tenant identifier
     * @param resourceType Source resource type (e.g., "Patient", "ExplanationOfBenefit")
     */
    public void persistBundle(Bundle bundle, String tenantId, String resourceType) {
        String bundleJson = fhirContext.newJsonParser().encodeResourceToString(bundle);
        int entryCount = bundle.hasEntry() ? bundle.getEntry().size() : 0;

        try {
            webClient.post()
                    .uri("/Bundle")
                    .header("X-Tenant-ID", tenantId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(bundleJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Persisted CMS {} bundle ({} entries) for tenant: {}",
                    resourceType, entryCount, tenantId);

            kafkaTemplate.send("cms.bcda.fhir-persisted",
                    UUID.randomUUID().toString(),
                    Map.of(
                            "tenantId", tenantId,
                            "resourceType", resourceType,
                            "entryCount", entryCount,
                            "timestamp", Instant.now().toString()
                    ));

        } catch (Exception e) {
            log.error("Failed to persist CMS {} bundle for tenant: {}", resourceType, tenantId, e);
            throw new RuntimeException("CMS FHIR persistence failed for " + resourceType, e);
        }
    }
}
