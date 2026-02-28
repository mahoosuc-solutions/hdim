package com.healthdata.gateway.clinical;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayClinicalController")
class GatewayClinicalControllerTest {

    @Mock
    private GatewayForwarder forwarder;

    @InjectMocks
    private GatewayClinicalController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "payerWorkflowsUrl", "http://payer-workflows-service:8098");
        ReflectionTestUtils.setField(controller, "dataIngestionUrl", "http://data-ingestion-service:8080");
        when(forwarder.forwardRequest(any(), any(), any(), any())).thenReturn(ResponseEntity.ok().build());
    }

    @Test
    @DisplayName("Routes revenue contract requests to payer-workflows service")
    void routesRevenueContractRequests() {
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
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/interoperability/adt/messages");
        controller.routeToAdtInteroperability(request, "{\"eventType\":\"A01\"}");

        verify(forwarder).forwardRequest(
                eq(request),
                eq("{\"eventType\":\"A01\"}"),
                eq("http://data-ingestion-service:8080"),
                eq("/api/v1/interoperability/adt")
        );
    }
}
