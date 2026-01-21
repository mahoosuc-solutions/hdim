package com.healthdata.gateway.service;

import com.healthdata.gateway.config.RateLimitConfiguration;
import com.healthdata.gateway.dto.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Test Suite for RateLimitService (Phase 2.0 Team 1)
 *
 * Tests cover:
 * - Basic rate limit functionality
 * - Role-based multipliers
 * - Tenant-specific overrides
 * - Endpoint-specific limits
 * - Redis behavior
 * - Fail-open strategy
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService Tests")
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private RateLimitConfiguration config;

    @InjectMocks
    private RateLimitService rateLimitService;

    private static final String CLIENT_ID = "user:test-user-123";
    private static final String ENDPOINT = "/api/v1/patients/123";
    private static final String TENANT_ID = "tenant-001";
    private static final int BASE_LIMIT = 1000;

    @BeforeEach
    void setup() {
        // Default configuration setup
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        RateLimitConfiguration.EndpointRateLimit endpointConfig =
            RateLimitConfiguration.EndpointRateLimit.builder()
                .path(ENDPOINT)
                .limitPerMinute(BASE_LIMIT)
                .description("Test endpoint")
                .build();

        when(config.getConfigForEndpoint(ENDPOINT))
            .thenReturn(endpointConfig);
        when(config.hasTenantOverride(TENANT_ID))
            .thenReturn(false);
    }

    @Test
    @DisplayName("Should allow first request within limit")
    void testFirstRequestAllowed() {
        when(valueOps.increment(anyString()))
            .thenReturn(1L);
        when(redisTemplate.expire(anyString(), any(Duration.class)))
            .thenReturn(true);

        RateLimitResult result = rateLimitService.checkLimit(
            CLIENT_ID, ENDPOINT, TENANT_ID);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getCurrent()).isEqualTo(1);
        assertThat(result.getRemaining()).isEqualTo(BASE_LIMIT - 1);
        assertThat(result.getRetryAfterSeconds()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should allow requests below limit")
    void testRequestsBelowLimitAllowed() {
        // Simulate 500 requests already made
        when(valueOps.increment(anyString()))
            .thenReturn(501L);

        RateLimitResult result = rateLimitService.checkLimit(
            CLIENT_ID, ENDPOINT, TENANT_ID);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getCurrent()).isEqualTo(501);
        assertThat(result.getRemaining()).isEqualTo(BASE_LIMIT - 501);
    }

    @Test
    @DisplayName("Should deny request at limit boundary")
    void testRequestAtLimitBoundaryDenied() {
        // Exactly at limit
        when(valueOps.increment(anyString()))
            .thenReturn((long) BASE_LIMIT + 1);

        RateLimitResult result = rateLimitService.checkLimit(
            CLIENT_ID, ENDPOINT, TENANT_ID);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getCurrent()).isEqualTo(BASE_LIMIT + 1);
        assertThat(result.getRemaining()).isEqualTo(0);
        assertThat(result.getRetryAfterSeconds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should deny request over limit")
    void testRequestOverLimitDenied() {
        // Far over limit
        when(valueOps.increment(anyString()))
            .thenReturn((long) BASE_LIMIT + 100);

        RateLimitResult result = rateLimitService.checkLimit(
            CLIENT_ID, ENDPOINT, TENANT_ID);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRemaining()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should apply ADMIN role multiplier (2x)")
    void testAdminRoleMultiplier() {
        // Setup ADMIN authentication
        setupAuthenticationWithRole("ADMIN");
        when(config.getRoleMultiplier("ADMIN"))
            .thenReturn(2.0);

        when(valueOps.increment(anyString()))
            .thenReturn(1501L);  // Over 1000, but under 2000 (2x)

        RateLimitResult result = rateLimitService.checkLimit(
            CLIENT_ID, ENDPOINT, TENANT_ID);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getLimit()).isEqualTo(BASE_LIMIT * 2);
    }

    @Test
    @DisplayName("Should apply VIEWER role multiplier (0.5x)")
    void testViewerRoleMultiplier() {
        // Setup VIEWER authentication
        setupAuthenticationWithRole("VIEWER");
        when(config.getRoleMultiplier("VIEWER"))
            .thenReturn(0.5);

        when(valueOps.increment(anyString()))
            .thenReturn(501L);  // Over 500 (0.5x of 1000)

        RateLimitResult result = rateLimitService.checkLimit(
            CLIENT_ID, ENDPOINT, TENANT_ID);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getLimit()).isEqualTo((int) (BASE_LIMIT * 0.5));
    }

    @Test
    @DisplayName("Should apply SUPER_ADMIN multiplier (10x)")
    void testSuperAdminRoleMultiplier() {
        setupAuthenticationWithRole("SUPER_ADMIN");
        when(config.getRoleMultiplier("SUPER_ADMIN"))
            .thenReturn(10.0);

        when(valueOps.increment(anyString()))
            .thenReturn(5001L);  // Over 1000, but under 10000 (10x)

        RateLimitResult result = rateLimitService.checkLimit(
            CLIENT_ID, ENDPOINT, TENANT_ID);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getLimit()).isEqualTo(BASE_LIMIT * 10);
    }

    @Test
    @DisplayName("Should apply tenant override limit")
    void testTenantOverrideLimitApplied() {
        int tenantLimit = 2000;
        when(config.hasTenantOverride(TENANT_ID))
            .thenReturn(true);
        when(config.getTenantLimit(TENANT_ID))
            .thenReturn(tenantLimit);

        when(valueOps.increment(anyString()))
            .thenReturn(1501L);  // Over 1000, but under 2000

        RateLimitResult result = rateLimitService.checkLimit(
            CLIENT_ID, ENDPOINT, TENANT_ID);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getLimit()).isEqualTo(tenantLimit);
    }

    @Test
    @DisplayName("Should tenant override take precedence over role multiplier")
    void testTenantOverridePrecedesRoleMultiplier() {
        setupAuthenticationWithRole("ADMIN");
        when(config.getRoleMultiplier("ADMIN"))
            .thenReturn(2.0);

        int tenantLimit = 3000;  // Override, ignoring role multiplier
        when(config.hasTenantOverride(TENANT_ID))
            .thenReturn(true);
        when(config.getTenantLimit(TENANT_ID))
            .thenReturn(tenantLimit);

        when(valueOps.increment(anyString()))
            .thenReturn(2001L);

        RateLimitResult result = rateLimitService.checkLimit(
            CLIENT_ID, ENDPOINT, TENANT_ID);

        assertThat(result.getLimit()).isEqualTo(tenantLimit);
        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    @DisplayName("Should set expiration on first request to window")
    void testExpirationSetOnFirstRequest() {
        when(valueOps.increment(anyString()))
            .thenReturn(1L);
        when(redisTemplate.expire(anyString(), any(Duration.class)))
            .thenReturn(true);

        rateLimitService.checkLimit(CLIENT_ID, ENDPOINT, TENANT_ID);

        verify(redisTemplate).expire(anyString(),
            argThat(d -> d.getSeconds() == 61));  // 60s + 1s buffer
    }

    @Test
    @DisplayName("Should not set expiration on subsequent requests")
    void testExpirationNotSetOnSubsequentRequests() {
        when(valueOps.increment(anyString()))
            .thenReturn(5L);  // Not first request

        rateLimitService.checkLimit(CLIENT_ID, ENDPOINT, TENANT_ID);

        verify(redisTemplate, never()).expire(anyString(), any());
    }

    @Test
    @DisplayName("Should generate valid Retry-After header value")
    void testRetryAfterHeaderGeneration() {
        when(valueOps.increment(anyString()))
            .thenReturn((long) BASE_LIMIT + 1);

        RateLimitResult result = rateLimitService.checkLimit(
            CLIENT_ID, ENDPOINT, TENANT_ID);

        assertThat(result.getRetryAfterSeconds())
            .isGreaterThan(0)
            .isLessThanOrEqualTo(60);
    }

    @Test
    @DisplayName("Should return reset time in future")
    void testResetTimeIsInFuture() {
        when(valueOps.increment(anyString()))
            .thenReturn(100L);

        RateLimitResult result = rateLimitService.checkLimit(
            CLIENT_ID, ENDPOINT, TENANT_ID);

        assertThat(result.getResetTime())
            .isGreaterThan(java.time.Instant.now());
    }

    @Test
    @DisplayName("Should handle Redis exception with fail-open strategy")
    void testFailOpenOnRedisException() {
        when(valueOps.increment(anyString()))
            .thenThrow(new RuntimeException("Redis connection failed"));

        RateLimitResult result = rateLimitService.checkLimit(
            CLIENT_ID, ENDPOINT, TENANT_ID);

        // Should fail open - allow request
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(BASE_LIMIT);
    }

    @Test
    @DisplayName("Should handle unauthenticated requests (no role multiplier)")
    void testUnauthenticatedRequestNoMultiplier() {
        SecurityContextHolder.clearContext();

        when(valueOps.increment(anyString()))
            .thenReturn(501L);

        RateLimitResult result = rateLimitService.checkLimit(
            CLIENT_ID, ENDPOINT, TENANT_ID);

        assertThat(result.getLimit()).isEqualTo(BASE_LIMIT);
    }

    @Test
    @DisplayName("Should calculate remaining requests correctly")
    void testRemainingRequestsCalculation() {
        when(valueOps.increment(anyString()))
            .thenReturn(750L);

        RateLimitResult result = rateLimitService.checkLimit(
            CLIENT_ID, ENDPOINT, TENANT_ID);

        assertThat(result.getRemaining())
            .isEqualTo(BASE_LIMIT - 750);
    }

    @Test
    @DisplayName("Should never return negative remaining requests")
    void testNegativeRemainingClamped() {
        when(valueOps.increment(anyString()))
            .thenReturn((long) BASE_LIMIT + 50);

        RateLimitResult result = rateLimitService.checkLimit(
            CLIENT_ID, ENDPOINT, TENANT_ID);

        assertThat(result.getRemaining()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should provide valid header strings")
    void testHeaderStringGeneration() {
        when(valueOps.increment(anyString()))
            .thenReturn(100L);

        RateLimitResult result = rateLimitService.checkLimit(
            CLIENT_ID, ENDPOINT, TENANT_ID);

        assertThat(result.getLimitHeader())
            .isNotEmpty()
            .matches("\\d+");
        assertThat(result.getRemainingHeader())
            .isNotEmpty()
            .matches("\\d+");
        assertThat(result.getResetHeader())
            .isNotEmpty()
            .matches("\\d+");
    }

    // ============ Helper Methods ============

    private void setupAuthenticationWithRole(String role) {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);

        GrantedAuthority authority = mock(GrantedAuthority.class);
        when(authority.getAuthority()).thenReturn("ROLE_" + role);

        Collection<GrantedAuthority> authorities =
            Collections.singletonList(authority);
        when(auth.getAuthorities()).thenReturn(authorities);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);

        SecurityContextHolder.setContext(context);
    }
}
