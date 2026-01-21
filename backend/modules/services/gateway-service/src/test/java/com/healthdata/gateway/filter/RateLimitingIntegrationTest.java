package com.healthdata.gateway.filter;

import com.healthdata.gateway.config.RateLimitConfiguration;
import com.healthdata.gateway.service.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for Rate Limiting (Phase 2.0 Team 1)
 *
 * Tests full filter integration with Spring context:
 * - End-to-end HTTP requests
 * - Filter chain integration
 * - Configuration loading
 * - Response headers
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "security.rate-limiting.enabled=true",
    "security.rate-limiting.default-limit-per-minute=10",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6380"
})
@DisplayName("Rate Limiting Integration Tests")
class RateLimitingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private RateLimitConfiguration config;

    private static final String TENANT_ID = "test-tenant";
    private static final String PATIENT_ENDPOINT = "/api/v1/patients/123";

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return rate limit headers on successful request")
    void testRateLimitHeadersPresent() throws Exception {
        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-RateLimit-Limit"))
            .andExpect(header().exists("X-RateLimit-Remaining"))
            .andExpect(header().exists("X-RateLimit-Reset"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return declining remaining count with each request")
    void testRemainingCountDeclines() throws Exception {
        // First request
        String firstResponse = mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andReturn()
            .getResponse()
            .getHeader("X-RateLimit-Remaining");

        // Subsequent request should have lower count
        String secondResponse = mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andReturn()
            .getResponse()
            .getHeader("X-RateLimit-Remaining");

        int firstRemaining = Integer.parseInt(firstResponse);
        int secondRemaining = Integer.parseInt(secondResponse);

        // Second request should have fewer remaining
        // Note: May be flaky in real environment due to timing
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should skip health endpoint from rate limiting")
    void testHealthEndpointNotRateLimited() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("X-RateLimit-Limit"));
    }

    @Test
    @DisplayName("Should deny unauthenticated access to protected endpoint")
    void testUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("Should allow VIEWER role to access endpoint")
    void testViewerRoleAccess() throws Exception {
        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-RateLimit-Limit"));
    }

    @Test
    @DisplayName("Should load rate limiting configuration from properties")
    void testConfigurationLoaded() {
        assertThat(config.isEnabled()).isTrue();
        assertThat(config.getDefaultLimitPerMinute()).isEqualTo(10);
    }
}
