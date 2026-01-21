package com.healthdata.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.healthdata.gateway.service.GatewayForwarder;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiGatewayController")
class ApiGatewayControllerTest {

    @Mock
    private GatewayForwarder gatewayForwarder;

    @Captor
    private ArgumentCaptor<HttpServletRequest> requestCaptor;

    @Captor
    private ArgumentCaptor<String> bodyCaptor;

    @Captor
    private ArgumentCaptor<String> serviceUrlCaptor;

    @Captor
    private ArgumentCaptor<String> pathPrefixCaptor;

    private ApiGatewayController controller;

    @BeforeEach
    void setUp() {
        controller = new ApiGatewayController(gatewayForwarder);
        ReflectionTestUtils.setField(controller, "cqlEngineUrl", "http://cql");
        ReflectionTestUtils.setField(controller, "qualityMeasureUrl", "http://qm");
        ReflectionTestUtils.setField(controller, "fhirUrl", "http://fhir");
        ReflectionTestUtils.setField(controller, "patientUrl", "http://patient");
        ReflectionTestUtils.setField(controller, "careGapUrl", "http://care-gap");
        ReflectionTestUtils.setField(controller, "agentBuilderUrl", "http://agent-builder");
        ReflectionTestUtils.setField(controller, "consentUrl", "http://consent");
        ReflectionTestUtils.setField(controller, "eventsUrl", "http://events");
        ReflectionTestUtils.setField(controller, "agentRuntimeUrl", "http://agent-runtime");
        ReflectionTestUtils.setField(controller, "qrdaExportUrl", "http://qrda");
        ReflectionTestUtils.setField(controller, "hccUrl", "http://hcc");
        ReflectionTestUtils.setField(controller, "ecrUrl", "http://ecr");
        ReflectionTestUtils.setField(controller, "priorAuthUrl", "http://prior-auth");
        ReflectionTestUtils.setField(controller, "salesAutomationUrl", "http://sales");
    }

    @Test
    @DisplayName("Should forward requests to configured services")
    void shouldForwardRequests() {
        ResponseEntity<?> mockResponse = ResponseEntity.ok().header("Connection", "close").body("ok");
        doReturn(mockResponse).when(gatewayForwarder)
            .forwardRequest(any(HttpServletRequest.class), nullable(String.class), anyString(), anyString());

        @SuppressWarnings("unchecked")
        List<ResponseEntity<?>> responses = (List<ResponseEntity<?>>) (List<?>) List.of(
            controller.routeToCqlEngine(request("/api/cql/test", "GET"), null),
            controller.routeToCqlEngineDirect(request("/cql-engine/test", "GET"), null),
            controller.routeToQualityMeasure(request("/api/quality/test", "GET"), null),
            controller.routeToQualityMeasureDirect(request("/quality-measure/test", "GET"), null),
            controller.routeToFhir(request("/api/fhir/test", "GET"), null),
            controller.routeToFhirDirect(request("/fhir/test", "GET"), null),
            controller.routeToPatient(request("/api/patients/test", "GET"), null),
            controller.routeToPatientDirect(request("/patient/test", "GET"), null),
            controller.routeToCareGap(request("/api/care-gaps/test", "GET"), null),
            controller.routeToCareGapDirect(request("/care-gap/test", "GET"), null),
            controller.routeToConsent(request("/api/consent/test", "GET"), null),
            controller.routeToConsentDirect(request("/consent/test", "GET"), null),
            controller.routeToEvents(request("/api/events/test", "GET"), null),
            controller.routeToEventsDirect(request("/events/test", "GET"), null),
            controller.routeToAgentBuilder(request("/api/v1/agent-builder/test", "GET"), null),
            controller.routeToAgentRuntimeTools(request("/api/v1/tools/test", "GET"), null),
            controller.routeToAgentRuntimeProviders(request("/api/v1/providers/test", "GET"), null),
            controller.routeToAgentRuntimeHealth(request("/api/v1/runtime/test", "GET"), null),
            controller.routeToQrdaExport(request("/api/v1/qrda/test", "GET"), null),
            controller.routeToHcc(request("/api/v1/hcc/test", "GET"), null),
            controller.routeToEcr(request("/api/ecr/test", "GET"), null),
            controller.routeToPriorAuth(request("/api/v1/prior-auth/test", "GET"), null),
            controller.routeToProviderAccess(request("/api/v1/provider-access/test", "GET"), null),
            controller.routeToSalesAutomation(request("/api/sales/test", "GET"), null),
            controller.routeToSalesAutomationDirect(request("/sales-automation/test", "GET"), null)
        );

        assertThat(responses).allSatisfy(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK));
        verify(gatewayForwarder, times(responses.size()))
            .forwardRequest(any(HttpServletRequest.class), nullable(String.class), anyString(), anyString());
    }

    @Test
    @DisplayName("Should forward headers correctly")
    void shouldForwardHeaders() {
        ResponseEntity<?> mockResponse = ResponseEntity.ok()
            .header("Content-Type", "application/json")
            .body("ok");
        doReturn(mockResponse).when(gatewayForwarder).forwardRequest(
            requestCaptor.capture(),
            bodyCaptor.capture(),
            serviceUrlCaptor.capture(),
            pathPrefixCaptor.capture());

        ResponseEntity<?> response = controller.routeToCqlEngine(
            request("/api/cql/test", "POST"),
            "payload"
        );

        // Verify forwarder was called with correct parameters
        HttpServletRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getRequestURI()).isEqualTo("/api/cql/test");
        assertThat(capturedRequest.getMethod()).isEqualTo("POST");
        assertThat(bodyCaptor.getValue()).isEqualTo("payload");
        assertThat(serviceUrlCaptor.getValue()).isEqualTo("http://cql");
        assertThat(pathPrefixCaptor.getValue()).isEqualTo("/api/cql");

        assertThat(response.getBody()).isEqualTo("ok");
    }

    @Test
    @DisplayName("Should return error response when forwarding fails")
    void shouldHandleForwardingErrors() {
        ResponseEntity<?> mockErrorResponse = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Gateway error: Service unavailable");
        doReturn(mockErrorResponse).when(gatewayForwarder)
            .forwardRequest(any(HttpServletRequest.class), nullable(String.class), anyString(), anyString());

        ResponseEntity<?> response = controller.routeToCqlEngine(request("/api/cql/test", "GET"), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).toString().contains("Gateway error");
    }

    @Test
    @DisplayName("Should forward to correct service URLs")
    void shouldForwardToCorrectServiceUrls() {
        ResponseEntity<?> mockResponse = ResponseEntity.ok().body("ok");
        doReturn(mockResponse).when(gatewayForwarder)
            .forwardRequest(any(), nullable(String.class), anyString(), anyString());

        // Test various routes
        controller.routeToCqlEngine(request("/api/cql/test", "GET"), null);
        verify(gatewayForwarder).forwardRequest(any(), nullable(String.class), eq("http://cql"), eq("/api/cql"));

        controller.routeToFhir(request("/api/fhir/test", "GET"), null);
        verify(gatewayForwarder).forwardRequest(any(), nullable(String.class), eq("http://fhir"), eq("/api/fhir"));

        controller.routeToPatient(request("/api/patients/test", "GET"), null);
        verify(gatewayForwarder).forwardRequest(any(), nullable(String.class), eq("http://patient"), eq("/api/patients"));
    }

    private HttpServletRequest request(String uri, String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setQueryString("foo=bar");
        request.addHeader("Authorization", "Bearer token");
        request.addHeader("X-Tenant-ID", "tenant-1");
        request.addHeader("X-User-ID", "user-1");
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Accept", "application/json");
        request.addHeader("Connection", "keep-alive");
        return request;
    }
}
