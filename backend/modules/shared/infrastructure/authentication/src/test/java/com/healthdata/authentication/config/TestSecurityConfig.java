package com.healthdata.authentication.config;

import com.healthdata.authentication.service.JwtTokenService;
import com.healthdata.authentication.config.JwtConfig;
import com.healthdata.cache.CacheEvictionService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration for authentication integration tests.
 *
 * This configuration provides a minimal security setup for testing authentication endpoints
 * while allowing test-specific security requirements.
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    /**
     * Configure security for test environment.
     * Permits all requests to authentication endpoints for testing.
     */
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/users/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * Authentication manager for test environment.
     * Required by AuthController for authentication.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authProvider);
    }

    /**
     * Password encoder for test environment.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cache manager for test environment.
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("users", "tokens", "sessions", "tenants");
    }

    /**
     * Cache eviction service for test environment.
     */
    @Bean
    public CacheEvictionService cacheEvictionService(CacheManager cacheManager) {
        return new CacheEvictionService(cacheManager);
    }

    /**
     * JWT configuration for test environment.
     */
    @Bean
    public JwtConfig jwtConfig() {
        JwtConfig config = new JwtConfig();
        config.setSecret("test-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm-and-more-padding");
        config.setAccessTokenExpiration(Duration.ofHours(1));
        config.setRefreshTokenExpiration(Duration.ofDays(7));
        config.setIssuer("healthdata-test");
        config.setAudience("healthdata-test-api");
        return config;
    }

    /**
     * JWT token service for test environment.
     */
    @Bean
    public JwtTokenService jwtTokenService(JwtConfig jwtConfig) {
        return new JwtTokenService(jwtConfig);
    }
}
