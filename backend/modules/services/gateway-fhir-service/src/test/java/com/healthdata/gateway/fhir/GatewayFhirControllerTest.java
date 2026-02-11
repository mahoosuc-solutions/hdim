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
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for GatewayFhirController.
 * Tests the FHIR gateway routing controller functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Gateway FHIR Controller Tests")
@Tag("unit")
class GatewayFhirControllerTest {

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
    @DisplayName("CQL Engine Routing Tests")
    class CqlEngineRoutingTests {

        @Test
        @DisplayName("Should route /api/cql/* requests to CQL engine service")
        void shouldRouteToCqlEngineViaApi() {
            // Given
            String body = "{\"expression\": \"Patient.name\"}";
            ResponseEntity<?> expectedResponse = ResponseEntity.ok("result");
            doReturn(expectedResponse)
                    .when(forwarder).forwardRequest(any(), eq(body), eq(CQL_ENGINE_URL), eq("/api/cql"));

            // When
            ResponseEntity<?> result = controller.routeToCqlEngine(request, body);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(forwarder).forwardRequest(request, body, CQL_ENGINE_URL, "/api/cql");
        }

        @Test
        @DisplayName("Should route /cql-engine/* requests directly to CQL engine service")
        void shouldRouteToCqlEngineDirect() {
            // Given
            String body = "{\"measureId\": \"CMS122\"}";
            ResponseEntity<?> expectedResponse = ResponseEntity.ok("evaluation result");
            doReturn(expectedResponse)
                    .when(forwarder).forwardRequest(any(), eq(body), eq(CQL_ENGINE_URL), eq("/cql-engine"));

            // When
            ResponseEntity<?> result = controller.routeToCqlEngineDirect(request, body);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(forwarder).forwardRequest(request, body, CQL_ENGINE_URL, "/cql-engine");
        }
    }

    @Nested
    @DisplayName("Quality Measure Routing Tests")
    class QualityMeasureRoutingTests {

        @Test
        @DisplayName("Should route /api/quality/* requests to quality measure service")
        void shouldRouteToQualityMeasureViaApi() {
            // Given
            String body = "{\"measureId\": \"CMS125\"}";
            ResponseEntity<?> expectedResponse = ResponseEntity.ok("measure data");
            doReturn(expectedResponse)
                    .when(forwarder).forwardRequest(any(), eq(body), eq(QUALITY_MEASURE_URL), eq("/api/quality"));

            // When
            ResponseEntity<?> result = controller.routeToQualityMeasure(request, body);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(forwarder).forwardRequest(request, body, QUALITY_MEASURE_URL, "/api/quality");
        }

        @Test
        @DisplayName("Should route /quality-measure/* requests directly to quality measure service")
        void shouldRouteToQualityMeasureDirect() {
            // Given
            String body = null;
            ResponseEntity<?> expectedResponse = ResponseEntity.ok("all measures");
            doReturn(expectedResponse)
                    .when(forwarder).forwardRequest(any(), eq(body), eq(QUALITY_MEASURE_URL), eq("/quality-measure"));

            // When
            ResponseEntity<?> result = controller.routeToQualityMeasureDirect(request, body);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(forwarder).forwardRequest(request, body, QUALITY_MEASURE_URL, "/quality-measure");
        }
    }

    @Nested
    @DisplayName("FHIR Service Routing Tests")
    class FhirServiceRoutingTests {

        @Test
        @DisplayName("Should route /api/fhir/* requests to FHIR service")
        void shouldRouteToFhirViaApi() {
            // Given
            String body = "{\"resourceType\": \"Patient\"}";
            ResponseEntity<?> expectedResponse = ResponseEntity.ok("patient bundle");
            doReturn(expectedResponse)
                    .when(forwarder).forwardRequest(any(), eq(body), eq(FHIR_URL), eq("/api/fhir"));

            // When
            ResponseEntity<?> result = controller.routeToFhir(request, body);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(forwarder).forwardRequest(request, body, FHIR_URL, "/api/fhir");
        }

        @Test
        @DisplayName("Should route /fhir/* requests directly to FHIR service")
        void shouldRouteToFhirDirect() {
            // Given
            String body = null;
            ResponseEntity<?> expectedResponse = ResponseEntity.ok("capability statement");
            doReturn(expectedResponse)
                    .when(forwarder).forwardRequest(any(), eq(body), eq(FHIR_URL), eq("/fhir"));

            // When
            ResponseEntity<?> result = controller.routeToFhirDirect(request, body);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(forwarder).forwardRequest(request, body, FHIR_URL, "/fhir");
        }
    }

    @Nested
    @DisplayName("Patient Service Routing Tests")
    class PatientServiceRoutingTests {

        @Test
        @DisplayName("Should route /api/patients/* requests to patient service")
        void shouldRouteToPatientViaApi() {
            // Given
            String body = "{\"name\": \"John Doe\"}";
            ResponseEntity<?> expectedResponse = ResponseEntity.ok("patient created");
            doReturn(expectedResponse)
                    .when(forwarder).forwardRequest(any(), eq(body), eq(PATIENT_URL), eq("/api/patients"));

            // When
            ResponseEntity<?> result = controller.routeToPatient(request, body);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(forwarder).forwardRequest(request, body, PATIENT_URL, "/api/patients");
        }

        @Test
        @DisplayName("Should route /patient/* requests directly to patient service")
        void shouldRouteToPatientDirect() {
            // Given
            String body = null;
            ResponseEntity<?> expectedResponse = ResponseEntity.ok("patient list");
            doReturn(expectedResponse)
                    .when(forwarder).forwardRequest(any(), eq(body), eq(PATIENT_URL), eq("/patient"));

            // When
            ResponseEntity<?> result = controller.routeToPatientDirect(request, body);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(forwarder).forwardRequest(request, body, PATIENT_URL, "/patient");
        }
    }

    @Nested
    @DisplayName("Request Body Handling Tests")
    class RequestBodyHandlingTests {

        @Test
        @DisplayName("Should handle null request body")
        void shouldHandleNullRequestBody() {
            // Given
            ResponseEntity<?> expectedResponse = ResponseEntity.ok("result");
            doReturn(expectedResponse)
                    .when(forwarder).forwardRequest(any(), eq(null), eq(FHIR_URL), eq("/api/fhir"));

            // When
            ResponseEntity<?> result = controller.routeToFhir(request, null);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(forwarder).forwardRequest(request, null, FHIR_URL, "/api/fhir");
        }

        @Test
        @DisplayName("Should handle empty request body")
        void shouldHandleEmptyRequestBody() {
            // Given
            String body = "";
            ResponseEntity<?> expectedResponse = ResponseEntity.ok("result");
            doReturn(expectedResponse)
                    .when(forwarder).forwardRequest(any(), eq(body), eq(PATIENT_URL), eq("/patient"));

            // When
            ResponseEntity<?> result = controller.routeToPatientDirect(request, body);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(forwarder).forwardRequest(request, body, PATIENT_URL, "/patient");
        }

        @Test
        @DisplayName("Should pass large JSON body to forwarder")
        void shouldPassLargeJsonBody() {
            // Given
            String body = "{\"patients\": [" + "\"patient-data\",".repeat(100) + "\"last\"]}";
            ResponseEntity<?> expectedResponse = ResponseEntity.ok("batch processed");
            doReturn(expectedResponse)
                    .when(forwarder).forwardRequest(any(), eq(body), eq(CQL_ENGINE_URL), eq("/api/cql"));

            // When
            ResponseEntity<?> result = controller.routeToCqlEngine(request, body);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(forwarder).forwardRequest(request, body, CQL_ENGINE_URL, "/api/cql");
        }
    }
}
