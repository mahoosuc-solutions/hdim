package com.healthdata.devops.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FhirServiceClient.
 * Tests WebClient-based FHIR service communication with mocked WebClient chain.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FHIR Service Client Tests")
@Tag("unit")
class FhirServiceClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private FhirServiceClient fhirServiceClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        fhirServiceClient = new FhirServiceClient("http://localhost:8085/fhir");
        // Replace the internally-built WebClient with our mock
        Field webClientField = FhirServiceClient.class.getDeclaredField("webClient");
        webClientField.setAccessible(true);
        webClientField.set(fhirServiceClient, webClient);
        objectMapper = new ObjectMapper();
    }

    @SuppressWarnings("unchecked")
    private void setupGetChain(JsonNode responseBody) {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.justOrEmpty(responseBody));
    }

    @SuppressWarnings("unchecked")
    private void setupGetChainWithError(Exception exception) {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.error(exception));
    }

    @Nested
    @DisplayName("getResourceCount Tests")
    class GetResourceCountTests {

        @Test
        @DisplayName("Should return count from JSON total field")
        void shouldReturnCountFromJsonTotalField() throws Exception {
            // Given
            JsonNode response = objectMapper.readTree("{\"resourceType\": \"Bundle\", \"total\": 42}");
            setupGetChain(response);

            // When
            Integer count = fhirServiceClient.getResourceCount("Patient");

            // Then
            assertThat(count).isEqualTo(42);
        }

        @Test
        @DisplayName("Should return 0 when response has no total field")
        void shouldReturn0WhenNoTotalField() throws Exception {
            // Given
            JsonNode response = objectMapper.readTree("{\"resourceType\": \"Bundle\"}");
            setupGetChain(response);

            // When
            Integer count = fhirServiceClient.getResourceCount("Patient");

            // Then
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 0 on exception")
        void shouldReturn0OnException() {
            // Given
            setupGetChainWithError(new RuntimeException("Connection refused"));

            // When
            Integer count = fhirServiceClient.getResourceCount("Patient");

            // Then
            assertThat(count).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getResourceCountByCode Tests")
    class GetResourceCountByCodeTests {

        @Test
        @DisplayName("Should return count when code matches")
        void shouldReturnCountWhenCodeMatches() throws Exception {
            // Given
            JsonNode response = objectMapper.readTree("{\"resourceType\": \"Bundle\", \"total\": 15}");
            setupGetChain(response);

            // When
            Integer count = fhirServiceClient.getResourceCountByCode("Condition", "44054006");

            // Then
            assertThat(count).isEqualTo(15);
        }

        @Test
        @DisplayName("Should return 0 on exception")
        void shouldReturn0OnException() {
            // Given
            setupGetChainWithError(
                    WebClientResponseException.create(500, "Internal Server Error", null, null, null));

            // When
            Integer count = fhirServiceClient.getResourceCountByCode("Condition", "44054006");

            // Then
            assertThat(count).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getMetadata Tests")
    class GetMetadataTests {

        @Test
        @DisplayName("Should return metadata JsonNode")
        void shouldReturnMetadataJsonNode() throws Exception {
            // Given
            JsonNode metadata = objectMapper.readTree(
                    "{\"fhirVersion\": \"4.0.1\", \"status\": \"active\", \"resourceType\": \"CapabilityStatement\"}");
            setupGetChain(metadata);

            // When
            JsonNode result = fhirServiceClient.getMetadata();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("fhirVersion").asText()).isEqualTo("4.0.1");
            assertThat(result.get("status").asText()).isEqualTo("active");
        }

        @Test
        @DisplayName("Should return null on exception")
        void shouldReturnNullOnException() {
            // Given
            setupGetChainWithError(new RuntimeException("Service unavailable"));

            // When
            JsonNode result = fhirServiceClient.getMetadata();

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getResourceSamples Tests")
    class GetResourceSamplesTests {

        @Test
        @DisplayName("Should return resource bundle")
        void shouldReturnResourceBundle() throws Exception {
            // Given
            JsonNode bundle = objectMapper.readTree(
                    "{\"resourceType\": \"Bundle\", \"entry\": [{\"resource\": {\"resourceType\": \"Patient\"}}]}");
            setupGetChain(bundle);

            // When
            JsonNode result = fhirServiceClient.getResourceSamples("Patient", 5);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("entry")).isNotNull();
            assertThat(result.get("entry").size()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return null on exception")
        void shouldReturnNullOnException() {
            // Given
            setupGetChainWithError(new RuntimeException("Timeout"));

            // When
            JsonNode result = fhirServiceClient.getResourceSamples("Patient", 5);

            // Then
            assertThat(result).isNull();
        }
    }
}
