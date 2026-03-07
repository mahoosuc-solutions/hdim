package com.healthdata.ihegateway.actors;

import com.healthdata.ihegateway.observability.AdapterSpanHelper;
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
class XcaInitiatingGatewayTest {

    @Mock private RestTemplate restTemplate;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private XcaInitiatingGateway gateway;

    @BeforeEach
    void setUp() {
        AdapterSpanHelper spanHelper = new AdapterSpanHelper(
                TracerProvider.noop().get("test"));
        gateway = new XcaInitiatingGateway(
                restTemplate, kafkaTemplate, spanHelper, "http://remote-community:8080");
    }

    @Test
    void crossGatewayQuery_shouldReturnDocumentReferences() {
        Map<String, Object> fhirBundle = Map.of("entry", List.of(
                Map.of("resource", Map.of("id", "doc-1")),
                Map.of("resource", Map.of("id", "doc-2"))
        ));
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(fhirBundle);

        XcaInitiatingGateway.XcaQueryResult result = gateway.crossGatewayQuery("P-001", "CCD");

        assertThat(result.getTotalResults()).isEqualTo(2);
        assertThat(result.getPatientId()).isEqualTo("P-001");
        assertThat(result.getDocumentType()).isEqualTo("CCD");
        assertThat(result.getSourceCommunity()).isEqualTo("http://remote-community:8080");
        verify(kafkaTemplate).send(eq("ihe.xca.query.results"), eq("P-001"), any());
    }

    @Test
    void crossGatewayQuery_withNoResults_shouldReturnEmptyResult() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(Map.of());

        XcaInitiatingGateway.XcaQueryResult result = gateway.crossGatewayQuery("P-002", "CCD");

        assertThat(result.getTotalResults()).isEqualTo(0);
        verify(kafkaTemplate, never()).send(eq("ihe.xca.query.results"), anyString(), any());
    }

    @Test
    void crossGatewayRetrieve_shouldReturnDocumentBytes() {
        byte[] content = "<?xml version=\"1.0\"?><ClinicalDocument/>".getBytes();
        when(restTemplate.getForObject(eq("http://remote-community:8080/docs/doc-1"), eq(byte[].class)))
                .thenReturn(content);

        byte[] result = gateway.crossGatewayRetrieve("http://remote-community:8080/docs/doc-1");

        assertThat(result).isEqualTo(content);
        verify(kafkaTemplate).send(eq("ihe.xca.retrieve.results"), any());
    }
}
