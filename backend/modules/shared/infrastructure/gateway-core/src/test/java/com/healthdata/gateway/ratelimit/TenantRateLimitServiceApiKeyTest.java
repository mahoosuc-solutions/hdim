package com.healthdata.gateway.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TenantRateLimitService API Key Limits")
class TenantRateLimitServiceApiKeyTest {

    @Test
    @DisplayName("Should enforce per-minute API key limit")
    void shouldEnforceApiKeyRateLimit() {
        TenantRateLimitService service = new TenantRateLimitService();
        ReflectionTestUtils.setField(service, "rateLimitEnabled", true);

        TenantRateLimitService.RateLimitResult first = service.tryConsumeApiKey("key-123", 2);
        TenantRateLimitService.RateLimitResult second = service.tryConsumeApiKey("key-123", 2);
        TenantRateLimitService.RateLimitResult third = service.tryConsumeApiKey("key-123", 2);

        assertThat(first.isAllowed()).isTrue();
        assertThat(second.isAllowed()).isTrue();
        assertThat(third.isAllowed()).isFalse();
        assertThat(third.getLimit()).isEqualTo(2);
        assertThat(third.getRetryAfterSeconds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should not enforce API key limit when disabled")
    void shouldBypassWhenDisabled() {
        TenantRateLimitService service = new TenantRateLimitService();
        ReflectionTestUtils.setField(service, "rateLimitEnabled", false);

        TenantRateLimitService.RateLimitResult result = service.tryConsumeApiKey("key-123", 1);

        assertThat(result.isAllowed()).isTrue();
    }
}
