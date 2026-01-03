package com.healthdata.patient.config;

import com.healthdata.authentication.filter.TrustedHeaderAuthFilter;
import com.healthdata.authentication.security.TrustedTenantAccessFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Patient Service Security Configuration
 *
 * Provides security configuration for different profiles:
 * - Test: Permits all requests without authentication
 * - Docker/Dev/Prod: Gateway-trust authentication via TrustedHeaderAuthFilter
 *
 * SECURITY ARCHITECTURE:
 * This service trusts gateway-injected headers for authentication.
 * The gateway validates JWT tokens and injects X-Auth-* headers with user context.
 * Backend services do NOT re-validate JWT or perform database lookups for users.
 *
 * Flow:
 * 1. Client sends JWT to Gateway
 * 2. Gateway validates JWT, injects X-Auth-* headers with HMAC signature
 * 3. TrustedHeaderAuthFilter validates signature, extracts user context
 * 4. TrustedTenantAccessFilter validates tenant access from attributes
 *
 * Public endpoints: Health checks, API documentation
 * Protected endpoints: All patient CRUD operations require gateway authentication
 *
 * This prevents CVE-INTERNAL-2025-001 (Complete Bypass of Tenant Isolation)
 *
 * @see TrustedHeaderAuthFilter
 * @see TrustedTenantAccessFilter
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class PatientSecurityConfig {

    @Value("${gateway.auth.signing-secret:}")
    private String signingSecret;

    @Value("${gateway.auth.dev-mode:true}")
    private boolean devMode;

    /**
     * CORS configuration for frontend applications.
     * Allows requests from Admin Portal (4200, 4201) and Clinical Portal (4202).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",
            "http://localhost:4201",
            "http://localhost:4202"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Creates the TrustedHeaderAuthFilter bean.
     *
     * In development mode, accepts any gateway signature with valid prefix.
     * In production mode, validates HMAC signature with shared secret.
     */
    @Bean
    @Profile("!test")
    public TrustedHeaderAuthFilter trustedHeaderAuthFilter(MeterRegistry meterRegistry) {
        TrustedHeaderAuthFilter.TrustedHeaderAuthConfig config;

        if (devMode) {
            config = TrustedHeaderAuthFilter.TrustedHeaderAuthConfig.development();
        } else {
            config = TrustedHeaderAuthFilter.TrustedHeaderAuthConfig.production(signingSecret);
        }

        return new TrustedHeaderAuthFilter(config, meterRegistry);
    }

    /**
     * Creates the TrustedTenantAccessFilter bean.
     *
     * Validates tenant access using request attributes (no database lookup).
     */
    @Bean
    @Profile("!test")
    public TrustedTenantAccessFilter trustedTenantAccessFilter(MeterRegistry meterRegistry) {
        return new TrustedTenantAccessFilter(meterRegistry);
    }

    /**
     * Test profile security filter chain.
     * Permits all HTTP requests without authentication for integration testing.
     */
    @Bean
    @Profile("test")
    @Order(1)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }

    /**
     * Production security filter chain for docker/dev/prod profiles.
     * Uses gateway-trust authentication with stateless sessions.
     *
     * SECURITY: This service trusts gateway-injected X-Auth-* headers.
     * It does NOT validate JWT tokens directly - that's the gateway's job.
     *
     * Public endpoints: Health checks (/_health), API documentation
     * Protected endpoints: All patient operations require gateway authentication
     */
    @Bean
    @Profile("!test")
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            TrustedHeaderAuthFilter trustedHeaderAuthFilter,
            TrustedTenantAccessFilter trustedTenantAccessFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/_health",  // Patient service health endpoint
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()

                // All patient data endpoints require authentication (HIPAA §164.312(d))
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // TrustedHeaderAuthFilter extracts user context from gateway headers
            .addFilterBefore(trustedHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // CRITICAL SECURITY: Add tenant access filter AFTER header authentication
        // This ensures tenant isolation is enforced for all authenticated requests
        // Uses request attributes (no database lookup)
        http.addFilterAfter(trustedTenantAccessFilter, TrustedHeaderAuthFilter.class);

        return http.build();
    }
}
