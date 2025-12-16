package com.healthdata.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ServiceRoutingService.
 * Tests circuit breaker, resilience, and service routing functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Service Routing Service Tests")
class ServiceRoutingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private ServiceRoutingService serviceRoutingService;

    // Test configuration values
    private static final String CQL_ENGINE_URL = "http://localhost:8081";
    private static final String QUALITY_MEASURE_URL = "http://localhost:8082";
    private static final String FHIR_URL = "http://localhost:8083";
    private static final String PATIENT_URL = "http://localhost:8084";
    private static final String CARE_GAP_URL = "http://localhost:8085";

    @BeforeEach
    void setUp() {
        serviceRoutingService = new ServiceRoutingService(restTemplate);

        // Set service URLs using reflection
        ReflectionTestUtils.setField(serviceRoutingService, "cqlEngineUrl", CQL_ENGINE_URL);
        ReflectionTestUtils.setField(serviceRoutingService, "qualityMeasureUrl", QUALITY_MEASURE_URL);
        ReflectionTestUtils.setField(serviceRoutingService, "fhirUrl", FHIR_URL);
        ReflectionTestUtils.setField(serviceRoutingService, "patientUrl", PATIENT_URL);
        ReflectionTestUtils.setField(serviceRoutingService, "careGapUrl", CARE_GAP_URL);
    }

    @Nested
    @DisplayName("CQL Engine Service Routing Tests")
    class CqlEngineRoutingTests {

        @Nested
        @DisplayName("Successful Routing")
        class SuccessfulRouting {

            @Test
            @DisplayName("Should route to correct CQL Engine service URL")
            void shouldRouteToCorrectServiceUrl() throws Exception {
                // Given
                String path = "/api/cql/evaluate";
                HttpMethod method = HttpMethod.POST;
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                String body = "{\"expression\":\"test\"}";

                ResponseEntity<String> expectedResponse = ResponseEntity.ok("Success");
                ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);

                when(restTemplate.exchange(
                    uriCaptor.capture(),
                    eq(method),
                    any(HttpEntity.class),
                    eq(String.class)
                )).thenReturn(expectedResponse);

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.routeToCqlEngine(path, method, headers, body);

                // Then
                assertThat(result).isNotNull();
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEqualTo("Success");
                assertThat(uriCaptor.getValue().toString()).isEqualTo(CQL_ENGINE_URL + path);
            }

            @Test
            @DisplayName("Should forward headers correctly")
            void shouldForwardHeadersCorrectly() throws Exception {
                // Given
                String path = "/api/cql/evaluate";
                HttpMethod method = HttpMethod.GET;
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Tenant-ID", "tenant-123");
                headers.set("Authorization", "Bearer token123");
                headers.setContentType(MediaType.APPLICATION_JSON);

                ResponseEntity<String> expectedResponse = ResponseEntity.ok("Success");
                ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

                when(restTemplate.exchange(
                    any(URI.class),
                    eq(method),
                    entityCaptor.capture(),
                    eq(String.class)
                )).thenReturn(expectedResponse);

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.routeToCqlEngine(path, method, headers, null);

                // Then
                result.get();
                @SuppressWarnings("unchecked")
                HttpEntity<String> capturedEntity = entityCaptor.getValue();
                assertThat(capturedEntity.getHeaders().get("X-Tenant-ID")).containsExactly("tenant-123");
                assertThat(capturedEntity.getHeaders().get("Authorization")).containsExactly("Bearer token123");
                assertThat(capturedEntity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
            }

            @Test
            @DisplayName("Should return service response")
            void shouldReturnServiceResponse() throws Exception {
                // Given
                String path = "/api/cql/evaluate";
                HttpMethod method = HttpMethod.POST;
                HttpHeaders headers = new HttpHeaders();
                String body = "{\"result\":\"calculated\"}";
                String responseBody = "{\"value\":42}";

                ResponseEntity<String> expectedResponse = ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody);

                when(restTemplate.exchange(
                    any(URI.class),
                    eq(method),
                    any(HttpEntity.class),
                    eq(String.class)
                )).thenReturn(expectedResponse);

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.routeToCqlEngine(path, method, headers, body);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEqualTo(responseBody);
                assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
            }
        }

        @Nested
        @DisplayName("Circuit Breaker Fallback")
        class CircuitBreakerFallback {

            @Test
            @DisplayName("Should return fallback when service unavailable")
            void shouldReturnFallbackWhenServiceUnavailable() throws Exception {
                // Given
                String path = "/api/cql/evaluate";
                HttpMethod method = HttpMethod.POST;
                HttpHeaders headers = new HttpHeaders();
                String body = "{\"expression\":\"test\"}";

                HttpServerErrorException exception = new HttpServerErrorException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Service Unavailable"
                );

                // When - Directly call fallback method
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.cqlEngineFallback(path, method, headers, body, exception);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                assertThat(response.getBody()).contains("Service Unavailable");
                assertThat(response.getBody()).contains("CQL Engine Service");
            }

            @Test
            @DisplayName("Should return correct error status code")
            void shouldReturnCorrectErrorStatusCode() throws Exception {
                // Given
                String path = "/api/cql/evaluate";
                HttpMethod method = HttpMethod.POST;
                HttpHeaders headers = new HttpHeaders();
                String body = "{\"expression\":\"test\"}";
                Exception exception = new RuntimeException("Connection refused");

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.cqlEngineFallback(path, method, headers, body, exception);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
            }

            @Test
            @DisplayName("Should include service name in fallback response")
            void shouldIncludeServiceNameInFallbackResponse() throws Exception {
                // Given
                String path = "/api/cql/evaluate";
                HttpMethod method = HttpMethod.POST;
                HttpHeaders headers = new HttpHeaders();
                String body = "{\"expression\":\"test\"}";
                Exception exception = new RuntimeException("Timeout");

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.cqlEngineFallback(path, method, headers, body, exception);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getBody()).contains("\"service\":\"CQL Engine Service\"");
                assertThat(response.getBody()).contains("\"status\":503");
            }

            @Test
            @DisplayName("Should include Retry-After header in fallback response")
            void shouldIncludeRetryAfterHeader() throws Exception {
                // Given
                String path = "/api/cql/evaluate";
                HttpMethod method = HttpMethod.POST;
                HttpHeaders headers = new HttpHeaders();
                String body = "{\"expression\":\"test\"}";
                Exception exception = new RuntimeException("Service down");

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.cqlEngineFallback(path, method, headers, body, exception);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getHeaders().get("Retry-After")).containsExactly("30");
            }
        }

        @Nested
        @DisplayName("Error Handling")
        class ErrorHandling {

            @Test
            @DisplayName("Should handle timeouts")
            void shouldHandleTimeouts() throws Exception {
                // Given
                String path = "/api/cql/evaluate";
                HttpMethod method = HttpMethod.POST;
                HttpHeaders headers = new HttpHeaders();
                String body = "{\"expression\":\"test\"}";

                ResourceAccessException timeoutException = new ResourceAccessException(
                    "Timeout",
                    new SocketTimeoutException("Read timed out")
                );

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.cqlEngineFallback(path, method, headers, body, timeoutException);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                assertThat(response.getBody()).contains("temporarily unavailable");
            }

            @Test
            @DisplayName("Should handle network errors")
            void shouldHandleNetworkErrors() throws Exception {
                // Given
                String path = "/api/cql/evaluate";
                HttpMethod method = HttpMethod.POST;
                HttpHeaders headers = new HttpHeaders();
                String body = "{\"expression\":\"test\"}";

                ResourceAccessException networkException = new ResourceAccessException(
                    "Connection refused"
                );

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.cqlEngineFallback(path, method, headers, body, networkException);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                assertThat(response.getBody()).contains("Service Unavailable");
            }
        }
    }

    @Nested
    @DisplayName("Quality Measure Service Routing Tests")
    class QualityMeasureRoutingTests {

        @Nested
        @DisplayName("Successful Routing")
        class SuccessfulRouting {

            @Test
            @DisplayName("Should route to correct Quality Measure service URL")
            void shouldRouteToCorrectServiceUrl() throws Exception {
                // Given
                String path = "/api/measures/calculate";
                HttpMethod method = HttpMethod.POST;
                HttpHeaders headers = new HttpHeaders();
                String body = "{\"measureId\":\"CMS123\"}";

                ResponseEntity<String> expectedResponse = ResponseEntity.ok("{\"score\":85}");
                ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);

                when(restTemplate.exchange(
                    uriCaptor.capture(),
                    eq(method),
                    any(HttpEntity.class),
                    eq(String.class)
                )).thenReturn(expectedResponse);

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.routeToQualityMeasure(path, method, headers, body);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(uriCaptor.getValue().toString()).isEqualTo(QUALITY_MEASURE_URL + path);
            }

            @Test
            @DisplayName("Should forward GET requests correctly")
            void shouldForwardGetRequests() throws Exception {
                // Given
                String path = "/api/measures/CMS123";
                HttpMethod method = HttpMethod.GET;
                HttpHeaders headers = new HttpHeaders();
                headers.set("Accept", "application/json");

                ResponseEntity<String> expectedResponse = ResponseEntity.ok("{\"id\":\"CMS123\"}");

                when(restTemplate.exchange(
                    any(URI.class),
                    eq(method),
                    any(HttpEntity.class),
                    eq(String.class)
                )).thenReturn(expectedResponse);

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.routeToQualityMeasure(path, method, headers, null);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).contains("CMS123");
            }
        }

        @Nested
        @DisplayName("Circuit Breaker Fallback")
        class CircuitBreakerFallback {

            @Test
            @DisplayName("Should return fallback when service fails")
            void shouldReturnFallbackWhenServiceFails() throws Exception {
                // Given
                String path = "/api/measures/calculate";
                HttpMethod method = HttpMethod.POST;
                HttpHeaders headers = new HttpHeaders();
                String body = "{\"measureId\":\"CMS123\"}";
                Exception exception = new RuntimeException("Service error");

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.qualityMeasureFallback(path, method, headers, body, exception);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                assertThat(response.getBody()).contains("Quality Measure Service");
            }

            @Test
            @DisplayName("Should include error metadata in fallback")
            void shouldIncludeErrorMetadata() throws Exception {
                // Given
                String path = "/api/measures/calculate";
                HttpMethod method = HttpMethod.POST;
                HttpHeaders headers = new HttpHeaders();
                String body = "{\"measureId\":\"CMS123\"}";
                Exception exception = new RuntimeException("Database connection failed");

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.qualityMeasureFallback(path, method, headers, body, exception);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getBody()).contains("\"error\":\"Service Unavailable\"");
                assertThat(response.getBody()).contains("\"status\":503");
            }
        }
    }

    @Nested
    @DisplayName("FHIR Service Routing Tests")
    class FhirServiceRoutingTests {

        @Nested
        @DisplayName("Successful Routing")
        class SuccessfulRouting {

            @Test
            @DisplayName("Should route to correct FHIR service URL")
            void shouldRouteToCorrectServiceUrl() throws Exception {
                // Given
                String path = "/fhir/Patient/123";
                HttpMethod method = HttpMethod.GET;
                HttpHeaders headers = new HttpHeaders();
                headers.set("Accept", "application/fhir+json");

                ResponseEntity<String> expectedResponse = ResponseEntity.ok("{\"resourceType\":\"Patient\"}");
                ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);

                when(restTemplate.exchange(
                    uriCaptor.capture(),
                    eq(method),
                    any(HttpEntity.class),
                    eq(String.class)
                )).thenReturn(expectedResponse);

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.routeToFhir(path, method, headers, null);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(uriCaptor.getValue().toString()).isEqualTo(FHIR_URL + path);
            }

            @Test
            @DisplayName("Should handle FHIR Bundle creation")
            void shouldHandleFhirBundleCreation() throws Exception {
                // Given
                String path = "/fhir/Bundle";
                HttpMethod method = HttpMethod.POST;
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.valueOf("application/fhir+json"));
                String body = "{\"resourceType\":\"Bundle\"}";

                ResponseEntity<String> expectedResponse = ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("{\"id\":\"bundle-123\"}");

                when(restTemplate.exchange(
                    any(URI.class),
                    eq(method),
                    any(HttpEntity.class),
                    eq(String.class)
                )).thenReturn(expectedResponse);

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.routeToFhir(path, method, headers, body);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            }
        }

        @Nested
        @DisplayName("Circuit Breaker Fallback")
        class CircuitBreakerFallback {

            @Test
            @DisplayName("Should return fallback for FHIR service failure")
            void shouldReturnFallbackForServiceFailure() throws Exception {
                // Given
                String path = "/fhir/Patient/123";
                HttpMethod method = HttpMethod.GET;
                HttpHeaders headers = new HttpHeaders();
                Exception exception = new RuntimeException("FHIR server unavailable");

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.fhirServiceFallback(path, method, headers, null, exception);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                assertThat(response.getBody()).contains("FHIR Service");
            }
        }
    }

    @Nested
    @DisplayName("Patient Service Routing Tests")
    class PatientServiceRoutingTests {

        @Nested
        @DisplayName("Successful Routing")
        class SuccessfulRouting {

            @Test
            @DisplayName("Should route to correct Patient service URL")
            void shouldRouteToCorrectServiceUrl() throws Exception {
                // Given
                String path = "/api/patients/12345";
                HttpMethod method = HttpMethod.GET;
                HttpHeaders headers = new HttpHeaders();

                ResponseEntity<String> expectedResponse = ResponseEntity.ok("{\"patientId\":\"12345\"}");
                ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);

                when(restTemplate.exchange(
                    uriCaptor.capture(),
                    eq(method),
                    any(HttpEntity.class),
                    eq(String.class)
                )).thenReturn(expectedResponse);

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.routeToPatient(path, method, headers, null);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(uriCaptor.getValue().toString()).isEqualTo(PATIENT_URL + path);
            }

            @Test
            @DisplayName("Should handle patient updates")
            void shouldHandlePatientUpdates() throws Exception {
                // Given
                String path = "/api/patients/12345";
                HttpMethod method = HttpMethod.PUT;
                HttpHeaders headers = new HttpHeaders();
                String body = "{\"name\":\"John Doe\"}";

                ResponseEntity<String> expectedResponse = ResponseEntity.ok("{\"updated\":true}");

                when(restTemplate.exchange(
                    any(URI.class),
                    eq(method),
                    any(HttpEntity.class),
                    eq(String.class)
                )).thenReturn(expectedResponse);

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.routeToPatient(path, method, headers, body);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).contains("updated");
            }
        }

        @Nested
        @DisplayName("Circuit Breaker Fallback")
        class CircuitBreakerFallback {

            @Test
            @DisplayName("Should return fallback for Patient service failure")
            void shouldReturnFallbackForServiceFailure() throws Exception {
                // Given
                String path = "/api/patients/12345";
                HttpMethod method = HttpMethod.GET;
                HttpHeaders headers = new HttpHeaders();
                Exception exception = new RuntimeException("Patient service down");

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.patientServiceFallback(path, method, headers, null, exception);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                assertThat(response.getBody()).contains("Patient Service");
            }
        }
    }

    @Nested
    @DisplayName("Care Gap Service Routing Tests")
    class CareGapServiceRoutingTests {

        @Nested
        @DisplayName("Successful Routing")
        class SuccessfulRouting {

            @Test
            @DisplayName("Should route to correct Care Gap service URL")
            void shouldRouteToCorrectServiceUrl() throws Exception {
                // Given
                String path = "/api/care-gaps/patient/12345";
                HttpMethod method = HttpMethod.GET;
                HttpHeaders headers = new HttpHeaders();

                ResponseEntity<String> expectedResponse = ResponseEntity.ok("{\"gaps\":[]}");
                ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);

                when(restTemplate.exchange(
                    uriCaptor.capture(),
                    eq(method),
                    any(HttpEntity.class),
                    eq(String.class)
                )).thenReturn(expectedResponse);

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.routeToCareGap(path, method, headers, null);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(uriCaptor.getValue().toString()).isEqualTo(CARE_GAP_URL + path);
            }

            @Test
            @DisplayName("Should handle care gap analysis requests")
            void shouldHandleCareGapAnalysis() throws Exception {
                // Given
                String path = "/api/care-gaps/analyze";
                HttpMethod method = HttpMethod.POST;
                HttpHeaders headers = new HttpHeaders();
                String body = "{\"patientId\":\"12345\"}";

                ResponseEntity<String> expectedResponse = ResponseEntity.ok(
                    "{\"gaps\":[{\"measure\":\"CMS125\",\"status\":\"open\"}]}"
                );

                when(restTemplate.exchange(
                    any(URI.class),
                    eq(method),
                    any(HttpEntity.class),
                    eq(String.class)
                )).thenReturn(expectedResponse);

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.routeToCareGap(path, method, headers, body);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).contains("gaps");
                assertThat(response.getBody()).contains("CMS125");
            }
        }

        @Nested
        @DisplayName("Circuit Breaker Fallback")
        class CircuitBreakerFallback {

            @Test
            @DisplayName("Should return fallback for Care Gap service failure")
            void shouldReturnFallbackForServiceFailure() throws Exception {
                // Given
                String path = "/api/care-gaps/patient/12345";
                HttpMethod method = HttpMethod.GET;
                HttpHeaders headers = new HttpHeaders();
                Exception exception = new RuntimeException("Care gap service unavailable");

                // When
                CompletableFuture<ResponseEntity<String>> result =
                    serviceRoutingService.careGapServiceFallback(path, method, headers, null, exception);

                // Then
                ResponseEntity<String> response = result.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                assertThat(response.getBody()).contains("Care Gap Service");
            }
        }
    }

    @Nested
    @DisplayName("Common Routing Behavior Tests")
    class CommonRoutingBehaviorTests {

        @Test
        @DisplayName("Should handle null body in request")
        void shouldHandleNullBody() throws Exception {
            // Given
            String path = "/api/health";
            HttpMethod method = HttpMethod.GET;
            HttpHeaders headers = new HttpHeaders();

            ResponseEntity<String> expectedResponse = ResponseEntity.ok("OK");

            when(restTemplate.exchange(
                any(URI.class),
                eq(method),
                any(HttpEntity.class),
                eq(String.class)
            )).thenReturn(expectedResponse);

            // When
            CompletableFuture<ResponseEntity<String>> result =
                serviceRoutingService.routeToCqlEngine(path, method, headers, null);

            // Then
            ResponseEntity<String> response = result.get();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should handle empty headers")
        void shouldHandleEmptyHeaders() throws Exception {
            // Given
            String path = "/api/test";
            HttpMethod method = HttpMethod.GET;
            HttpHeaders headers = new HttpHeaders();

            ResponseEntity<String> expectedResponse = ResponseEntity.ok("Success");

            when(restTemplate.exchange(
                any(URI.class),
                eq(method),
                any(HttpEntity.class),
                eq(String.class)
            )).thenReturn(expectedResponse);

            // When
            CompletableFuture<ResponseEntity<String>> result =
                serviceRoutingService.routeToFhir(path, method, headers, null);

            // Then
            ResponseEntity<String> response = result.get();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should preserve response status codes")
        void shouldPreserveResponseStatusCodes() throws Exception {
            // Given
            String path = "/api/resource";
            HttpMethod method = HttpMethod.POST;
            HttpHeaders headers = new HttpHeaders();
            String body = "{\"data\":\"test\"}";

            ResponseEntity<String> expectedResponse = ResponseEntity
                .status(HttpStatus.CREATED)
                .body("{\"id\":\"123\"}");

            when(restTemplate.exchange(
                any(URI.class),
                eq(method),
                any(HttpEntity.class),
                eq(String.class)
            )).thenReturn(expectedResponse);

            // When
            CompletableFuture<ResponseEntity<String>> result =
                serviceRoutingService.routeToPatient(path, method, headers, body);

            // Then
            ResponseEntity<String> response = result.get();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("Should preserve response headers")
        void shouldPreserveResponseHeaders() throws Exception {
            // Given
            String path = "/api/data";
            HttpMethod method = HttpMethod.GET;
            HttpHeaders headers = new HttpHeaders();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set("X-Custom-Header", "custom-value");
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> expectedResponse = ResponseEntity
                .ok()
                .headers(responseHeaders)
                .body("{\"result\":\"data\"}");

            when(restTemplate.exchange(
                any(URI.class),
                eq(method),
                any(HttpEntity.class),
                eq(String.class)
            )).thenReturn(expectedResponse);

            // When
            CompletableFuture<ResponseEntity<String>> result =
                serviceRoutingService.routeToCareGap(path, method, headers, null);

            // Then
            ResponseEntity<String> response = result.get();
            assertThat(response.getHeaders().get("X-Custom-Header")).containsExactly("custom-value");
            assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        }
    }

    @Nested
    @DisplayName("Fallback Response Creation Tests")
    class FallbackResponseCreationTests {

        @Test
        @DisplayName("Should create well-formed JSON fallback response")
        void shouldCreateWellFormedJsonFallback() throws Exception {
            // Given
            String path = "/api/test";
            HttpMethod method = HttpMethod.GET;
            HttpHeaders headers = new HttpHeaders();
            Exception exception = new RuntimeException("Test exception");

            // When
            CompletableFuture<ResponseEntity<String>> result =
                serviceRoutingService.cqlEngineFallback(path, method, headers, null, exception);

            // Then
            ResponseEntity<String> response = result.get();
            String body = response.getBody();

            assertThat(body).isNotNull();
            assertThat(body).contains("\"error\":");
            assertThat(body).contains("\"service\":");
            assertThat(body).contains("\"message\":");
            assertThat(body).contains("\"status\":");
        }

        @Test
        @DisplayName("Should set correct content type for fallback")
        void shouldSetCorrectContentTypeForFallback() throws Exception {
            // Given
            String path = "/api/test";
            HttpMethod method = HttpMethod.POST;
            HttpHeaders headers = new HttpHeaders();
            Exception exception = new RuntimeException("Service error");

            // When
            CompletableFuture<ResponseEntity<String>> result =
                serviceRoutingService.qualityMeasureFallback(path, method, headers, null, exception);

            // Then
            ResponseEntity<String> response = result.get();
            assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        }

        @Test
        @DisplayName("Should include retry guidance in fallback")
        void shouldIncludeRetryGuidance() throws Exception {
            // Given
            String path = "/api/test";
            HttpMethod method = HttpMethod.GET;
            HttpHeaders headers = new HttpHeaders();
            Exception exception = new RuntimeException("Temporary failure");

            // When
            CompletableFuture<ResponseEntity<String>> result =
                serviceRoutingService.fhirServiceFallback(path, method, headers, null, exception);

            // Then
            ResponseEntity<String> response = result.get();
            assertThat(response.getBody()).contains("Please try again later");
        }
    }

    @Nested
    @DisplayName("Asynchronous Execution Tests")
    class AsynchronousExecutionTests {

        @Test
        @DisplayName("Should execute routing asynchronously")
        void shouldExecuteRoutingAsynchronously() {
            // Given
            String path = "/api/async-test";
            HttpMethod method = HttpMethod.GET;
            HttpHeaders headers = new HttpHeaders();

            ResponseEntity<String> expectedResponse = ResponseEntity.ok("Async Success");

            when(restTemplate.exchange(
                any(URI.class),
                eq(method),
                any(HttpEntity.class),
                eq(String.class)
            )).thenReturn(expectedResponse);

            // When
            CompletableFuture<ResponseEntity<String>> result =
                serviceRoutingService.routeToCqlEngine(path, method, headers, null);

            // Then
            assertThat(result).isInstanceOf(CompletableFuture.class);
            // Future may or may not be completed yet depending on timing
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should complete future successfully on success")
        void shouldCompleteFutureSuccessfully() throws Exception {
            // Given
            String path = "/api/test";
            HttpMethod method = HttpMethod.GET;
            HttpHeaders headers = new HttpHeaders();

            ResponseEntity<String> expectedResponse = ResponseEntity.ok("Success");

            when(restTemplate.exchange(
                any(URI.class),
                eq(method),
                any(HttpEntity.class),
                eq(String.class)
            )).thenReturn(expectedResponse);

            // When
            CompletableFuture<ResponseEntity<String>> result =
                serviceRoutingService.routeToQualityMeasure(path, method, headers, null);

            // Then
            assertThat(result.get()).isNotNull();
            assertThat(result.isCompletedExceptionally()).isFalse();
        }
    }

    @Nested
    @DisplayName("Multiple Service Differentiation Tests")
    class MultipleServiceDifferentiationTests {

        @Test
        @DisplayName("Should route to different service URLs correctly")
        void shouldRouteToDifferentServiceUrls() throws Exception {
            // Given
            String path = "/api/test";
            HttpMethod method = HttpMethod.GET;
            HttpHeaders headers = new HttpHeaders();

            ResponseEntity<String> response = ResponseEntity.ok("Success");

            ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
            when(restTemplate.exchange(
                uriCaptor.capture(),
                eq(method),
                any(HttpEntity.class),
                eq(String.class)
            )).thenReturn(response);

            // When - Call multiple services
            serviceRoutingService.routeToCqlEngine(path, method, headers, null).get();
            String cqlUrl = uriCaptor.getValue().toString();

            serviceRoutingService.routeToQualityMeasure(path, method, headers, null).get();
            String qualityUrl = uriCaptor.getValue().toString();

            serviceRoutingService.routeToFhir(path, method, headers, null).get();
            String fhirServiceUrl = uriCaptor.getValue().toString();

            serviceRoutingService.routeToPatient(path, method, headers, null).get();
            String patientUrl = uriCaptor.getValue().toString();

            serviceRoutingService.routeToCareGap(path, method, headers, null).get();
            String careGapServiceUrl = uriCaptor.getValue().toString();

            // Then - Verify each service has unique URL
            assertThat(cqlUrl).isEqualTo(CQL_ENGINE_URL + path);
            assertThat(qualityUrl).isEqualTo(QUALITY_MEASURE_URL + path);
            assertThat(fhirServiceUrl).isEqualTo(FHIR_URL + path);
            assertThat(patientUrl).isEqualTo(PATIENT_URL + path);
            assertThat(careGapServiceUrl).isEqualTo(CARE_GAP_URL + path);
        }

        @Test
        @DisplayName("Should have different fallback service names")
        void shouldHaveDifferentFallbackServiceNames() throws Exception {
            // Given
            String path = "/api/test";
            HttpMethod method = HttpMethod.GET;
            HttpHeaders headers = new HttpHeaders();
            Exception exception = new RuntimeException("Error");

            // When
            String cqlFallback = serviceRoutingService
                .cqlEngineFallback(path, method, headers, null, exception)
                .get().getBody();

            String qualityFallback = serviceRoutingService
                .qualityMeasureFallback(path, method, headers, null, exception)
                .get().getBody();

            String fhirFallback = serviceRoutingService
                .fhirServiceFallback(path, method, headers, null, exception)
                .get().getBody();

            String patientFallback = serviceRoutingService
                .patientServiceFallback(path, method, headers, null, exception)
                .get().getBody();

            String careGapFallback = serviceRoutingService
                .careGapServiceFallback(path, method, headers, null, exception)
                .get().getBody();

            // Then
            assertThat(cqlFallback).contains("CQL Engine Service");
            assertThat(qualityFallback).contains("Quality Measure Service");
            assertThat(fhirFallback).contains("FHIR Service");
            assertThat(patientFallback).contains("Patient Service");
            assertThat(careGapFallback).contains("Care Gap Service");
        }
    }
}
