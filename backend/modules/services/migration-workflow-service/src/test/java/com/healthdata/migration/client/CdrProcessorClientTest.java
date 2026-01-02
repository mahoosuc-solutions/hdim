package com.healthdata.migration.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.healthdata.cdr.dto.CdaProcessResponse;
import com.healthdata.cdr.dto.ProcessMessageResponse;
import com.healthdata.migration.dto.DataType;

import reactor.core.publisher.Mono;

@DisplayName("CdrProcessorClient")
class CdrProcessorClientTest {

    @Test
    @DisplayName("Should process HL7 message via WebClient")
    void shouldProcessHl7Message() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        ProcessMessageResponse response = ProcessMessageResponse.builder().success(true).build();

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(eq("/api/v1/cdr/hl7/v2"))).thenReturn(bodyUriSpec);
        when(bodyUriSpec.contentType(eq(MediaType.APPLICATION_JSON))).thenReturn(bodyUriSpec);
        doReturn(headersSpec).when(bodyUriSpec).bodyValue(any());
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(ProcessMessageResponse.class))).thenReturn(Mono.just(response));

        CdrProcessorClient client = new CdrProcessorClient(webClient);
        ReflectionTestUtils.setField(client, "timeout", Duration.ofSeconds(1));

        ProcessMessageResponse result = client.processHl7Message("MSH|^~\\&|", "tenant", true);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Should process CDA document via WebClient")
    void shouldProcessCdaDocument() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        CdaProcessResponse response = CdaProcessResponse.builder().success(true).build();

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(eq("/api/v1/cdr/cda"))).thenReturn(bodyUriSpec);
        when(bodyUriSpec.contentType(eq(MediaType.APPLICATION_JSON))).thenReturn(bodyUriSpec);
        doReturn(headersSpec).when(bodyUriSpec).bodyValue(any());
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(CdaProcessResponse.class))).thenReturn(Mono.just(response));

        CdrProcessorClient client = new CdrProcessorClient(webClient);
        ReflectionTestUtils.setField(client, "timeout", Duration.ofSeconds(1));

        CdaProcessResponse result = client.processCdaDocument("<ClinicalDocument/>", "tenant", true);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Should handle processRecord variants")
    void shouldHandleProcessRecordVariants() {
        WebClient webClient = mock(WebClient.class);
        CdrProcessorClient client = new CdrProcessorClient(webClient);
        ReflectionTestUtils.setField(client, "timeout", Duration.ofSeconds(1));

        CdrProcessingResult fhirResult = client.processRecord("{}", DataType.FHIR_BUNDLE, "tenant");
        assertThat(fhirResult.isSuccess()).isTrue();
        assertThat(fhirResult.getResourceCounts().get("Bundle")).isEqualTo(1);

        CdrProcessorClient spyClient = Mockito.spy(client);
        doThrow(new RuntimeException("boom"))
                .when(spyClient)
                .processHl7Message(anyString(), anyString(), anyBoolean());

        CdrProcessingResult errorResult = spyClient.processRecord("msg", DataType.HL7V2, "tenant");
        assertThat(errorResult.isSuccess()).isFalse();

        CdrProcessingResult nullTypeResult = client.processRecord("msg", null, "tenant");
        assertThat(nullTypeResult.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("Should report health status")
    void shouldReportHealthStatus() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec<?> headersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        doReturn(headersUriSpec).when(webClient).get();
        doReturn(headersSpec).when(headersUriSpec).uri(eq("/api/v1/cdr/health"));
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(Map.of("status", "UP")));

        CdrProcessorClient client = new CdrProcessorClient(webClient);
        ReflectionTestUtils.setField(client, "timeout", Duration.ofSeconds(1));

        assertThat(client.isHealthy()).isTrue();
    }

    @Test
    @DisplayName("Should handle health check failures")
    void shouldHandleHealthCheckFailures() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec<?> headersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        doReturn(headersUriSpec).when(webClient).get();
        doReturn(headersSpec).when(headersUriSpec).uri(eq("/api/v1/cdr/health"));
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.error(new RuntimeException("fail")));

        CdrProcessorClient client = new CdrProcessorClient(webClient);
        ReflectionTestUtils.setField(client, "timeout", Duration.ofSeconds(1));

        assertThat(client.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("Should return false when health status is not UP")
    void shouldReturnFalseWhenHealthNotUp() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec<?> headersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        doReturn(headersUriSpec).when(webClient).get();
        doReturn(headersSpec).when(headersUriSpec).uri(eq("/api/v1/cdr/health"));
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(Map.of("status", "DOWN")));

        CdrProcessorClient client = new CdrProcessorClient(webClient);
        ReflectionTestUtils.setField(client, "timeout", Duration.ofSeconds(1));

        assertThat(client.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("Should process record via HL7 and CDA paths")
    void shouldProcessRecordWithHl7AndCda() {
        WebClient webClient = mock(WebClient.class);
        CdrProcessorClient client = Mockito.spy(new CdrProcessorClient(webClient));

        ProcessMessageResponse hl7Response = new ProcessMessageResponse();
        hl7Response.setSuccess(true);
        CdaProcessResponse cdaResponse = new CdaProcessResponse();
        cdaResponse.setSuccess(true);

        Mockito.doReturn(hl7Response).when(client).processHl7Message(any(), any(), anyBoolean());
        Mockito.doReturn(cdaResponse).when(client).processCdaDocument(any(), any(), anyBoolean());

        CdrProcessingResult hl7Result = client.processRecord("MSH|^~\\&|", DataType.HL7V2, "tenant");
        CdrProcessingResult cdaResult = client.processRecord("<ClinicalDocument/>", DataType.CDA, "tenant");

        assertThat(hl7Result.isSuccess()).isTrue();
        assertThat(cdaResult.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Should return failure when CDA processing throws")
    void shouldReturnFailureWhenCdaThrows() {
        WebClient webClient = mock(WebClient.class);
        CdrProcessorClient client = Mockito.spy(new CdrProcessorClient(webClient));

        Mockito.doThrow(new RuntimeException("CDA error"))
                .when(client)
                .processCdaDocument(any(), any(), anyBoolean());

        CdrProcessingResult result = client.processRecord("<ClinicalDocument/>", DataType.CDA, "tenant");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("CDA error");
    }

    @Test
    @DisplayName("Should return false when health response is null")
    void shouldReturnFalseWhenHealthResponseNull() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec<?> headersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        doReturn(headersUriSpec).when(webClient).get();
        doReturn(headersSpec).when(headersUriSpec).uri(eq("/api/v1/cdr/health"));
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.justOrEmpty(null));

        CdrProcessorClient client = new CdrProcessorClient(webClient);
        ReflectionTestUtils.setField(client, "timeout", Duration.ofSeconds(1));

        assertThat(client.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("Should return fallback responses for circuit breaker")
    void shouldReturnFallbackResponses() {
        WebClient webClient = mock(WebClient.class);
        CdrProcessorClient client = new CdrProcessorClient(webClient);

        ProcessMessageResponse hl7Fallback = ReflectionTestUtils.invokeMethod(
                client,
                "processHl7MessageFallback",
                "msg",
                "tenant",
                true,
                new RuntimeException("down")
        );

        CdaProcessResponse cdaFallback = ReflectionTestUtils.invokeMethod(
                client,
                "processCdaDocumentFallback",
                "<ClinicalDocument/>",
                "tenant",
                true,
                new RuntimeException("down")
        );

        assertThat(hl7Fallback).isNotNull();
        assertThat(hl7Fallback.isSuccess()).isFalse();
        assertThat(cdaFallback).isNotNull();
        assertThat(cdaFallback.isSuccess()).isFalse();
    }
}
