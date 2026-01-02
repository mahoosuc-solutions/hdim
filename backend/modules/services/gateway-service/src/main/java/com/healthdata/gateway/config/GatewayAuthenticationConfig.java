package com.healthdata.gateway.config;

import com.healthdata.authentication.config.JwtConfig;
import com.healthdata.authentication.repository.ApiKeyRepository;
import com.healthdata.authentication.repository.RefreshTokenRepository;
import com.healthdata.authentication.repository.UserRepository;
import com.healthdata.authentication.service.ApiKeyService;
import com.healthdata.authentication.service.JwtTokenService;
import com.healthdata.authentication.service.LogoutService;
import com.healthdata.authentication.service.MfaService;
import com.healthdata.authentication.service.RefreshTokenService;
import com.healthdata.cache.CacheEvictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway-specific authentication configuration.
 *
 * This configuration explicitly creates authentication service beans that require
 * database repositories. These services are NOT auto-scanned in the authentication
 * module to prevent microservices from accidentally loading them.
 *
 * Architecture:
 * - Microservices: Use only JWT validation (JwtTokenService, JwtAuthenticationFilter)
 * - Gateway: Uses full auth stack including these database-dependent services
 *
 * See: /backend/AUTHENTICATION-ARCHITECTURE.md for full documentation
 */
@Configuration
@ConditionalOnProperty(
    name = "authentication.controller.enabled",
    havingValue = "true",
    matchIfMissing = false
)
// Note: @EnableJpaRepositories and @EntityScan are on GatewayApplication to avoid duplicate bean definitions
public class GatewayAuthenticationConfig {

    /**
     * LogoutService for HIPAA-compliant logout with cache eviction.
     * Clears PHI caches when users log out.
     */
    @Bean
    public LogoutService logoutService(
            UserRepository userRepository,
            @Autowired(required = false) CacheEvictionService cacheEvictionService) {
        return new LogoutService(userRepository, cacheEvictionService);
    }

    /**
     * MfaService for multi-factor authentication.
     * Handles TOTP setup, verification, and recovery codes.
     */
    @Bean
    public MfaService mfaService(UserRepository userRepository) {
        return new MfaService(userRepository);
    }

    /**
     * RefreshTokenService for token refresh flows.
     * Stores refresh tokens in database for revocation support.
     */
    @Bean
    public RefreshTokenService refreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            JwtTokenService jwtTokenService,
            JwtConfig jwtConfig) {
        return new RefreshTokenService(refreshTokenRepository, userRepository, jwtTokenService, jwtConfig);
    }

    /**
     * ApiKeyService for API key management.
     * Handles key creation, rotation, and validation.
     */
    @Bean
    public ApiKeyService apiKeyService(ApiKeyRepository apiKeyRepository) {
        return new ApiKeyService(apiKeyRepository);
    }
}
