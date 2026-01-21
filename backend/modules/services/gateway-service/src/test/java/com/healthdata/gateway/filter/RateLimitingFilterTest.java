package com.healthdata.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.gateway.config.RateLimitConfiguration;
import com.healthdata.gateway.dto.RateLimitResult;
import com.healthdata.gateway.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Test Suite for RateLimitingFilter (Phase 2.0 Team 1)
 *
 * Tests cover:
 * - HTTP 429 responses
 * - Rate limit headers
 * - Excluded paths
 * - Client identification
 * - Proxy headers (X-Forwarded-For)
 * - Filter integration
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingFilter Tests")
class RateLimitingFilterTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private RateLimitConfiguration config;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RateLimitingFilter filter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String CLIENT_ID = "user:test-user";
    private static final String ENDPOINT = "/api/v1/patients/123";
    private static final String TENANT_ID = "tenant-001";

    @BeforeEach
    void setup() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        when(config.isEnabled()).thenReturn(true);
    }

    @Test
    @DisplayName("Should pass through request when rate limit not exceeded")
    void testRequestAllowedWhenBelowLimit() throws ServletException, IOException {
        setupRequest(ENDPOINT);

        RateLimitResult result = RateLimitResult.builder()
            .limit(1000)
            .current(10)
            .remaining(990)
            .allowed(true)
            .resetTime(Instant.now().plusSeconds(30))
            .retryAfterSeconds(0)
            .build();

        when(rateLimitService.checkLimit(anyString(), anyString(), anyString()))
            .thenReturn(result);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    @DisplayName("Should return 429 when rate limit exceeded")
    void testReturn429WhenLimitExceeded() throws ServletException, IOException {
        setupRequest(ENDPOINT);
        setupObjectMapper();

        RateLimitResult result = RateLimitResult.builder()
            .limit(1000)
            .current(1001)
            .remaining(0)
            .allowed(false)
            .resetTime(Instant.now().plusSeconds(30))
            .retryAfterSeconds(30)
            .build();

        when(rateLimitService.checkLimit(anyString(), anyString(), anyString()))
            .thenReturn(result);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Should include X-RateLimit-Limit header")
    void testXRateLimitLimitHeader() throws ServletException, IOException {
        setupRequest(ENDPOINT);
        setupObjectMapper();

        RateLimitResult result = RateLimitResult.builder()
            .limit(1000)
            .current(100)
            .remaining(900)
            .allowed(true)
            .resetTime(Instant.now().plusSeconds(30))
            .retryAfterSeconds(0)
            .build();

        when(rateLimitService.checkLimit(anyString(), anyString(), anyString()))
            .thenReturn(result);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader("X-RateLimit-Limit"))
            .isEqualTo("1000");
    }

    @Test
    @DisplayName("Should include X-RateLimit-Remaining header")
    void testXRateLimitRemainingHeader() throws ServletException, IOException {
        setupRequest(ENDPOINT);
        setupObjectMapper();

        RateLimitResult result = RateLimitResult.builder()
            .limit(1000)
            .current(100)
            .remaining(900)
            .allowed(true)
            .resetTime(Instant.now().plusSeconds(30))
            .retryAfterSeconds(0)
            .build();

        when(rateLimitService.checkLimit(anyString(), anyString(), anyString()))
            .thenReturn(result);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader("X-RateLimit-Remaining"))
            .isEqualTo("900");
    }

    @Test
    @DisplayName("Should include X-RateLimit-Reset header")
    void testXRateLimitResetHeader() throws ServletException, IOException {
        setupRequest(ENDPOINT);
        setupObjectMapper();

        Instant resetTime = Instant.now().plusSeconds(30);
        RateLimitResult result = RateLimitResult.builder()
            .limit(1000)
            .current(100)
            .remaining(900)
            .allowed(true)
            .resetTime(resetTime)
            .retryAfterSeconds(0)
            .build();

        when(rateLimitService.checkLimit(anyString(), anyString(), anyString()))
            .thenReturn(result);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader("X-RateLimit-Reset"))
            .isEqualTo(String.valueOf(resetTime.getEpochSecond()));
    }

    @Test
    @DisplayName("Should include Retry-After header when limited")
    void testRetryAfterHeader() throws ServletException, IOException {
        setupRequest(ENDPOINT);
        setupObjectMapper();

        RateLimitResult result = RateLimitResult.builder()
            .limit(1000)
            .current(1001)
            .remaining(0)
            .allowed(false)
            .resetTime(Instant.now().plusSeconds(45))
            .retryAfterSeconds(45)
            .build();

        when(rateLimitService.checkLimit(anyString(), anyString(), anyString()))
            .thenReturn(result);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader("Retry-After"))
            .isEqualTo("45");
    }

    @Test
    @DisplayName("Should set Content-Type to application/json on 429")
    void testContentTypeJson() throws ServletException, IOException {
        setupRequest(ENDPOINT);
        setupObjectMapper();

        RateLimitResult result = RateLimitResult.builder()
            .limit(1000)
            .current(1001)
            .remaining(0)
            .allowed(false)
            .resetTime(Instant.now().plusSeconds(30))
            .retryAfterSeconds(30)
            .build();

        when(rateLimitService.checkLimit(anyString(), anyString(), anyString()))
            .thenReturn(result);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getContentType())
            .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    @DisplayName("Should skip rate limiting for health endpoint")
    void testExcludedHealthEndpoint() throws ServletException, IOException {
        request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader("X-Tenant-ID", TENANT_ID);

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimitService, never()).checkLimit(anyString(), anyString(), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should skip rate limiting for actuator endpoint")
    void testExcludedActuatorEndpoint() throws ServletException, IOException {
        request = new MockHttpServletRequest("GET", "/actuator/metrics");
        request.addHeader("X-Tenant-ID", TENANT_ID);

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimitService, never()).checkLimit(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should skip rate limiting for swagger endpoint")
    void testExcludedSwaggerEndpoint() throws ServletException, IOException {
        request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        request.addHeader("X-Tenant-ID", TENANT_ID);

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimitService, never()).checkLimit(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should use user ID when JWT attribute present")
    void testClientIdFromJwtAttribute() throws ServletException, IOException {
        setupRequest(ENDPOINT);
        request.setAttribute("user_id", "user-123");

        RateLimitResult result = RateLimitResult.builder()
            .limit(1000)
            .current(1)
            .remaining(999)
            .allowed(true)
            .resetTime(Instant.now().plusSeconds(60))
            .retryAfterSeconds(0)
            .build();

        when(rateLimitService.checkLimit(
            argThat(arg -> arg.contains("user:user-123")),
            anyString(), anyString()))
            .thenReturn(result);

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimitService).checkLimit(
            argThat(arg -> arg.contains("user:user-123")),
            anyString(), anyString());
    }

    @Test
    @DisplayName("Should fall back to IP address when no JWT attribute")
    void testClientIdFromIpAddress() throws ServletException, IOException {
        request = new MockHttpServletRequest("GET", ENDPOINT);
        request.setRemoteAddr("192.168.1.100");
        request.addHeader("X-Tenant-ID", TENANT_ID);

        RateLimitResult result = RateLimitResult.builder()
            .limit(1000)
            .current(1)
            .remaining(999)
            .allowed(true)
            .resetTime(Instant.now().plusSeconds(60))
            .retryAfterSeconds(0)
            .build();

        when(rateLimitService.checkLimit(
            argThat(arg -> arg.contains("ip:192.168.1.100")),
            anyString(), anyString()))
            .thenReturn(result);

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimitService).checkLimit(
            argThat(arg -> arg.contains("ip:192.168.1.100")),
            anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle X-Forwarded-For header for proxy IP")
    void testXForwardedForHeader() throws ServletException, IOException {
        request = new MockHttpServletRequest("GET", ENDPOINT);
        request.addHeader("X-Forwarded-For", "203.0.113.45, 198.51.100.178");
        request.addHeader("X-Tenant-ID", TENANT_ID);
        request.setRemoteAddr("127.0.0.1");

        RateLimitResult result = RateLimitResult.builder()
            .limit(1000)
            .current(1)
            .remaining(999)
            .allowed(true)
            .resetTime(Instant.now().plusSeconds(60))
            .retryAfterSeconds(0)
            .build();

        when(rateLimitService.checkLimit(
            argThat(arg -> arg.contains("ip:203.0.113.45")),
            anyString(), anyString()))
            .thenReturn(result);

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimitService).checkLimit(
            argThat(arg -> arg.contains("ip:203.0.113.45")),
            anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle X-Real-IP header")
    void testXRealIpHeader() throws ServletException, IOException {
        request = new MockHttpServletRequest("GET", ENDPOINT);
        request.addHeader("X-Real-IP", "198.51.100.123");
        request.addHeader("X-Tenant-ID", TENANT_ID);
        request.setRemoteAddr("127.0.0.1");

        RateLimitResult result = RateLimitResult.builder()
            .limit(1000)
            .current(1)
            .remaining(999)
            .allowed(true)
            .resetTime(Instant.now().plusSeconds(60))
            .retryAfterSeconds(0)
            .build();

        when(rateLimitService.checkLimit(
            argThat(arg -> arg.contains("ip:198.51.100.123")),
            anyString(), anyString()))
            .thenReturn(result);

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimitService).checkLimit(
            argThat(arg -> arg.contains("ip:198.51.100.123")),
            anyString(), anyString());
    }

    @Test
    @DisplayName("Should disable rate limiting when config disabled")
    void testRateLimitingDisabled() throws ServletException, IOException {
        setupRequest(ENDPOINT);
        when(config.isEnabled()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimitService, never()).checkLimit(anyString(), anyString(), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle service exception gracefully (fail-open)")
    void testGracefulExceptionHandling() throws ServletException, IOException {
        setupRequest(ENDPOINT);

        when(rateLimitService.checkLimit(anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("Service error"));

        filter.doFilterInternal(request, response, filterChain);

        // Should continue despite exception
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should return error response body on 429")
    void testErrorResponseBodyOnLimit() throws ServletException, IOException {
        setupRequest(ENDPOINT);
        setupObjectMapper();

        RateLimitResult result = RateLimitResult.builder()
            .limit(1000)
            .current(1001)
            .remaining(0)
            .allowed(false)
            .resetTime(Instant.now().plusSeconds(30))
            .retryAfterSeconds(30)
            .build();

        when(rateLimitService.checkLimit(anyString(), anyString(), anyString()))
            .thenReturn(result);

        when(objectMapper.writeValueAsString(any()))
            .thenReturn("{\"error\":\"Rate Limit Exceeded\"}");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getContentAsString())
            .isNotEmpty();
    }

    @Test
    @DisplayName("Should include all rate limit headers on successful request")
    void testAllHeadersOnSuccess() throws ServletException, IOException {
        setupRequest(ENDPOINT);
        setupObjectMapper();

        Instant resetTime = Instant.now().plusSeconds(45);
        RateLimitResult result = RateLimitResult.builder()
            .limit(2000)
            .current(100)
            .remaining(1900)
            .allowed(true)
            .resetTime(resetTime)
            .retryAfterSeconds(0)
            .build();

        when(rateLimitService.checkLimit(anyString(), anyString(), anyString()))
            .thenReturn(result);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader("X-RateLimit-Limit"))
            .isEqualTo("2000");
        assertThat(response.getHeader("X-RateLimit-Remaining"))
            .isEqualTo("1900");
        assertThat(response.getHeader("X-RateLimit-Reset"))
            .isEqualTo(String.valueOf(resetTime.getEpochSecond()));
    }

    // ============ Helper Methods ============

    private void setupRequest(String endpoint) {
        request = new MockHttpServletRequest("GET", endpoint);
        request.addHeader("X-Tenant-ID", TENANT_ID);
        request.setRemoteAddr("192.168.1.100");
    }

    private void setupObjectMapper() {
        // No-op for now - ObjectMapper is mocked
    }
}
