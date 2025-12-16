package com.healthdata.gateway.ratelimit;

import com.healthdata.gateway.ratelimit.TenantRateLimitService.EndpointType;
import com.healthdata.gateway.ratelimit.TenantRateLimitService.RateLimitResult;
import com.healthdata.gateway.ratelimit.TenantRateLimitService.RateLimitTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TenantRateLimitService.
 * Tests rate limiting logic for different tiers, endpoints, and scenarios.
 */
@DisplayName("Tenant Rate Limit Service Tests")
class TenantRateLimitServiceTest {

    private TenantRateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new TenantRateLimitService();
        ReflectionTestUtils.setField(rateLimitService, "rateLimitEnabled", true);
        ReflectionTestUtils.setField(rateLimitService, "useRedis", false);
    }

    @Nested
    @DisplayName("Rate Limit Tier Tests")
    class RateLimitTierTests {

        @Test
        @DisplayName("Should have correct values for ANONYMOUS tier")
        void shouldHaveCorrectAnonymousValues() {
            assertThat(RateLimitTier.ANONYMOUS.getRequestsPerSecond()).isEqualTo(10);
            assertThat(RateLimitTier.ANONYMOUS.getBurstCapacity()).isEqualTo(20);
        }

        @Test
        @DisplayName("Should have correct values for STANDARD tier")
        void shouldHaveCorrectStandardValues() {
            assertThat(RateLimitTier.STANDARD.getRequestsPerSecond()).isEqualTo(100);
            assertThat(RateLimitTier.STANDARD.getBurstCapacity()).isEqualTo(150);
        }

        @Test
        @DisplayName("Should have correct values for PREMIUM tier")
        void shouldHaveCorrectPremiumValues() {
            assertThat(RateLimitTier.PREMIUM.getRequestsPerSecond()).isEqualTo(500);
            assertThat(RateLimitTier.PREMIUM.getBurstCapacity()).isEqualTo(750);
        }

        @Test
        @DisplayName("Should have correct values for ENTERPRISE tier")
        void shouldHaveCorrectEnterpriseValues() {
            assertThat(RateLimitTier.ENTERPRISE.getRequestsPerSecond()).isEqualTo(2000);
            assertThat(RateLimitTier.ENTERPRISE.getBurstCapacity()).isEqualTo(3000);
        }

        @Test
        @DisplayName("Should have correct values for INTERNAL tier")
        void shouldHaveCorrectInternalValues() {
            assertThat(RateLimitTier.INTERNAL.getRequestsPerSecond()).isEqualTo(5000);
            assertThat(RateLimitTier.INTERNAL.getBurstCapacity()).isEqualTo(7500);
        }
    }

    @Nested
    @DisplayName("Endpoint Type Tests")
    class EndpointTypeTests {

        @Test
        @DisplayName("Should have correct multiplier for READ")
        void shouldHaveCorrectReadMultiplier() {
            assertThat(EndpointType.READ.getMultiplier()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should have correct multiplier for WRITE")
        void shouldHaveCorrectWriteMultiplier() {
            assertThat(EndpointType.WRITE.getMultiplier()).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should have correct multiplier for BULK")
        void shouldHaveCorrectBulkMultiplier() {
            assertThat(EndpointType.BULK.getMultiplier()).isEqualTo(0.1);
        }

        @Test
        @DisplayName("Should have correct multiplier for HEALTH")
        void shouldHaveCorrectHealthMultiplier() {
            assertThat(EndpointType.HEALTH.getMultiplier()).isEqualTo(10.0);
        }
    }

    @Nested
    @DisplayName("Determine Endpoint Type Tests")
    class DetermineEndpointTypeTests {

        @Test
        @DisplayName("Should return HEALTH for actuator endpoints")
        void shouldReturnHealthForActuator() {
            assertThat(rateLimitService.determineEndpointType("GET", "/actuator/health"))
                    .isEqualTo(EndpointType.HEALTH);
        }

        @Test
        @DisplayName("Should return HEALTH for health endpoints")
        void shouldReturnHealthForHealthEndpoint() {
            assertThat(rateLimitService.determineEndpointType("GET", "/api/health"))
                    .isEqualTo(EndpointType.HEALTH);
        }

        @Test
        @DisplayName("Should return BULK for bulk endpoints")
        void shouldReturnBulkForBulkEndpoint() {
            assertThat(rateLimitService.determineEndpointType("POST", "/api/bulk/import"))
                    .isEqualTo(EndpointType.BULK);
        }

        @Test
        @DisplayName("Should return BULK for batch endpoints")
        void shouldReturnBulkForBatchEndpoint() {
            assertThat(rateLimitService.determineEndpointType("POST", "/api/batch/process"))
                    .isEqualTo(EndpointType.BULK);
        }

        @Test
        @DisplayName("Should return BULK for export endpoints")
        void shouldReturnBulkForExportEndpoint() {
            assertThat(rateLimitService.determineEndpointType("GET", "/api/export/data"))
                    .isEqualTo(EndpointType.BULK);
        }

        @Test
        @DisplayName("Should return WRITE for POST method")
        void shouldReturnWriteForPost() {
            assertThat(rateLimitService.determineEndpointType("POST", "/api/patients"))
                    .isEqualTo(EndpointType.WRITE);
        }

        @Test
        @DisplayName("Should return WRITE for PUT method")
        void shouldReturnWriteForPut() {
            assertThat(rateLimitService.determineEndpointType("PUT", "/api/patients/123"))
                    .isEqualTo(EndpointType.WRITE);
        }

        @Test
        @DisplayName("Should return WRITE for DELETE method")
        void shouldReturnWriteForDelete() {
            assertThat(rateLimitService.determineEndpointType("DELETE", "/api/patients/123"))
                    .isEqualTo(EndpointType.WRITE);
        }

        @Test
        @DisplayName("Should return WRITE for PATCH method")
        void shouldReturnWriteForPatch() {
            assertThat(rateLimitService.determineEndpointType("PATCH", "/api/patients/123"))
                    .isEqualTo(EndpointType.WRITE);
        }

        @Test
        @DisplayName("Should return READ for GET method")
        void shouldReturnReadForGet() {
            assertThat(rateLimitService.determineEndpointType("GET", "/api/patients"))
                    .isEqualTo(EndpointType.READ);
        }
    }

    @Nested
    @DisplayName("Get Tenant Tier Tests")
    class GetTenantTierTests {

        @Test
        @DisplayName("Should return ANONYMOUS for null tenant")
        void shouldReturnAnonymousForNullTenant() {
            assertThat(rateLimitService.getTenantTier(null))
                    .isEqualTo(RateLimitTier.ANONYMOUS);
        }

        @Test
        @DisplayName("Should return ANONYMOUS for empty tenant")
        void shouldReturnAnonymousForEmptyTenant() {
            assertThat(rateLimitService.getTenantTier(""))
                    .isEqualTo(RateLimitTier.ANONYMOUS);
        }

        @Test
        @DisplayName("Should return STANDARD for unknown tenant")
        void shouldReturnStandardForUnknownTenant() {
            assertThat(rateLimitService.getTenantTier("unknown-tenant"))
                    .isEqualTo(RateLimitTier.STANDARD);
        }

        @Test
        @DisplayName("Should return cached tier after update")
        void shouldReturnCachedTierAfterUpdate() {
            // Given
            String tenantId = "premium-tenant";
            rateLimitService.updateTenantTier(tenantId, RateLimitTier.PREMIUM);

            // When/Then
            assertThat(rateLimitService.getTenantTier(tenantId))
                    .isEqualTo(RateLimitTier.PREMIUM);
        }
    }

    @Nested
    @DisplayName("Update Tenant Tier Tests")
    class UpdateTenantTierTests {

        @Test
        @DisplayName("Should update tenant tier")
        void shouldUpdateTenantTier() {
            // Given
            String tenantId = "tenant-123";

            // When
            rateLimitService.updateTenantTier(tenantId, RateLimitTier.ENTERPRISE);

            // Then
            assertThat(rateLimitService.getTenantTier(tenantId))
                    .isEqualTo(RateLimitTier.ENTERPRISE);
        }

        @Test
        @DisplayName("Should overwrite existing tier")
        void shouldOverwriteExistingTier() {
            // Given
            String tenantId = "tenant-123";
            rateLimitService.updateTenantTier(tenantId, RateLimitTier.PREMIUM);

            // When
            rateLimitService.updateTenantTier(tenantId, RateLimitTier.ENTERPRISE);

            // Then
            assertThat(rateLimitService.getTenantTier(tenantId))
                    .isEqualTo(RateLimitTier.ENTERPRISE);
        }
    }

    @Nested
    @DisplayName("Try Consume Tests")
    class TryConsumeTests {

        @Test
        @DisplayName("Should allow request when rate limit disabled")
        void shouldAllowWhenDisabled() {
            // Given
            ReflectionTestUtils.setField(rateLimitService, "rateLimitEnabled", false);

            // When
            RateLimitResult result = rateLimitService.tryConsume("tenant-1", "user-1", EndpointType.READ);

            // Then
            assertThat(result.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("Should allow first request")
        void shouldAllowFirstRequest() {
            // When
            RateLimitResult result = rateLimitService.tryConsume("tenant-1", "user-1", EndpointType.READ);

            // Then
            assertThat(result.isAllowed()).isTrue();
            assertThat(result.getRemainingTokens()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should allow multiple requests within limit")
        void shouldAllowMultipleRequestsWithinLimit() {
            // Given
            String tenantId = "tenant-1";
            String userId = "user-1";

            // When - Make several requests (less than burst capacity)
            for (int i = 0; i < 10; i++) {
                RateLimitResult result = rateLimitService.tryConsume(tenantId, userId, EndpointType.READ);
                assertThat(result.isAllowed()).isTrue();
            }
        }

        @Test
        @DisplayName("Should reject when exceeding burst capacity")
        void shouldRejectWhenExceedingBurst() {
            // Given - Use ANONYMOUS tier with low limits (20 burst)
            String tenantId = ""; // Empty means ANONYMOUS
            String userId = "user-1";

            // When - Exhaust the burst capacity
            for (int i = 0; i < 25; i++) {
                rateLimitService.tryConsume(tenantId, userId, EndpointType.READ);
            }

            RateLimitResult result = rateLimitService.tryConsume(tenantId, userId, EndpointType.READ);

            // Then
            assertThat(result.isAllowed()).isFalse();
            assertThat(result.getRetryAfterSeconds()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Try Consume Tenant Aggregate Tests")
    class TryConsumeTenantAggregateTests {

        @Test
        @DisplayName("Should allow tenant aggregate request when rate limit disabled")
        void shouldAllowAggregateWhenDisabled() {
            // Given
            ReflectionTestUtils.setField(rateLimitService, "rateLimitEnabled", false);

            // When
            RateLimitResult result = rateLimitService.tryConsumeTenantAggregate("tenant-1");

            // Then
            assertThat(result.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("Should allow first tenant aggregate request")
        void shouldAllowFirstAggregateRequest() {
            // When
            RateLimitResult result = rateLimitService.tryConsumeTenantAggregate("tenant-1");

            // Then
            assertThat(result.isAllowed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Rate Limit Result Tests")
    class RateLimitResultTests {

        @Test
        @DisplayName("Should create allowed result without tokens")
        void shouldCreateAllowedResultWithoutTokens() {
            RateLimitResult result = RateLimitResult.allowed();

            assertThat(result.isAllowed()).isTrue();
            assertThat(result.getRemainingTokens()).isEqualTo(-1);
            assertThat(result.getLimit()).isEqualTo(-1);
            assertThat(result.getRetryAfterSeconds()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should create allowed result with tokens")
        void shouldCreateAllowedResultWithTokens() {
            RateLimitResult result = RateLimitResult.allowed(50, 100);

            assertThat(result.isAllowed()).isTrue();
            assertThat(result.getRemainingTokens()).isEqualTo(50);
            assertThat(result.getLimit()).isEqualTo(100);
            assertThat(result.getRetryAfterSeconds()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should create rejected result")
        void shouldCreateRejectedResult() {
            RateLimitResult result = RateLimitResult.rejected(30, 100);

            assertThat(result.isAllowed()).isFalse();
            assertThat(result.getRemainingTokens()).isEqualTo(0);
            assertThat(result.getLimit()).isEqualTo(100);
            assertThat(result.getRetryAfterSeconds()).isEqualTo(30);
        }
    }
}
