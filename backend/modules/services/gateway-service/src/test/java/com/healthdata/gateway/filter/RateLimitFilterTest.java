package com.healthdata.gateway.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitFilter.
 * Tests API Gateway rate limiting based on user ID, tenant ID, or client IP.
 *
 * Security Critical: These tests verify rate limiting enforcement to prevent
 * DoS attacks and ensure system availability for legitimate operations.
 *
 * HIPAA Compliance: Rate limiting helps protect PHI by preventing abuse
 * and maintaining system availability for healthcare operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Rate Limit Filter Tests")
class RateLimitFilterTest {

    @Mock
    private RateLimiterRegistry rateLimiterRegistry;

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter filter;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(rateLimiterRegistry);
        ReflectionTestUtils.setField(filter, "requestsPerSecond", 100);
        ReflectionTestUtils.setField(filter, "burstCapacity", 150);

        responseWriter = new StringWriter();
    }

    private void setupResponseWriter() throws IOException {
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Nested
    @DisplayName("Key Determination")
    class KeyDetermination {

        @Test
        @DisplayName("Should use user ID when authenticated")
        void shouldUseUserIdWhenAuthenticated() throws ServletException, IOException {
            // Given
            String userId = "user-123";
            String expectedKey = "user:" + userId;

            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(userId);
            when(rateLimiterRegistry.rateLimiter(expectedKey)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimiterRegistry).rateLimiter(expectedKey);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should use tenant ID when user not authenticated")
        void shouldUseTenantIdWhenNoUser() throws ServletException, IOException {
            // Given
            String tenantId = "tenant-456";
            String expectedKey = "tenant:" + tenantId;

            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(null);
            when(request.getHeader("X-Tenant-ID")).thenReturn(tenantId);
            // getRemoteAddr not stubbed - not needed when tenant ID is present
            when(rateLimiterRegistry.rateLimiter(expectedKey)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimiterRegistry).rateLimiter(expectedKey);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should use client IP as fallback")
        void shouldUseClientIpAsFallback() throws ServletException, IOException {
            // Given
            String clientIp = "10.0.0.100";
            String expectedKey = "ip:" + clientIp;

            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(null);
            when(request.getHeader("X-Tenant-ID")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(rateLimiterRegistry.rateLimiter(expectedKey)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimiterRegistry).rateLimiter(expectedKey);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should prefer user ID over tenant ID")
        void shouldPreferUserIdOverTenantId() throws ServletException, IOException {
            // Given
            String userId = "user-123";
            String expectedKey = "user:" + userId;

            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(userId);
            // X-Tenant-ID not stubbed - not checked when user ID is present
            when(rateLimiterRegistry.rateLimiter(expectedKey)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimiterRegistry).rateLimiter(expectedKey);
            verify(request, never()).getRemoteAddr();
        }

        @Test
        @DisplayName("Should prefer tenant ID over IP address")
        void shouldPreferTenantIdOverIp() throws ServletException, IOException {
            // Given
            String tenantId = "tenant-456";
            String expectedKey = "tenant:" + tenantId;

            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(null);
            when(request.getHeader("X-Tenant-ID")).thenReturn(tenantId);
            when(rateLimiterRegistry.rateLimiter(expectedKey)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimiterRegistry).rateLimiter(expectedKey);
            verify(request, never()).getRemoteAddr();
        }

        @Test
        @DisplayName("Should handle empty user ID")
        void shouldHandleEmptyUserId() throws ServletException, IOException {
            // Given
            String tenantId = "tenant-456";
            String expectedKey = "tenant:" + tenantId;

            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn("");
            when(request.getHeader("X-Tenant-ID")).thenReturn(tenantId);
            when(rateLimiterRegistry.rateLimiter(expectedKey)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimiterRegistry).rateLimiter(expectedKey);
        }

        @Test
        @DisplayName("Should handle empty tenant ID")
        void shouldHandleEmptyTenantId() throws ServletException, IOException {
            // Given
            String clientIp = "10.0.0.100";
            String expectedKey = "ip:" + clientIp;

            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(null);
            when(request.getHeader("X-Tenant-ID")).thenReturn("");
            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(rateLimiterRegistry.rateLimiter(expectedKey)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimiterRegistry).rateLimiter(expectedKey);
        }
    }

    @Nested
    @DisplayName("IP Extraction")
    class IpExtraction {

        @Test
        @DisplayName("Should handle X-Forwarded-For header")
        void shouldHandleXForwardedForHeader() throws ServletException, IOException {
            // Given
            String forwardedFor = "203.0.113.195, 70.41.3.18, 150.172.238.178";
            String expectedIp = "203.0.113.195"; // First IP in chain
            String expectedKey = "ip:" + expectedIp;

            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(null);
            when(request.getHeader("X-Tenant-ID")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedFor);
            when(rateLimiterRegistry.rateLimiter(expectedKey)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimiterRegistry).rateLimiter(expectedKey);
            verify(request, never()).getRemoteAddr();
        }

        @Test
        @DisplayName("Should handle X-Forwarded-For with single IP")
        void shouldHandleXForwardedForWithSingleIp() throws ServletException, IOException {
            // Given
            String forwardedFor = "203.0.113.195";
            String expectedKey = "ip:" + forwardedFor;

            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(null);
            when(request.getHeader("X-Tenant-ID")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedFor);
            when(rateLimiterRegistry.rateLimiter(expectedKey)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimiterRegistry).rateLimiter(expectedKey);
        }

        @Test
        @DisplayName("Should trim whitespace from X-Forwarded-For")
        void shouldTrimWhitespaceFromXForwardedFor() throws ServletException, IOException {
            // Given
            String forwardedFor = "  203.0.113.195  , 70.41.3.18";
            String expectedIp = "203.0.113.195";
            String expectedKey = "ip:" + expectedIp;

            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(null);
            when(request.getHeader("X-Tenant-ID")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedFor);
            when(rateLimiterRegistry.rateLimiter(expectedKey)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimiterRegistry).rateLimiter(expectedKey);
        }

        @Test
        @DisplayName("Should handle X-Real-IP header")
        void shouldHandleXRealIpHeader() throws ServletException, IOException {
            // Given
            String realIp = "198.51.100.42";
            String expectedKey = "ip:" + realIp;

            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(null);
            when(request.getHeader("X-Tenant-ID")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(realIp);
            when(rateLimiterRegistry.rateLimiter(expectedKey)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimiterRegistry).rateLimiter(expectedKey);
            verify(request, never()).getRemoteAddr();
        }

        @Test
        @DisplayName("Should prefer X-Forwarded-For over X-Real-IP")
        void shouldPreferXForwardedForOverXRealIp() throws ServletException, IOException {
            // Given
            String forwardedFor = "203.0.113.195";
            String expectedKey = "ip:" + forwardedFor;

            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(null);
            when(request.getHeader("X-Tenant-ID")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedFor);
            // X-Real-IP not stubbed - not checked when X-Forwarded-For is present
            when(rateLimiterRegistry.rateLimiter(expectedKey)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimiterRegistry).rateLimiter(expectedKey);
        }

        @Test
        @DisplayName("Should fallback to RemoteAddr when no headers present")
        void shouldFallbackToRemoteAddr() throws ServletException, IOException {
            // Given
            String remoteAddr = "172.16.0.50";
            String expectedKey = "ip:" + remoteAddr;

            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(null);
            when(request.getHeader("X-Tenant-ID")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn(remoteAddr);
            when(rateLimiterRegistry.rateLimiter(expectedKey)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimiterRegistry).rateLimiter(expectedKey);
            verify(request).getRemoteAddr();
        }

        @Test
        @DisplayName("Should handle empty X-Forwarded-For header")
        void shouldHandleEmptyXForwardedForHeader() throws ServletException, IOException {
            // Given
            String realIp = "198.51.100.42";
            String expectedKey = "ip:" + realIp;

            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(null);
            when(request.getHeader("X-Tenant-ID")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn("");
            when(request.getHeader("X-Real-IP")).thenReturn(realIp);
            when(rateLimiterRegistry.rateLimiter(expectedKey)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimiterRegistry).rateLimiter(expectedKey);
        }

        @Test
        @DisplayName("Should handle empty X-Real-IP header")
        void shouldHandleEmptyXRealIpHeader() throws ServletException, IOException {
            // Given
            String remoteAddr = "172.16.0.50";
            String expectedKey = "ip:" + remoteAddr;

            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(null);
            when(request.getHeader("X-Tenant-ID")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("");
            when(request.getRemoteAddr()).thenReturn(remoteAddr);
            when(rateLimiterRegistry.rateLimiter(expectedKey)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimiterRegistry).rateLimiter(expectedKey);
        }
    }

    @Nested
    @DisplayName("Rate Limiting")
    class RateLimiting {

        @Test
        @DisplayName("Should consume from rate limiter on success")
        void shouldConsumeFromRateLimiter() throws ServletException, IOException {
            // Given
            String userId = "user-123";

            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(userId);
            when(rateLimiterRegistry.rateLimiter("user:" + userId)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimiter).acquirePermission();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should return 429 with Retry-After header when rate limited")
        void shouldReturn429WithRetryAfterHeader() throws ServletException, IOException {
            // Given
            String userId = "user-123";

            setupResponseWriter();
            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(userId);
            when(rateLimiterRegistry.rateLimiter("user:" + userId)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(429);
            verify(response).setContentType("application/json");
            verify(response).setHeader("Retry-After", "1");
            verify(response).setHeader("X-RateLimit-Limit", "100");
            verify(response).setHeader("X-RateLimit-Remaining", "0");
            verify(filterChain, never()).doFilter(any(), any());

            String responseBody = responseWriter.toString();
            assertThat(responseBody).contains("Too Many Requests");
            assertThat(responseBody).contains("Rate limit exceeded");
            assertThat(responseBody).contains("\"status\":429");
            assertThat(responseBody).contains("\"key\":\"user\"");
        }

        @Test
        @DisplayName("Should only expose key type in error response, not value")
        void shouldOnlyExposeKeyTypeInErrorResponse() throws ServletException, IOException {
            // Given
            String userId = "user-123";

            setupResponseWriter();
            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(userId);
            when(rateLimiterRegistry.rateLimiter("user:" + userId)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            String responseBody = responseWriter.toString();
            assertThat(responseBody).contains("\"key\":\"user\"");
            assertThat(responseBody).doesNotContain(userId);
        }

        @Test
        @DisplayName("Should expose tenant key type without value")
        void shouldExposeTenantKeyTypeWithoutValue() throws ServletException, IOException {
            // Given
            String tenantId = "tenant-456";

            setupResponseWriter();
            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(null);
            when(request.getHeader("X-Tenant-ID")).thenReturn(tenantId);
            when(rateLimiterRegistry.rateLimiter("tenant:" + tenantId)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            String responseBody = responseWriter.toString();
            assertThat(responseBody).contains("\"key\":\"tenant\"");
            assertThat(responseBody).doesNotContain(tenantId);
        }

        @Test
        @DisplayName("Should expose IP key type without value")
        void shouldExposeIpKeyTypeWithoutValue() throws ServletException, IOException {
            // Given
            String clientIp = "10.0.0.100";

            setupResponseWriter();
            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getAttribute("userId")).thenReturn(null);
            when(request.getHeader("X-Tenant-ID")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(rateLimiterRegistry.rateLimiter("ip:" + clientIp)).thenReturn(rateLimiter);
            when(rateLimiter.acquirePermission()).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            String responseBody = responseWriter.toString();
            assertThat(responseBody).contains("\"key\":\"ip\"");
            assertThat(responseBody).doesNotContain(clientIp);
        }

        @Test
        @DisplayName("Should bypass rate limiting for actuator health endpoint")
        void shouldBypassRateLimitingForActuatorHealth() throws ServletException, IOException {
            // Given
            when(request.getRequestURI()).thenReturn("/actuator/health");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verify(rateLimiterRegistry, never()).rateLimiter(anyString());
            verify(rateLimiter, never()).acquirePermission();
        }

        @Test
        @DisplayName("Should bypass rate limiting for health endpoint")
        void shouldBypassRateLimitingForHealthEndpoint() throws ServletException, IOException {
            // Given
            when(request.getRequestURI()).thenReturn("/health");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verify(rateLimiterRegistry, never()).rateLimiter(anyString());
        }

        @Test
        @DisplayName("Should bypass rate limiting for root path")
        void shouldBypassRateLimitingForRootPath() throws ServletException, IOException {
            // Given
            when(request.getRequestURI()).thenReturn("/");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verify(rateLimiterRegistry, never()).rateLimiter(anyString());
        }

        @Test
        @DisplayName("Should bypass rate limiting when path contains health (even partial match)")
        void shouldBypassRateLimitingForPathContainingHealth() throws ServletException, IOException {
            // Given - Note: /api/healthcare contains "/health" so it will be bypassed
            when(request.getRequestURI()).thenReturn("/api/healthcare");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then - Should bypass due to contains("/health") check
            verify(filterChain).doFilter(request, response);
            verify(rateLimiterRegistry, never()).rateLimiter(anyString());
            verify(rateLimiter, never()).acquirePermission();
        }
    }
}
