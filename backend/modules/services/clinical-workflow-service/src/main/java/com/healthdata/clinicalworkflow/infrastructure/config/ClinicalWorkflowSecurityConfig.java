package com.healthdata.clinicalworkflow.infrastructure.config;

import com.healthdata.authentication.filter.TrustedHeaderAuthFilter;
import com.healthdata.authentication.security.TrustedTenantAccessFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Clinical Workflow Service Security Configuration
 *
 * Implements Gateway-Trust Authentication Pattern:
 * - Gateway (Kong) validates JWT and injects X-Auth-* headers
 * - This service trusts those headers without re-validating JWT
 * - No direct database lookups for user validation
 * - TrustedTenantAccessFilter enforces multi-tenant isolation
 *
 * CRITICAL: Do NOT use JwtAuthenticationFilter + TenantAccessFilter (DB lookups)
 *
 * @see com.healthdata.authentication.security.TrustedHeaderAuthFilter
 * @see com.healthdata.authentication.security.TrustedTenantAccessFilter
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class ClinicalWorkflowSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            TrustedHeaderAuthFilter trustedHeaderAuthFilter,
            TrustedTenantAccessFilter trustedTenantAccessFilter) throws Exception {

        http
            // Disable CSRF for REST API (handled by gateway)
            .csrf().disable()

            // Use gateway-trust authentication (not stateful sessions)
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()

            // Add authentication filters (gateway trust pattern)
            .addFilterBefore(trustedHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(trustedTenantAccessFilter, TrustedHeaderAuthFilter.class)

            // CORS configuration for frontends
            .cors()
                .and()

            // Authorization rules
            .authorizeHttpRequests()
                // Health check endpoints (no auth required)
                .requestMatchers("/actuator/health", "/actuator/health/liveness").permitAll()
                // API documentation (no auth required)
                .requestMatchers("/swagger-ui**", "/v3/api-docs**", "/api-docs**").permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
                .and()

            // Exception handling
            .exceptionHandling();

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",  // Frontend dev server
            "http://localhost:4201",  // Alternative frontend port
            "http://localhost:4202"   // Demo server
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
