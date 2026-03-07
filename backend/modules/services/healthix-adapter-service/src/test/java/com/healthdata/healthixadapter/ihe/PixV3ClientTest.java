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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class PixV3ClientTest {

    @Mock private RestTemplate restTemplate;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private PixV3Client client;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        io.opentelemetry.api.trace.Tracer noopTracer = io.opentelemetry.api.trace.TracerProvider.noop().get("test");
        AdapterSpanHelper spanHelper = new AdapterSpanHelper(noopTracer);
        client = new PixV3Client(restTemplate, registry, kafkaTemplate, spanHelper);
    }

    @Test
    void queryCrossReferences_withMatches_publishesToKafka() {
        PixV3Client.PixCrossReferenceResult result = new PixV3Client.PixCrossReferenceResult();
        result.setSourcePatientId("P-001");
        result.setSourceAuthority("HDIM");
        PixV3Client.CrossReferenceIdentifier id = new PixV3Client.CrossReferenceIdentifier();
        id.setPatientId("MRN-12345");
        id.setAssigningAuthority("OHSU");
        id.setIdentifierType("MRN");
        result.setIdentifiers(List.of(id));

        when(restTemplate.postForObject(eq("/api/v1/mpi/pix-query"), any(), eq(PixV3Client.PixCrossReferenceResult.class)))
                .thenReturn(result);

        PixV3Client.PixCrossReferenceResult actual = client.queryCrossReferences("P-001", "HDIM");

        assertThat(actual.getIdentifiers()).hasSize(1);
        assertThat(actual.getIdentifiers().get(0).getAssigningAuthority()).isEqualTo("OHSU");
        verify(kafkaTemplate).send(eq("ihe.patient.crossref"), eq("P-001"), any());
    }

    @Test
    void queryCrossReferences_noMatches_doesNotPublishToKafka() {
        PixV3Client.PixCrossReferenceResult result = new PixV3Client.PixCrossReferenceResult();
        result.setSourcePatientId("P-999");
        result.setIdentifiers(List.of());

        when(restTemplate.postForObject(eq("/api/v1/mpi/pix-query"), any(), eq(PixV3Client.PixCrossReferenceResult.class)))
                .thenReturn(result);

        PixV3Client.PixCrossReferenceResult actual = client.queryCrossReferences("P-999", "HDIM");

        assertThat(actual.getIdentifiers()).isEmpty();
        verifyNoInteractions(kafkaTemplate);
    }
}
