package com.healthdata.ehr.service;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists FHIR Bundles from EHR sync operations to the FHIR store.
 *
 * <p>After EhrSyncService pulls data from Epic/Cerner/Athena, this service
 * forwards the resulting FHIR resources to the FHIR service's Bundle
 * transaction endpoint.</p>
 *
 * <p>Follows the same WebClient pattern as FhirIngestionClient in
 * data-ingestion-service.</p>
 */
@Service
@Slf4j
public class EhrFhirPersistenceService {

    private final WebClient webClient;
    private final FhirContext fhirContext;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EhrFhirPersistenceService(
            @Value("${ehr.fhir-service-url:http://localhost:8085/fhir}") String fhirServiceUrl,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.webClient = WebClient.builder()
                .baseUrl(fhirServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/fhir+json")
                .build();
        this.fhirContext = FhirContext.forR4();
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Persist a FHIR Bundle to the FHIR store and emit a Kafka event
     * for downstream processing (CQL evaluation, care gap detection).
     *
     * @param bundle FHIR transaction Bundle
     * @param tenantId Tenant identifier
     * @param patientIds Patient IDs affected by this sync
     */
    public void persistBundle(Bundle bundle, String tenantId, List<String> patientIds) {
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

            log.info("EHR sync persisted {} resources for tenant: {}", entryCount, tenantId);

            kafkaTemplate.send("ehr.sync.fhir-persisted",
                    UUID.randomUUID().toString(),
                    Map.of(
                            "tenantId", tenantId,
                            "entryCount", entryCount,
                            "patientIds", patientIds != null ? patientIds : List.of(),
                            "timestamp", Instant.now().toString()
                    ));

        } catch (Exception e) {
            log.error("Failed to persist EHR sync bundle for tenant: {}", tenantId, e);
            throw new RuntimeException("EHR FHIR persistence failed", e);
        }
    }
}
