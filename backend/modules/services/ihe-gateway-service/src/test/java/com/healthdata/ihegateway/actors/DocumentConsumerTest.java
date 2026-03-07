package com.healthdata.ihegateway.actors;

import com.healthdata.ihegateway.config.IheGatewayProperties;
import com.healthdata.ihegateway.observability.AdapterSpanHelper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DocumentConsumerTest {

    @Mock private RestTemplate restTemplate;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private DocumentConsumer consumer;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        io.opentelemetry.api.trace.Tracer noopTracer = io.opentelemetry.api.trace.TracerProvider.noop().get("test");
        AdapterSpanHelper spanHelper = new AdapterSpanHelper(noopTracer);
        IheGatewayProperties props = new IheGatewayProperties();
        props.setHealthixDocumentUrl("http://localhost:3010");
        consumer = new DocumentConsumer(restTemplate, registry, kafkaTemplate, spanHelper, props);
    }

    @Test
    void queryDocuments_withResults_publishesToKafka() {
        Map<String, Object> fhirBundle = Map.of("entry", List.of(
                Map.of("resource", Map.of("id", "doc-1")),
                Map.of("resource", Map.of("id", "doc-2"))
        ));
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(fhirBundle);

        DocumentConsumer.DocumentQueryResult result = consumer.queryDocuments("P-001", "clinical-note");

        assertThat(result.getTotalResults()).isEqualTo(2);
        assertThat(result.getPatientId()).isEqualTo("P-001");
        verify(kafkaTemplate).send(eq("ihe.documents.received"), eq("P-001"), any());
    }

    @Test
    void queryDocuments_noResults_publishesEmptyResult() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(Map.of());

        DocumentConsumer.DocumentQueryResult result = consumer.queryDocuments("P-002", "discharge");

        assertThat(result.getTotalResults()).isEqualTo(0);
        verify(kafkaTemplate).send(eq("ihe.documents.received"), eq("P-002"), any());
    }

    @Test
    void retrieveDocument_returnsContent() {
        byte[] content = "<?xml version=\"1.0\"?><ClinicalDocument/>".getBytes();
        when(restTemplate.getForObject(eq("http://docs/doc-1"), eq(byte[].class))).thenReturn(content);

        byte[] result = consumer.retrieveDocument("http://docs/doc-1");

        assertThat(result).isEqualTo(content);
    }
}
