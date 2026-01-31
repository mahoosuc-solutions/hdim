package com.healthdata.quality.config;

import com.healthdata.authentication.filter.TrustedHeaderAuthFilter;
import com.healthdata.authentication.filter.UserAutoRegistrationFilter;
import com.healthdata.authentication.security.TrustedTenantAccessFilter;
import com.healthdata.quality.persistence.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
 * Quality Measure Service Security Configuration
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
 * This prevents CVE-INTERNAL-2025-001 (Complete Bypass of Tenant Isolation)
 *
 * @see TrustedHeaderAuthFilter
 * @see TrustedTenantAccessFilter
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class QualityMeasureSecurityConfig {

    @Value("${gateway.auth.signing-secret:}")
    private String signingSecret;

    @Value("${gateway.auth.dev-mode:false}")
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
     * Creates the UserAutoRegistrationFilter bean.
     *
     * Automatically registers users in the service database on first access.
     * Extracts user information from gateway-validated headers and creates user records.
     * Updates last_login_at on subsequent access. Audit logs all user registrations.
     */
    @Bean
    @Profile("!test")
    @ConditionalOnBean(UserRepository.class)
    public UserAutoRegistrationFilter userAutoRegistrationFilter(UserRepository userRepository) {
        return new UserAutoRegistrationFilter(userRepository);
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
            )
            .anonymous(anonymous -> anonymous.authorities("ROLE_MEASURE_DEVELOPER", "ROLE_EVALUATOR", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"));

        return http.build();
    }

    /**
     * Production security filter chain for docker/dev/prod profiles.
     * Uses gateway-trust authentication with stateless sessions.
     *
     * HIPAA Security Rule Compliance:
     * - §164.312(d) - Person or Entity Authentication
     * - §164.312(a)(1) - Access Control
     * - §164.308(a)(4)(i) - Isolate Healthcare Clearinghouse Functions
     *
     * Public endpoints: Health checks, API documentation
     * Protected endpoints: All endpoints require gateway-authenticated headers
     *
     * SECURITY: This service trusts gateway-injected X-Auth-* headers.
     * It does NOT validate JWT tokens directly - that's the gateway's job.
     *
     * WebSocket Security:
     * - WebSocket upgrade requests (/ws/**) are permitted at HTTP level
     * - Authentication is enforced by JwtWebSocketHandshakeInterceptor during WebSocket handshake
     * - Authorization is enforced by TenantAccessInterceptor
     * - All connections audited by AuditLoggingInterceptor
     */
    @Bean
    @Profile("!test")
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            TrustedHeaderAuthFilter trustedHeaderAuthFilter,
            ObjectProvider<UserAutoRegistrationFilter> userAutoRegistrationFilterProvider,
            TrustedTenantAccessFilter trustedTenantAccessFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers(
                    "/actuator/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()

                // WebSocket endpoints - authentication handled by WebSocket interceptors
                // HTTP upgrade request must be permitted, security enforced in handshake
                .requestMatchers("/ws/**").permitAll()

                // All API endpoints require authentication (HIPAA §164.312(d))
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // TrustedHeaderAuthFilter extracts user context from gateway headers
            .addFilterBefore(trustedHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // USER MANAGEMENT: Add auto-registration filter AFTER header authentication
        // This ensures users are automatically registered in service database on first access
        // Extracts user info from validated gateway headers, creates user record if doesn't exist
        UserAutoRegistrationFilter userAutoRegistrationFilter =
            userAutoRegistrationFilterProvider.getIfAvailable();
        if (userAutoRegistrationFilter != null) {
            http.addFilterAfter(userAutoRegistrationFilter, TrustedHeaderAuthFilter.class);
        }

        // CRITICAL SECURITY: Add tenant access filter AFTER user auto-registration
        // This ensures tenant isolation is enforced for all authenticated requests
        // Uses request attributes (no database lookup)
        if (userAutoRegistrationFilter != null) {
            http.addFilterAfter(trustedTenantAccessFilter, UserAutoRegistrationFilter.class);
        } else {
            http.addFilterAfter(trustedTenantAccessFilter, TrustedHeaderAuthFilter.class);
        }

        return http.build();
    }
}
