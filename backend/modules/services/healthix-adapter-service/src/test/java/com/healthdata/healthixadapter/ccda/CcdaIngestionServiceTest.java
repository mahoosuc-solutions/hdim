package com.healthdata.healthixadapter.ccda;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CcdaIngestionService")
class CcdaIngestionServiceTest {

    @Mock
    private RestTemplate documentRestTemplate;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private CcdaIngestionService service;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        service = new CcdaIngestionService(documentRestTemplate, kafkaTemplate, registry);
    }

    @Test
    @DisplayName("should fetch document and publish to Kafka")
    void ingestDocument_shouldFetchAndPublish() {
        Map<String, Object> document = Map.of("id", "doc-123", "content", "<ClinicalDocument/>");
        when(documentRestTemplate.getForObject(anyString(), eq(Map.class), eq("doc-123")))
                .thenReturn(document);

        service.ingestDocument("doc-123", "tenant-1");

        verify(kafkaTemplate).send(eq("external.healthix.documents"), eq("tenant-1"), any());
    }

    @Test
    @DisplayName("should skip when document not found")
    void ingestDocument_shouldSkipWhenDocumentNull() {
        when(documentRestTemplate.getForObject(anyString(), eq(Map.class), eq("doc-404")))
                .thenReturn(null);

        service.ingestDocument("doc-404", "tenant-1");

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("should route C-CDA webhook to ingestion")
    void onDocumentWebhook_shouldIngestCcdaDocuments() {
        Map<String, Object> webhook = new HashMap<>();
        webhook.put("documentId", "doc-789");
        webhook.put("documentType", "C-CDA");

        Map<String, Object> document = Map.of("id", "doc-789", "content", "<ClinicalDocument/>");
        when(documentRestTemplate.getForObject(anyString(), eq(Map.class), eq("doc-789")))
                .thenReturn(document);

        service.onDocumentWebhook(webhook, "tenant-1");

        verify(kafkaTemplate).send(eq("external.healthix.documents"), eq("tenant-1"), any());
    }

    @Test
    @DisplayName("should also accept CDA document type")
    void onDocumentWebhook_shouldAcceptCdaType() {
        Map<String, Object> webhook = new HashMap<>();
        webhook.put("documentId", "doc-cda");
        webhook.put("documentType", "CDA");

        Map<String, Object> document = Map.of("id", "doc-cda");
        when(documentRestTemplate.getForObject(anyString(), eq(Map.class), eq("doc-cda")))
                .thenReturn(document);

        service.onDocumentWebhook(webhook, "tenant-1");

        verify(kafkaTemplate).send(eq("external.healthix.documents"), eq("tenant-1"), any());
    }

    @Test
    @DisplayName("should skip non-C-CDA document types")
    void onDocumentWebhook_shouldSkipNonCcdaDocuments() {
        Map<String, Object> webhook = new HashMap<>();
        webhook.put("documentId", "doc-pdf");
        webhook.put("documentType", "PDF");

        service.onDocumentWebhook(webhook, "tenant-1");

        verifyNoInteractions(kafkaTemplate);
    }
}
