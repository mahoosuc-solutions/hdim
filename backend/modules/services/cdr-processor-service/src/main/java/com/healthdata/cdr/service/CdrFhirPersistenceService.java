package com.healthdata.cdr.service;

import ca.uhn.fhir.context.FhirContext;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Persists FHIR Bundles produced by the HL7-to-FHIR and CDA-to-FHIR converters
 * to the FHIR store via the Bundle transaction endpoint.
 *
 * <p>Follows the same WebClient pattern as FhirIngestionClient in data-ingestion-service.</p>
 */
@Service
@Slf4j
public class CdrFhirPersistenceService {

    private final WebClient webClient;
    private final FhirContext fhirContext;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CdrFhirPersistenceService(
            @Value("${cdr.fhir-service-url:http://localhost:8085/fhir}") String fhirServiceUrl,
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
     * @param sourceType Source of the data (e.g., "HL7v2", "CDA")
     */
    public void persistBundle(Bundle bundle, String tenantId, String sourceType) {
        String bundleJson = fhirContext.newJsonParser().encodeResourceToString(bundle);
        int entryCount = bundle.hasEntry() ? bundle.getEntry().size() : 0;

        try {
            String response = webClient.post()
                    .uri("/Bundle")
                    .header("X-Tenant-ID", tenantId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(bundleJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Persisted {} FHIR bundle ({} entries) for tenant: {}",
                    sourceType, entryCount, tenantId);

            // Emit event for downstream pipeline (CQL evaluation, care gap detection)
            kafkaTemplate.send("cdr." + sourceType.toLowerCase() + ".fhir-persisted",
                    UUID.randomUUID().toString(),
                    Map.of(
                            "tenantId", tenantId,
                            "sourceType", sourceType,
                            "entryCount", entryCount,
                            "timestamp", Instant.now().toString()
                    ));

        } catch (Exception e) {
            log.error("Failed to persist {} FHIR bundle for tenant: {}", sourceType, tenantId, e);
            throw new RuntimeException("FHIR persistence failed for " + sourceType, e);
        }
    }
}
