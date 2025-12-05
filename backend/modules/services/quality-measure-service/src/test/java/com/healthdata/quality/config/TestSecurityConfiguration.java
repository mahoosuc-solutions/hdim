package com.healthdata.quality.config;

import com.healthdata.authentication.service.JwtTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test Security Configuration for Quality Measure Service Tests
 *
 * Provides mock implementations of security-related beans to avoid dependencies
 * on external authentication services during testing.
 *
 * Key Features:
 * - Mock JwtTokenService for token validation
 * - All tokens are considered valid in tests
 * - Configurable tenant and user claims
 * - Auto-loaded via @Profile("test") - no need to manually import
 */
@Configuration
@Profile("test")
public class TestSecurityConfiguration {

    /**
     * Mock JwtTokenService bean for tests.
     * All tokens are considered valid, and extraction methods return test values.
     */
    @Bean
    @Primary
    public JwtTokenService jwtTokenService() {
        JwtTokenService mockService = mock(JwtTokenService.class);

        // All tokens are valid in tests
        when(mockService.validateToken(anyString())).thenReturn(true);

        // Token is never expired in tests
        when(mockService.isTokenExpired(anyString())).thenReturn(false);

        // Return test values for extraction methods
        when(mockService.extractUsername(anyString())).thenReturn("test-user");
        when(mockService.extractUserId(anyString())).thenReturn(java.util.UUID.randomUUID());
        when(mockService.extractTenantIds(anyString())).thenReturn(java.util.Set.of("test-tenant"));
        when(mockService.extractRoles(anyString())).thenReturn(java.util.Set.of("USER", "ADMIN"));

        return mockService;
    }
}
