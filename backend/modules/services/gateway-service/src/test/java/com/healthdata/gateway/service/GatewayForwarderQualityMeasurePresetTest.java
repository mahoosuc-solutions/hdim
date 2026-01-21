package com.healthdata.gateway.service;

import com.healthdata.gateway.config.GatewayAuthProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("GatewayForwarder -> Quality Measure preset routing")
class GatewayForwarderQualityMeasurePresetTest {

    @Test
    @DisplayName("Should forward preset request to quality-measure service")
    void shouldForwardDefaultPresetRequest() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        GatewayAuthProperties authProperties = new GatewayAuthProperties();
        authProperties.setEnforced(true);

        GatewayForwarder forwarder = new GatewayForwarder(restTemplate, authProperties);

        server.expect(once(), requestTo("http://quality-measure-service:8087/quality-measure/evaluation-presets/default"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer test-token"))
            .andExpect(header("X-Tenant-ID", "acme-health"))
            .andRespond(withSuccess("{\"measureId\":\"measure-1\"}", MediaType.APPLICATION_JSON));

        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/quality-measure/evaluation-presets/default");
        request.addHeader("Authorization", "Bearer test-token");
        request.addHeader("X-Tenant-ID", "acme-health");
        request.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE);

        var response = forwarder.forwardRequest(request, null, "http://quality-measure-service:8087/quality-measure", "/quality-measure");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("{\"measureId\":\"measure-1\"}");
        server.verify();
    }
}
