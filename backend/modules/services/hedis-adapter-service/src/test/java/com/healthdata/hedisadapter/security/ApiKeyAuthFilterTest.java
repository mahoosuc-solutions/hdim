package com.healthdata.hedisadapter.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ApiKeyAuthFilterTest {

    private ApiKeyAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthFilter("test-api-key-12345");
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @Test
    void validApiKey_allowsRequest() throws Exception {
        request.addHeader("X-API-Key", "test-api-key-12345");
        request.setRequestURI("/hedis-adapter/api/v1/external/hedis/measures/sync");

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    void missingApiKey_returns401() throws Exception {
        request.setRequestURI("/hedis-adapter/api/v1/external/hedis/measures/sync");

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("X-API-Key header is required");
        assertThat(filterChain.getRequest()).isNull();
    }

    @Test
    void wrongApiKey_returns401() throws Exception {
        request.addHeader("X-API-Key", "wrong-key");
        request.setRequestURI("/hedis-adapter/api/v1/external/hedis/measures/calculate");

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid API key");
    }

    @Test
    void actuatorEndpoint_skipsAuth() throws Exception {
        request.setRequestURI("/hedis-adapter/actuator/health");

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    void statusEndpoint_skipsAuth() throws Exception {
        request.setRequestURI("/hedis-adapter/api/v1/external/hedis/status");

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    void webSocketUpgrade_skipsApiKeyAuth() throws Exception {
        request.setRequestURI("/hedis-adapter/ws/events");

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isNotNull();
    }
}
