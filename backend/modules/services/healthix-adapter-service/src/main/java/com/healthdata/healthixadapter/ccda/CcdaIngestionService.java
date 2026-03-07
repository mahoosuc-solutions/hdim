package com.healthdata.healthixadapter.ccda;

import com.healthdata.common.external.ExternalEventEnvelope;
import com.healthdata.common.external.ExternalEventMetadata;
import com.healthdata.common.external.PhiLevel;
import com.healthdata.common.external.SourceSystem;
import com.healthdata.healthixadapter.observability.AdapterSpanHelper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * C-CDA (Consolidated Clinical Document Architecture) document ingestion.
 * Fetches C-CDA XML documents from Healthix document-service,
 * parses them, and publishes FHIR resources via the event bus.
 */
@Service
@ConditionalOnProperty(name = "external.healthix.enabled", havingValue = "true")
@Slf4j
public class CcdaIngestionService {

    private final RestTemplate documentRestTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CircuitBreaker circuitBreaker;
    private final AdapterSpanHelper spanHelper;

    private static final String TOPIC_DOCUMENTS = "external.healthix.documents";

    public CcdaIngestionService(
            @Qualifier("healthixDocumentRestTemplate") RestTemplate documentRestTemplate,
            KafkaTemplate<String, Object> kafkaTemplate,
            CircuitBreakerRegistry registry,
            AdapterSpanHelper spanHelper) {
        this.documentRestTemplate = documentRestTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.circuitBreaker = registry.circuitBreaker("healthix-documents");
        this.spanHelper = spanHelper;
    }

    /**
     * Fetch and process a C-CDA document from Healthix.
     */
    public void ingestDocument(String documentId, String tenantId) {
        spanHelper.tracedRun("healthix.ccda.fetch_and_ingest", () -> {
            log.info("Ingesting C-CDA document from Healthix: documentId={}", documentId);

            @SuppressWarnings("unchecked")
            Supplier<Map<String, Object>> supplier = CircuitBreaker.decorateSupplier(
                    circuitBreaker,
                    () -> documentRestTemplate.getForObject(
                            "/api/v1/documents/{id}",
                            Map.class,
                            documentId));

            Map<String, Object> document = supplier.get();

            if (document == null) {
                log.warn("Document not found in Healthix: {}", documentId);
                return;
            }

            ExternalEventEnvelope<Map<String, Object>> envelope = ExternalEventEnvelope.of(
                    "external.healthix.documents.received",
                    "healthix-adapter-service",
                    tenantId,
                    document,
                    ExternalEventMetadata.builder()
                            .sourceSystem(SourceSystem.HEALTHIX)
                            .phiLevel(PhiLevel.FULL)
                            .build());

            kafkaTemplate.send(TOPIC_DOCUMENTS, tenantId, envelope);
            log.info("Published C-CDA document event for downstream FHIR conversion");
        }, "adapter", "healthix", "phi.level", "FULL");
    }

    /**
     * Handle webhook callback from Healthix document-service
     * when a new document is available.
     */
    public void onDocumentWebhook(Map<String, Object> webhookPayload, String tenantId) {
        spanHelper.tracedRun("healthix.ccda.document_webhook", () -> {
            String documentId = (String) webhookPayload.get("documentId");
            String documentType = (String) webhookPayload.getOrDefault("documentType", "C-CDA");

            log.info("Received document webhook from Healthix: type={}, id={}",
                    documentType, documentId);

            if ("C-CDA".equals(documentType) || "CDA".equals(documentType)) {
                ingestDocument(documentId, tenantId);
            } else {
                log.info("Skipping non-C-CDA document type: {}", documentType);
            }
        }, "adapter", "healthix", "phi.level", "FULL");
    }
}
