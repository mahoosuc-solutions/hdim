package com.healthdata.gateway.clinical;

import com.healthdata.gateway.clinical.executive.CmoOnboardingAggregationService;
import com.healthdata.gateway.service.GatewayForwarder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayClinicalController")
class GatewayClinicalControllerTest {

    @Mock
    private GatewayForwarder forwarder;

    @Mock
    private CmoOnboardingAggregationService cmoOnboardingAggregationService;

    @InjectMocks
    private GatewayClinicalController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "payerWorkflowsUrl", "http://payer-workflows-service:8098");
        ReflectionTestUtils.setField(controller, "dataIngestionUrl", "http://data-ingestion-service:8080");
    }

    @Test
    @DisplayName("Routes revenue contract requests to payer-workflows service")
    void routesRevenueContractRequests() {
        when(forwarder.forwardRequest(any(), any(), any(), any())).thenReturn(ResponseEntity.ok().build());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/revenue/claims/submissions");
        controller.routeToRevenueContracts(request, "{\"claimId\":\"abc\"}");

        verify(forwarder).forwardRequest(
                eq(request),
                eq("{\"claimId\":\"abc\"}"),
                eq("http://payer-workflows-service:8098"),
                eq("/api/v1/revenue")
        );
    }

    @Test
    @DisplayName("Routes ADT interoperability requests to data-ingestion service")
    void routesAdtInteroperabilityRequests() {
        when(forwarder.forwardRequest(any(), any(), any(), any())).thenReturn(ResponseEntity.ok().build());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/interoperability/adt/messages");
        controller.routeToAdtInteroperability(request, "{\"eventType\":\"A01\"}");

        verify(forwarder).forwardRequest(
                eq(request),
                eq("{\"eventType\":\"A01\"}"),
                eq("http://data-ingestion-service:8080"),
                eq("/api/v1/interoperability/adt")
        );
    }

    @Test
    @DisplayName("Returns CMO onboarding summary contract")
    @SuppressWarnings("unchecked")
    void returnsCmoOnboardingSummaryContract() {
        when(cmoOnboardingAggregationService.buildSummary(eq("acme-health"), eq("Bearer token")))
            .thenReturn(Map.of(
                "tenantId", "acme-health",
                "kpis", List.of(
                    Map.of("label", "Care Gap Closure Rate", "value", "68%", "trend", "+6.2 pts", "status", "improving"),
                    Map.of("label", "High-Risk Intervention Completion", "value", "74%", "trend", "+4.8 pts", "status", "improving"),
                    Map.of("label", "Data Freshness SLA", "value", "99.1%", "trend", "+0.7 pts", "status", "stable"),
                    Map.of("label", "Compliance Evidence Completion", "value", "92%", "trend", "+12.0 pts", "status", "improving")
                ),
                "topActions", List.of("a1", "a2", "a3"),
                "governanceSignals", List.of("g1", "g2", "g3", "g4")
            ));

        ResponseEntity<?> response = controller.getCmoOnboardingSummary("acme-health", "Bearer token");

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("acme-health", body.get("tenantId"));

        List<Map<String, Object>> kpis = (List<Map<String, Object>>) body.get("kpis");
        assertNotNull(kpis);
        assertEquals(4, kpis.size());

        List<String> topActions = (List<String>) body.get("topActions");
        assertNotNull(topActions);
        assertEquals(3, topActions.size());

        List<String> governanceSignals = (List<String>) body.get("governanceSignals");
        assertNotNull(governanceSignals);
        assertEquals(4, governanceSignals.size());
    }
}
