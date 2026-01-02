package com.healthdata.gateway.ratelimit;

import com.healthdata.gateway.ratelimit.TenantRateLimitService.EndpointType;
import com.healthdata.gateway.ratelimit.TenantRateLimitService.RateLimitResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TenantRateLimitFilter.
 * Tests per-tenant rate limiting with differentiated limits based on tenant tier.
 *
 * Security Critical: These tests verify rate limiting enforcement to prevent
 * abuse and ensure fair resource allocation across tenants.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tenant Rate Limit Filter Tests")
class TenantRateLimitFilterTest {

    @Mock
    private TenantRateLimitService rateLimitService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TenantRateLimitFilter filter;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        responseWriter = new StringWriter();
    }

    private void setupResponseWriter() throws IOException {
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Nested
    @DisplayName("Filter Behavior")
    class FilterBehavior {

        @Test
        @DisplayName("Should allow request when rate limit not exceeded")
        void shouldAllowRequestWhenRateLimitNotExceeded() throws ServletException, IOException {
            // Given
            String path = "/api/patients";
            String tenantId = "tenant-123";
            String userId = "user-456";

            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeader("X-Tenant-ID")).thenReturn(tenantId);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn(userId);

            RateLimitResult userResult = RateLimitResult.allowed(95, 100);
            RateLimitResult tenantResult = RateLimitResult.allowed(950, 1000);

            when(rateLimitService.determineEndpointType("GET", path)).thenReturn(EndpointType.READ);
            when(rateLimitService.tryConsume(tenantId, userId, EndpointType.READ)).thenReturn(userResult);
            when(rateLimitService.tryConsumeTenantAggregate(tenantId)).thenReturn(tenantResult);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verify(response).setHeader("X-RateLimit-Remaining", "95");
            verify(response).setHeader("X-RateLimit-Limit", "100");
            verify(response).setHeader(eq("X-RateLimit-Reset"), anyString());
            verify(rateLimitService).tryConsume(tenantId, userId, EndpointType.READ);
            verify(rateLimitService).tryConsumeTenantAggregate(tenantId);
        }

        @Test
        @DisplayName("Should return 429 when user rate limit exceeded")
        void shouldReturn429WhenUserRateLimitExceeded() throws ServletException, IOException {
            // Given
            String path = "/api/patients";
            String tenantId = "tenant-123";
            String userId = "user-456";

            setupResponseWriter();
            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeader("X-Tenant-ID")).thenReturn(tenantId);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn(userId);

            RateLimitResult userResult = RateLimitResult.rejected(30, 100);

            when(rateLimitService.determineEndpointType("GET", path)).thenReturn(EndpointType.READ);
            when(rateLimitService.tryConsume(tenantId, userId, EndpointType.READ)).thenReturn(userResult);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(429);
            verify(response).setContentType("application/json");
            verify(response).setHeader("Retry-After", "30");
            verify(response).setHeader("X-RateLimit-Remaining", "0");
            verify(response).setHeader("X-RateLimit-Limit", "100");
            verify(filterChain, never()).doFilter(any(), any());

            String responseBody = responseWriter.toString();
            assertThat(responseBody).contains("rate_limit_exceeded");
            assertThat(responseBody).contains("user rate limit exceeded");
            assertThat(responseBody).contains("\"retry_after\":30");
            assertThat(responseBody).contains("\"limit\":100");
        }

        @Test
        @DisplayName("Should return 429 when tenant aggregate rate limit exceeded")
        void shouldReturn429WhenTenantRateLimitExceeded() throws ServletException, IOException {
            // Given
            String path = "/api/patients";
            String tenantId = "tenant-123";
            String userId = "user-456";

            setupResponseWriter();
            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeader("X-Tenant-ID")).thenReturn(tenantId);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn(userId);

            RateLimitResult userResult = RateLimitResult.allowed(95, 100);
            RateLimitResult tenantResult = RateLimitResult.rejected(60, 1000);

            when(rateLimitService.determineEndpointType("GET", path)).thenReturn(EndpointType.READ);
            when(rateLimitService.tryConsume(tenantId, userId, EndpointType.READ)).thenReturn(userResult);
            when(rateLimitService.tryConsumeTenantAggregate(tenantId)).thenReturn(tenantResult);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(429);
            verify(response).setContentType("application/json");
            verify(response).setHeader("Retry-After", "60");
            verify(response).setHeader("X-RateLimit-Remaining", "0");
            verify(response).setHeader("X-RateLimit-Limit", "1000");
            verify(filterChain, never()).doFilter(any(), any());

            String responseBody = responseWriter.toString();
            assertThat(responseBody).contains("rate_limit_exceeded");
            assertThat(responseBody).contains("tenant rate limit exceeded");
        }

        @Test
        @DisplayName("Should bypass health check endpoints")
        void shouldBypassHealthCheckEndpoints() throws ServletException, IOException {
            // Given
            String[] healthPaths = {
                "/actuator/health",
                "/actuator/info",
                "/actuator/prometheus",
                "/health",
                "/ready",
                "/live"
            };

            for (String path : healthPaths) {
                reset(request, filterChain);
                when(request.getRequestURI()).thenReturn(path);

                // When
                filter.doFilterInternal(request, response, filterChain);

                // Then
                verify(filterChain).doFilter(request, response);
                verify(rateLimitService, never()).determineEndpointType(anyString(), anyString());
                verify(rateLimitService, never()).tryConsume(anyString(), anyString(), any());
            }
        }

        @Test
        @DisplayName("Should add rate limit headers to successful response")
        void shouldAddRateLimitHeaders() throws ServletException, IOException {
            // Given
            String path = "/api/patients";
            String tenantId = "tenant-123";
            String userId = "user-456";

            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("POST");
            when(request.getHeader("X-Tenant-ID")).thenReturn(tenantId);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn(userId);

            RateLimitResult userResult = RateLimitResult.allowed(45, 50);
            RateLimitResult tenantResult = RateLimitResult.allowed(450, 500);

            when(rateLimitService.determineEndpointType("POST", path)).thenReturn(EndpointType.WRITE);
            when(rateLimitService.tryConsume(tenantId, userId, EndpointType.WRITE)).thenReturn(userResult);
            when(rateLimitService.tryConsumeTenantAggregate(tenantId)).thenReturn(tenantResult);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setHeader("X-RateLimit-Remaining", "45");
            verify(response).setHeader("X-RateLimit-Limit", "50");
            verify(response).setHeader(eq("X-RateLimit-Reset"), anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should skip tenant aggregate check when tenant ID is null")
        void shouldSkipTenantAggregateWhenTenantIsNull() throws ServletException, IOException {
            // Given
            String path = "/api/patients";
            String userId = "user-456";

            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeader("X-Tenant-ID")).thenReturn(null);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn(userId);

            RateLimitResult userResult = RateLimitResult.allowed(95, 100);

            when(rateLimitService.determineEndpointType("GET", path)).thenReturn(EndpointType.READ);
            when(rateLimitService.tryConsume(null, userId, EndpointType.READ)).thenReturn(userResult);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimitService).tryConsume(null, userId, EndpointType.READ);
            verify(rateLimitService, never()).tryConsumeTenantAggregate(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should skip tenant aggregate check when tenant ID is empty")
        void shouldSkipTenantAggregateWhenTenantIsEmpty() throws ServletException, IOException {
            // Given
            String path = "/api/patients";
            String userId = "user-456";
            String emptyTenantId = "";

            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeader("X-Tenant-ID")).thenReturn(emptyTenantId);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn(userId);
            // When header is empty, code checks JWT and falls through to null

            RateLimitResult userResult = RateLimitResult.allowed(95, 100);

            when(rateLimitService.determineEndpointType("GET", path)).thenReturn(EndpointType.READ);
            // Empty header results in null tenantId after extraction
            when(rateLimitService.tryConsume(isNull(), eq(userId), eq(EndpointType.READ))).thenReturn(userResult);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimitService).tryConsume(isNull(), eq(userId), eq(EndpointType.READ));
            verify(rateLimitService, never()).tryConsumeTenantAggregate(anyString());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Tenant Extraction")
    class TenantExtraction {

        @Test
        @DisplayName("Should extract tenant from header")
        void shouldExtractTenantFromHeader() throws ServletException, IOException {
            // Given
            String path = "/api/patients";
            String tenantId = "tenant-from-header";
            String userId = "user-456";

            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeader("X-Tenant-ID")).thenReturn(tenantId);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn(userId);

            RateLimitResult userResult = RateLimitResult.allowed(95, 100);
            RateLimitResult tenantResult = RateLimitResult.allowed(950, 1000);

            when(rateLimitService.determineEndpointType("GET", path)).thenReturn(EndpointType.READ);
            when(rateLimitService.tryConsume(tenantId, userId, EndpointType.READ)).thenReturn(userResult);
            when(rateLimitService.tryConsumeTenantAggregate(tenantId)).thenReturn(tenantResult);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimitService).tryConsume(tenantId, userId, EndpointType.READ);
            verify(rateLimitService).tryConsumeTenantAggregate(tenantId);
        }

        @Test
        @DisplayName("Should extract tenant from JWT claim when header not present")
        void shouldExtractTenantFromJwtClaim() throws ServletException, IOException {
            // Given
            String path = "/api/patients";
            String tenantId = "tenant-from-jwt";
            String userId = "user-456";

            Map<String, Object> authDetails = new HashMap<>();
            authDetails.put("tenant_id", tenantId);

            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeader("X-Tenant-ID")).thenReturn(null);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn(userId);
            when(authentication.getDetails()).thenReturn(authDetails);

            RateLimitResult userResult = RateLimitResult.allowed(95, 100);
            RateLimitResult tenantResult = RateLimitResult.allowed(950, 1000);

            when(rateLimitService.determineEndpointType("GET", path)).thenReturn(EndpointType.READ);
            when(rateLimitService.tryConsume(tenantId, userId, EndpointType.READ)).thenReturn(userResult);
            when(rateLimitService.tryConsumeTenantAggregate(tenantId)).thenReturn(tenantResult);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimitService).tryConsume(tenantId, userId, EndpointType.READ);
            verify(rateLimitService).tryConsumeTenantAggregate(tenantId);
        }

        @Test
        @DisplayName("Should prefer header over JWT claim")
        void shouldPreferHeaderOverJwtClaim() throws ServletException, IOException {
            // Given
            String path = "/api/patients";
            String tenantIdFromHeader = "tenant-from-header";
            String userId = "user-456";

            // Note: When header is present, authentication.getDetails() is never called
            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeader("X-Tenant-ID")).thenReturn(tenantIdFromHeader);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn(userId);

            RateLimitResult userResult = RateLimitResult.allowed(95, 100);
            RateLimitResult tenantResult = RateLimitResult.allowed(950, 1000);

            when(rateLimitService.determineEndpointType("GET", path)).thenReturn(EndpointType.READ);
            when(rateLimitService.tryConsume(tenantIdFromHeader, userId, EndpointType.READ)).thenReturn(userResult);
            when(rateLimitService.tryConsumeTenantAggregate(tenantIdFromHeader)).thenReturn(tenantResult);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimitService).tryConsume(tenantIdFromHeader, userId, EndpointType.READ);
            verify(rateLimitService).tryConsumeTenantAggregate(tenantIdFromHeader);
        }

        @Test
        @DisplayName("Should use null tenant for anonymous requests")
        void shouldUseNullForAnonymousRequests() throws ServletException, IOException {
            // Given
            String path = "/api/public";

            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeader("X-Tenant-ID")).thenReturn(null);
            when(securityContext.getAuthentication()).thenReturn(null);

            RateLimitResult userResult = RateLimitResult.allowed(5, 10);

            when(rateLimitService.determineEndpointType("GET", path)).thenReturn(EndpointType.READ);
            when(rateLimitService.tryConsume(null, null, EndpointType.READ)).thenReturn(userResult);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimitService).tryConsume(null, null, EndpointType.READ);
            verify(rateLimitService, never()).tryConsumeTenantAggregate(anyString());
        }
    }

    @Nested
    @DisplayName("User Extraction")
    class UserExtraction {

        @Test
        @DisplayName("Should extract user from security context")
        void shouldExtractUserFromSecurityContext() throws ServletException, IOException {
            // Given
            String path = "/api/patients";
            String tenantId = "tenant-123";
            String userId = "authenticated-user";

            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeader("X-Tenant-ID")).thenReturn(tenantId);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn(userId);

            RateLimitResult userResult = RateLimitResult.allowed(95, 100);
            RateLimitResult tenantResult = RateLimitResult.allowed(950, 1000);

            when(rateLimitService.determineEndpointType("GET", path)).thenReturn(EndpointType.READ);
            when(rateLimitService.tryConsume(tenantId, userId, EndpointType.READ)).thenReturn(userResult);
            when(rateLimitService.tryConsumeTenantAggregate(tenantId)).thenReturn(tenantResult);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimitService).tryConsume(tenantId, userId, EndpointType.READ);
            verify(authentication).getName();
        }

        @Test
        @DisplayName("Should handle missing authentication")
        void shouldHandleMissingAuthentication() throws ServletException, IOException {
            // Given
            String path = "/api/public";
            String tenantId = "tenant-123";

            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeader("X-Tenant-ID")).thenReturn(tenantId);
            when(securityContext.getAuthentication()).thenReturn(null);

            RateLimitResult userResult = RateLimitResult.allowed(5, 10);
            RateLimitResult tenantResult = RateLimitResult.allowed(50, 100);

            when(rateLimitService.determineEndpointType("GET", path)).thenReturn(EndpointType.READ);
            when(rateLimitService.tryConsume(tenantId, null, EndpointType.READ)).thenReturn(userResult);
            when(rateLimitService.tryConsumeTenantAggregate(tenantId)).thenReturn(tenantResult);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimitService).tryConsume(tenantId, null, EndpointType.READ);
        }

        @Test
        @DisplayName("Should handle unauthenticated user")
        void shouldHandleUnauthenticatedUser() throws ServletException, IOException {
            // Given
            String path = "/api/public";
            String tenantId = "tenant-123";

            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeader("X-Tenant-ID")).thenReturn(tenantId);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(false);

            RateLimitResult userResult = RateLimitResult.allowed(5, 10);
            RateLimitResult tenantResult = RateLimitResult.allowed(50, 100);

            when(rateLimitService.determineEndpointType("GET", path)).thenReturn(EndpointType.READ);
            when(rateLimitService.tryConsume(tenantId, null, EndpointType.READ)).thenReturn(userResult);
            when(rateLimitService.tryConsumeTenantAggregate(tenantId)).thenReturn(tenantResult);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimitService).tryConsume(tenantId, null, EndpointType.READ);
            verify(authentication, never()).getName();
        }
    }
}
