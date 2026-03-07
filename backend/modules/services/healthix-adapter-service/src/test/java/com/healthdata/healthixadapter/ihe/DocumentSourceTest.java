package com.healthdata.healthixadapter.ihe;

import com.healthdata.healthixadapter.observability.AdapterSpanHelper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DocumentSourceTest {

    @Mock private RestTemplate restTemplate;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private DocumentSource source;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        io.opentelemetry.api.trace.Tracer noopTracer = io.opentelemetry.api.trace.TracerProvider.noop().get("test");
        AdapterSpanHelper spanHelper = new AdapterSpanHelper(noopTracer);
        source = new DocumentSource(restTemplate, registry, kafkaTemplate, spanHelper);
    }

    @Test
    void submitDocument_success_publishesResult() {
        Map<String, Object> response = Map.of("id", "doc-new-1", "resourceType", "DocumentReference");
        when(restTemplate.postForObject(eq("/fhir/DocumentReference"), any(), eq(Map.class)))
                .thenReturn(response);

        DocumentSource.DocumentSubmission submission = new DocumentSource.DocumentSubmission();
        submission.setPatientId("P-001");
        submission.setDocumentType("care-gap-report");
        submission.setDescription("HEDIS BCS care gap report");

        DocumentSource.DocumentSubmissionResult result = source.submitDocument(submission);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDocumentId()).isEqualTo("doc-new-1");
        verify(kafkaTemplate).send(eq("ihe.documents.submitted"), eq("P-001"), any());
    }

    @Test
    void submitDocument_nullResponse_marksFailure() {
        when(restTemplate.postForObject(eq("/fhir/DocumentReference"), any(), eq(Map.class)))
                .thenReturn(null);

        DocumentSource.DocumentSubmission submission = new DocumentSource.DocumentSubmission();
        submission.setPatientId("P-002");
        submission.setDocumentType("quality-summary");
        submission.setDescription("Q1 quality summary");

        DocumentSource.DocumentSubmissionResult result = source.submitDocument(submission);

        assertThat(result.isSuccess()).isFalse();
        verify(kafkaTemplate).send(eq("ihe.documents.submitted"), eq("P-002"), any());
    }
}
