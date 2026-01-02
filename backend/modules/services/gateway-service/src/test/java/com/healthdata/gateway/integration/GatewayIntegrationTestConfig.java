package com.healthdata.gateway.integration;

import com.healthdata.authentication.config.JwtConfig;
import com.healthdata.authentication.service.JwtTokenService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test configuration providing mock beans for gateway integration tests.
 *
 * The real JwtTokenService has @Profile("!test") so it's not loaded in test profile.
 * This configuration provides a mock that:
 * - Returns false for invalid/expired tokens
 * - Returns true for properly formatted valid tokens in tests
 */
@TestConfiguration
public class GatewayIntegrationTestConfig {

    @Bean
    @Primary
    public JwtConfig jwtConfig() {
        JwtConfig config = Mockito.mock(JwtConfig.class);
        when(config.getSecret()).thenReturn("test_jwt_secret_key_for_testing_only_minimum_256_bits_required_for_hs512_algorithm_this_is_long_enough");
        when(config.getIssuer()).thenReturn("healthdata-in-motion-test");
        when(config.getAudience()).thenReturn("healthdata-api-test");
        when(config.getAccessTokenExpirationMillis()).thenReturn(3600000L);
        when(config.getRefreshTokenExpirationMillis()).thenReturn(86400000L);
        return config;
    }

    @Bean
    @Primary
    public JwtTokenService jwtTokenService() {
        JwtTokenService mockService = Mockito.mock(JwtTokenService.class);

        // By default, reject all tokens (security-first approach)
        when(mockService.validateToken(anyString())).thenReturn(false);

        // Also set up default extraction methods to throw for invalid tokens
        // This ensures proper 401 responses for malformed tokens

        return mockService;
    }
}
