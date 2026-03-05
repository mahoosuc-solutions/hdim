package com.healthdata.gateway.fhir;

import com.healthdata.gateway.service.GatewayForwarder;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Edge case and coverage gap tests for GatewayFhirController.
 * Covers error propagation, non-2xx response passthrough, the V1 quality
 * measure route, and concurrent route isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Gateway FHIR Controller Edge Case Tests")
@Tag("unit")
class GatewayFhirRoutingEdgeCaseTest {

    @Mock
    private GatewayForwarder forwarder;

    @Mock
    private HttpServletRequest request;

    private GatewayFhirController controller;

    private static final String CQL_ENGINE_URL = "http://cql-engine:8081";
    private static final String QUALITY_MEASURE_URL = "http://quality-measure:8087";
    private static final String FHIR_URL = "http://fhir-service:8085";
    private static final String PATIENT_URL = "http://patient-service:8084";

    @BeforeEach
    void setUp() {
        controller = new GatewayFhirController(forwarder);
        ReflectionTestUtils.setField(controller, "cqlEngineUrl", CQL_ENGINE_URL);
        ReflectionTestUtils.setField(controller, "qualityMeasureUrl", QUALITY_MEASURE_URL);
        ReflectionTestUtils.setField(controller, "fhirUrl", FHIR_URL);
        ReflectionTestUtils.setField(controller, "patientUrl", PATIENT_URL);
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should propagate RuntimeException from forwarder on CQL route")
        void shouldPropagateRuntimeExceptionFromCqlRoute() {
            // Given
            doThrow(new RuntimeException("Connection refused"))
                    .when(forwarder).forwardRequest(any(), any(), eq(CQL_ENGINE_URL), eq("/api/cql"));

            // When / Then
            assertThatThrownBy(() -> controller.routeToCqlEngine(request, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Connection refused");
        }

        @Test
        @DisplayName("Should propagate RuntimeException from forwarder on FHIR route")
        void shouldPropagateRuntimeExceptionFromFhirRoute() {
            // Given
            doThrow(new RuntimeException("Service unavailable"))
                    .when(forwarder).forwardRequest(any(), any(), eq(FHIR_URL), eq("/api/fhir"));

            // When / Then
            assertThatThrownBy(() -> controller.routeToFhir(request, "{\"resourceType\":\"Patient\"}"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Service unavailable");
        }

        @Test
        @DisplayName("Should propagate RuntimeException from forwarder on patient route")
        void shouldPropagateRuntimeExceptionFromPatientRoute() {
            // Given
            doThrow(new RuntimeException("Read timed out"))
                    .when(forwarder).forwardRequest(any(), any(), eq(PATIENT_URL), eq("/patient"));

            // When / Then
            assertThatThrownBy(() -> controller.routeToPatientDirect(request, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Read timed out");
        }
    }

    @Nested
    @DisplayName("Response Passthrough Tests")
    class ResponsePassthroughTests {

        @Test
        @DisplayName("Should pass through 404 Not Found response from forwarder")
        void shouldPassThrough404Response() {
            // Given
            ResponseEntity<?> notFoundResponse = ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("{\"error\":\"Patient not found\"}");
            doReturn(notFoundResponse)
                    .when(forwarder).forwardRequest(any(), any(), eq(PATIENT_URL), eq("/api/patients"));

            // When
            ResponseEntity<?> result = controller.routeToPatient(request, null);

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(result.getBody()).isEqualTo("{\"error\":\"Patient not found\"}");
        }

        @Test
        @DisplayName("Should pass through 500 Internal Server Error response from forwarder")
        void shouldPassThrough500Response() {
            // Given
            ResponseEntity<?> serverErrorResponse = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"Internal server error\"}");
            doReturn(serverErrorResponse)
                    .when(forwarder).forwardRequest(any(), any(), eq(FHIR_URL), eq("/fhir"));

            // When
            ResponseEntity<?> result = controller.routeToFhirDirect(request, null);

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(result.getBody()).isEqualTo("{\"error\":\"Internal server error\"}");
        }

        @Test
        @DisplayName("Should pass through 422 Unprocessable Entity response from forwarder")
        void shouldPassThrough422Response() {
            // Given
            String invalidBody = "{\"resourceType\": \"Invalid\"}";
            ResponseEntity<?> unprocessableResponse = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body("{\"error\":\"Invalid FHIR resource\"}");
            doReturn(unprocessableResponse)
                    .when(forwarder).forwardRequest(any(), eq(invalidBody), eq(CQL_ENGINE_URL), eq("/cql-engine"));

            // When
            ResponseEntity<?> result = controller.routeToCqlEngineDirect(request, invalidBody);

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(result.getBody()).isEqualTo("{\"error\":\"Invalid FHIR resource\"}");
        }
    }

    @Nested
    @DisplayName("Additional Route Coverage Tests")
    class AdditionalRouteTests {

        @Test
        @DisplayName("Should route /api/v1/quality-measures/* requests to quality measure service")
        void shouldRouteToQualityMeasureV1() {
            // Given
            String body = "{\"measureId\":\"CMS130v12\",\"period\":\"2026\"}";
            ResponseEntity<?> expectedResponse = ResponseEntity.ok("v1 measure result");
            doReturn(expectedResponse)
                    .when(forwarder).forwardRequest(any(), eq(body), eq(QUALITY_MEASURE_URL), eq("/api/v1/quality-measures"));

            // When
            ResponseEntity<?> result = controller.routeToQualityMeasureV1(request, body);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(forwarder).forwardRequest(request, body, QUALITY_MEASURE_URL, "/api/v1/quality-measures");
        }

        @Test
        @DisplayName("Should route /api/v1/quality-measures/* with null body")
        void shouldRouteToQualityMeasureV1WithNullBody() {
            // Given
            ResponseEntity<?> expectedResponse = ResponseEntity.ok("all v1 measures");
            doReturn(expectedResponse)
                    .when(forwarder).forwardRequest(any(), eq(null), eq(QUALITY_MEASURE_URL), eq("/api/v1/quality-measures"));

            // When
            ResponseEntity<?> result = controller.routeToQualityMeasureV1(request, null);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(forwarder).forwardRequest(request, null, QUALITY_MEASURE_URL, "/api/v1/quality-measures");
        }

        @Test
        @DisplayName("Should isolate concurrent routes to different backend services")
        void shouldIsolateConcurrentRoutesToDifferentServices() {
            // Given - set up distinct responses for four different routes
            ResponseEntity<?> cqlResponse = ResponseEntity.ok("cql-result");
            ResponseEntity<?> fhirResponse = ResponseEntity.ok("fhir-result");
            ResponseEntity<?> patientResponse = ResponseEntity.ok("patient-result");
            ResponseEntity<?> qualityResponse = ResponseEntity.ok("quality-result");

            doReturn(cqlResponse)
                    .when(forwarder).forwardRequest(any(), eq(null), eq(CQL_ENGINE_URL), eq("/api/cql"));
            doReturn(fhirResponse)
                    .when(forwarder).forwardRequest(any(), eq(null), eq(FHIR_URL), eq("/api/fhir"));
            doReturn(patientResponse)
                    .when(forwarder).forwardRequest(any(), eq(null), eq(PATIENT_URL), eq("/api/patients"));
            doReturn(qualityResponse)
                    .when(forwarder).forwardRequest(any(), eq(null), eq(QUALITY_MEASURE_URL), eq("/api/v1/quality-measures"));

            // When - invoke all four routes
            ResponseEntity<?> cqlResult = controller.routeToCqlEngine(request, null);
            ResponseEntity<?> fhirResult = controller.routeToFhir(request, null);
            ResponseEntity<?> patientResult = controller.routeToPatient(request, null);
            ResponseEntity<?> qualityResult = controller.routeToQualityMeasureV1(request, null);

            // Then - each route returns its own distinct response
            assertThat(cqlResult.getBody()).isEqualTo("cql-result");
            assertThat(fhirResult.getBody()).isEqualTo("fhir-result");
            assertThat(patientResult.getBody()).isEqualTo("patient-result");
            assertThat(qualityResult.getBody()).isEqualTo("quality-result");
        }
    }
}
