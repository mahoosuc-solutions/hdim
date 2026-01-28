package com.healthdata.fhir.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import com.healthdata.authentication.filter.TrustedHeaderAuthFilter;
import com.healthdata.authentication.filter.UserAutoRegistrationFilter;
import com.healthdata.fhir.persistence.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import com.healthdata.authentication.security.TrustedTenantAccessFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Profile;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.core.Ordered;
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
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.cors.CorsConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.cors.CorsConfigurationSource;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Arrays;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * FHIR Service Security Configuration
 *
 * Provides security configuration for different profiles:
 * - Test: Permits all requests without authentication
 * - Docker/Dev/Prod: Gateway-trust authentication via TrustedHeaderAuthFilter
 *
 * SECURITY ARCHITECTURE:
 * This service trusts gateway-injected headers for authentication.
 * The gateway validates JWT tokens and injects X-Auth-* headers with user context.
 * Backend service does NOT re-validate JWT or perform database lookups for users.
 *
 * FHIR-Specific Considerations:
 * - SMART on FHIR OAuth endpoints remain public per spec (/.well-known/smart-configuration, etc.)
 * - FHIR capability statement (/fhir/metadata) remains public for FHIR clients
 * - All FHIR data endpoints require gateway authentication
 * - Tenant isolation enforced via TrustedTenantAccessFilter on all FHIR operations
 *
 * This prevents CVE-INTERNAL-2025-001 (Complete Bypass of Tenant Isolation)
 *
 * @see TrustedHeaderAuthFilter
 * @see TrustedTenantAccessFilter
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class FhirSecurityConfig {

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

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(
            @Qualifier("corsConfigurationSource") CorsConfigurationSource source) {
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * Creates the TrustedHeaderAuthFilter bean.
     * Validates gateway-injected headers for FHIR operations.
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
     * Validates tenant access and enforces isolation on FHIR operations.
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
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
     * Public endpoints: FHIR metadata, SMART on FHIR OAuth, health checks, API documentation
     * Protected endpoints: All FHIR data endpoints require gateway authentication
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
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - health, documentation, FHIR metadata
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/prometheus",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/fhir/metadata",
                    "/metadata"  // FHIR metadata endpoint (capability statement)
                ).permitAll()

                // SMART on FHIR OAuth endpoints (public per spec)
                .requestMatchers(
                    "/.well-known/smart-configuration",
                    "/oauth/authorize",
                    "/oauth/token",
                    "/oauth/revoke",
                    "/oauth/introspect",
                    "/.well-known/jwks.json"
                ).permitAll()

                // All FHIR data endpoints require authentication (HIPAA §164.312(d))
                .anyRequest().authenticated()
            )
            // TrustedHeaderAuthFilter extracts user context from gateway headers
            .addFilterBefore(trustedHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);

        UserAutoRegistrationFilter userAutoRegistrationFilter = userAutoRegistrationFilterProvider.getIfAvailable();
        if (userAutoRegistrationFilter != null) {
            // USER MANAGEMENT: Add auto-registration filter AFTER header authentication
            // This ensures users are automatically registered in service database on first access
            http.addFilterAfter(userAutoRegistrationFilter, TrustedHeaderAuthFilter.class);

            // CRITICAL SECURITY: Add tenant access filter AFTER user auto-registration
            // This ensures tenant isolation is enforced for all authenticated FHIR operations
            http.addFilterAfter(trustedTenantAccessFilter, UserAutoRegistrationFilter.class);
        } else {
            // Fall back to enforcing tenant access after header auth when auto-registration is not available.
            http.addFilterAfter(trustedTenantAccessFilter, TrustedHeaderAuthFilter.class);
        }

        return http.build();
    }
}
