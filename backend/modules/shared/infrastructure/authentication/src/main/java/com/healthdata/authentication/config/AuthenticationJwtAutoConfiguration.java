package com.healthdata.authentication.config;

import com.healthdata.authentication.filter.JwtAuthenticationFilter;
import com.healthdata.authentication.service.JwtTokenService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Auto-configuration for JWT validation components only.
 *
 * This configuration is ALWAYS enabled and provides the minimal components needed
 * for microservices to validate JWT tokens issued by the Gateway service:
 * - JwtAuthenticationFilter (validates JWT tokens from Authorization header)
 * - JwtTokenService (validates and parses JWT tokens)
 * - JwtConfig (JWT configuration properties)
 * - PasswordEncoder (BCrypt for token validation)
 *
 * This configuration does NOT include:
 * - AuthController (user login/registration endpoints)
 * - UserRepository (user database access)
 * - RefreshTokenService (token refresh logic)
 * - LogoutService (logout and cache eviction)
 *
 * Those components are loaded by AuthenticationControllerAutoConfiguration
 * and only when UserRepository is available (typically in the Gateway service).
 *
 * ARCHITECTURE:
 * - Gateway Service: Uses BOTH JWT validation and AuthController (full auth stack)
 * - Microservices: Use ONLY JWT validation (this configuration)
 *
 * This separation ensures microservices can validate tokens without requiring
 * the full authentication infrastructure (database, user management, etc.).
 */
@AutoConfiguration
@Import({
    JwtAuthenticationFilter.class,
    JwtTokenService.class,
    JwtConfig.class
})
public class AuthenticationJwtAutoConfiguration {

    /**
     * Provide BCrypt password encoder bean if not already defined.
     * Used by JwtTokenService for token validation.
     */
    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
