package com.healthdata.patient.config;

import com.healthdata.authentication.filter.JwtAuthenticationFilter;
import com.healthdata.authentication.security.TenantAccessFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
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
 * - Docker/Dev/Prod: JWT-based authentication via JwtAuthenticationFilter
 *
 * Public endpoints: Health checks, API documentation
 * Protected endpoints: All patient CRUD operations require JWT with tenant validation
 *
 * SECURITY: TenantAccessFilter is enabled to enforce multi-tenant isolation.
 * This prevents CVE-INTERNAL-2025-001 (Complete Bypass of Tenant Isolation)
 */
@Configuration
public class PatientSecurityConfig {

    @Autowired(required = false)
    private TenantAccessFilter tenantAccessFilter;

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
     * Uses JWT-based authentication with stateless sessions.
     * 
     * Public endpoints: Health checks, API documentation
     * Protected endpoints: All patient CRUD operations (/api/v1/patients/**) require JWT
     */
    @Bean
    @Profile("!test")
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                // All patient data endpoints require JWT authentication
                .requestMatchers("/api/v1/patients/**", "/api/v1/health/**").authenticated()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // CRITICAL SECURITY: Add tenant access filter AFTER JWT authentication
        // This ensures tenant isolation is enforced for all authenticated requests
        if (tenantAccessFilter != null) {
            http.addFilterAfter(tenantAccessFilter, JwtAuthenticationFilter.class);
        }

        return http.build();
    }
}
