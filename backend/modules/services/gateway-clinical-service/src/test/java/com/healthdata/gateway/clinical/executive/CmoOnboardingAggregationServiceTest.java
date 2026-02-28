package com.healthdata.gateway.clinical.executive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CmoOnboardingAggregationService")
class CmoOnboardingAggregationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private CmoOnboardingAggregationService service;

    @BeforeEach
    void setUp() {
        service = new CmoOnboardingAggregationService(restTemplate);
        ReflectionTestUtils.setField(service, "careGapUrl", "http://care-gap-service:8086");
        ReflectionTestUtils.setField(service, "analyticsUrl", "http://analytics-service:8092");
    }

    @Test
    @DisplayName("Builds summary from live source values when available")
    @SuppressWarnings("unchecked")
    void buildsSummaryFromLiveSources() {
        when(restTemplate.exchange(
            eq("http://care-gap-service:8086/care-gap/population-report"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map.class))
        ).thenReturn(ResponseEntity.ok(Map.of("openGapCount", 17, "closureRate", "71")));

        when(restTemplate.exchange(
            eq("http://analytics-service:8092/api/analytics/kpis"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map.class))
        ).thenReturn(ResponseEntity.ok(Map.of(
            "highRiskInterventionCompletion", "77",
            "dataFreshnessSla", "98.5",
            "complianceEvidenceCompletion", "89"
        )));

        Map<String, Object> result = service.buildSummary("acme-health", "Bearer abc");
        assertNotNull(result);
        assertEquals("acme-health", result.get("tenantId"));

        var kpis = (java.util.List<Map<String, Object>>) result.get("kpis");
        assertEquals("71%", kpis.get(0).get("value"));
        assertEquals("77%", kpis.get(1).get("value"));
        assertEquals("98.5%", kpis.get(2).get("value"));
        assertEquals("89%", kpis.get(3).get("value"));
    }

    @Test
    @DisplayName("Falls back to defaults when live source calls fail")
    @SuppressWarnings("unchecked")
    void fallsBackWhenLiveSourcesFail() {
        when(restTemplate.exchange(
            eq("http://care-gap-service:8086/care-gap/population-report"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map.class))
        ).thenThrow(new RuntimeException("care gap unavailable"));

        when(restTemplate.exchange(
            eq("http://analytics-service:8092/api/analytics/kpis"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map.class))
        ).thenThrow(new RuntimeException("analytics unavailable"));

        Map<String, Object> result = service.buildSummary("acme-health", null);
        assertNotNull(result);

        var kpis = (java.util.List<Map<String, Object>>) result.get("kpis");
        assertEquals("68%", kpis.get(0).get("value"));
        assertEquals("74%", kpis.get(1).get("value"));
        assertEquals("99.1%", kpis.get(2).get("value"));
        assertEquals("92%", kpis.get(3).get("value"));
    }
}
