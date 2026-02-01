package com.healthdata.consent.config;

import com.healthdata.authentication.filter.TrustedHeaderAuthFilter;
import io.micrometer.core.instrument.MeterRegistry;
import com.healthdata.authentication.security.TrustedTenantAccessFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Profile;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.core.annotation.Order;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.security.config.http.SessionCreationPolicy;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.security.web.SecurityFilterChain;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.cors.CorsConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.cors.CorsConfigurationSource;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Arrays;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Consent Service Security Configuration
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
 * This prevents CVE-INTERNAL-2025-001 (Complete Bypass of Tenant Isolation)
 *
 * @see TrustedHeaderAuthFilter
 * @see TrustedTenantAccessFilter
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class ConsentSecurityConfig {

    @Value("${gateway.auth.signing-secret:}")
    private String signingSecret;

    @Value("${gateway.auth.dev-mode:false}")
    private boolean devMode;

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
                // Health endpoints
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()

                // All consent endpoints require authentication (HIPAA §164.312(d))
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // TrustedHeaderAuthFilter extracts user context from gateway headers
            .addFilterBefore(trustedHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // CRITICAL SECURITY: Add tenant access filter AFTER header authentication
        // This ensures tenant isolation is enforced for all authenticated requests
        http.addFilterAfter(trustedTenantAccessFilter, TrustedHeaderAuthFilter.class);

        return http.build();
    }
}
