package com.healthdata.healthixadapter.ihe;

import com.healthdata.healthixadapter.observability.AdapterSpanHelper;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
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
class XcaRespondingGatewayTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private AdapterSpanHelper spanHelper;
    private XcaRespondingGateway gateway;

    @BeforeEach
    void setUp() {
        Tracer tracer = TracerProvider.noop().get("test");
        spanHelper = new AdapterSpanHelper(tracer);
        gateway = new XcaRespondingGateway(restTemplate, kafkaTemplate, spanHelper);
    }

    @Test
    void respondToQuery_shouldFetchFromHealthixAndReturnBundle() {
        Map<String, Object> fhirBundle = Map.of(
                "resourceType", "Bundle",
                "total", 3,
                "entry", List.of(
                        Map.of("resource", Map.of("resourceType", "DocumentReference", "id", "doc-a")),
                        Map.of("resource", Map.of("resourceType", "DocumentReference", "id", "doc-b")),
                        Map.of("resource", Map.of("resourceType", "DocumentReference", "id", "doc-c"))
                )
        );
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(fhirBundle);

        var result = gateway.respondToQuery("patient-789", "CCD");

        assertThat(result).containsEntry("total", 3);
        verify(kafkaTemplate).send(eq("ihe.xca.responding.query"), anyString(), any());
    }

    @Test
    void respondToRetrieve_shouldFetchDocumentFromHealthix() {
        byte[] docBytes = "<ClinicalDocument>content</ClinicalDocument>".getBytes();
        when(restTemplate.getForObject(anyString(), eq(byte[].class))).thenReturn(docBytes);

        byte[] result = gateway.respondToRetrieve("doc-abc-123");

        assertThat(result).isEqualTo(docBytes);
        verify(kafkaTemplate).send(eq("ihe.xca.responding.retrieve"), anyString(), any());
    }
}
