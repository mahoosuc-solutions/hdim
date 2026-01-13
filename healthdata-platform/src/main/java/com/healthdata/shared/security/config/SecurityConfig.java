package com.healthdata.shared.security.config;

import com.healthdata.shared.security.jwt.JwtAuthenticationFilter;
import com.healthdata.shared.security.jwt.JwtTokenProvider;
import com.healthdata.shared.security.tenant.TenantAccessFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration - Configures Spring Security with JWT authentication
 *
 * Features:
 * - JWT-based stateless authentication
 * - CORS configuration for frontend integration
 * - CSRF disabled for API (using JWT instead)
 * - Method-level security with @PreAuthorize and @RolesAllowed
 * - Public and protected endpoint definitions
 * - Session management (stateless)
 *
 * Spring Boot 3.3.5 compatible - Uses Jakarta EE (jakarta.*)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TenantAccessFilter tenantAccessFilter;

    /**
     * Configure HTTP security with JWT authentication
     *
     * - CORS enabled for frontend integration
     * - CSRF disabled (using JWT for API security)
     * - Stateless session management (no server-side session)
     * - JWT filter added before UsernamePasswordAuthenticationFilter
     * - Public and protected endpoint configuration
     *
     * @param http HttpSecurity builder
     * @return Configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Spring Security with JWT authentication");

        http
                // Enable CORS support
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Disable CSRF (using JWT instead)
                .csrf(csrf -> csrf.disable())

                // Configure session management (stateless for JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure endpoint authorization
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints - no authentication required
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        // Patient endpoints - require PATIENT or PROVIDER role
                        .requestMatchers(HttpMethod.GET, "/api/patients/**")
                                .hasAnyRole("PATIENT", "PROVIDER", "ADMIN", "CARE_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/patients/**")
                                .hasAnyRole("PROVIDER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/patients/**")
                                .hasAnyRole("PROVIDER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/patients/**")
                                .hasRole("ADMIN")

                        // FHIR endpoints - require PROVIDER or ADMIN role
                        .requestMatchers("/api/fhir/**")
                                .hasAnyRole("PROVIDER", "ADMIN", "CARE_MANAGER")

                        // Quality measure endpoints - require PROVIDER or ADMIN role
                        .requestMatchers(HttpMethod.GET, "/api/measures/**")
                                .hasAnyRole("PROVIDER", "ADMIN", "CARE_MANAGER", "PATIENT")
                        .requestMatchers(HttpMethod.POST, "/api/measures/**")
                                .hasAnyRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/measures/**")
                                .hasRole("ADMIN")

                        // Care gap endpoints - require PROVIDER or ADMIN role
                        .requestMatchers("/api/care-gaps/**")
                                .hasAnyRole("PROVIDER", "ADMIN", "CARE_MANAGER")
                        .requestMatchers("/api/caregaps/**")
                                .hasAnyRole("PROVIDER", "ADMIN", "CARE_MANAGER")

                        // Notification endpoints - require PATIENT or ADMIN role
                        .requestMatchers("/api/notifications/**")
                                .hasAnyRole("PATIENT", "PROVIDER", "ADMIN")

                        // Reports endpoints - require PROVIDER or ADMIN role
                        .requestMatchers("/api/reports/**")
                                .hasAnyRole("PROVIDER", "ADMIN", "CARE_MANAGER")

                        // Admin endpoints - require ADMIN role
                        .requestMatchers("/api/admin/**")
                                .hasRole("ADMIN")

                        // WebSocket endpoints - require authentication
                        .requestMatchers("/ws/**")
                                .authenticated()

                        // Actuator endpoints - require ADMIN role (except /health)
                        .requestMatchers("/actuator/**")
                                .hasRole("ADMIN")

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                // Exception handling for authentication errors
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                        .accessDeniedHandler(new JwtAccessDeniedHandler())
                );

        // Add JWT authentication filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(tenantAccessFilter, JwtAuthenticationFilter.class);

        log.info("Spring Security configuration completed successfully");
        return http.build();
    }

    /**
     * Configure CORS for frontend integration
     *
     * Allows requests from:
     * - Local development (localhost:3000, localhost:4200)
     * - Production Vercel deployment
     * - Custom origins (configurable via environment)
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Configure allowed origins
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:3000",      // React development
                "http://localhost:4200",      // Angular development
                "http://localhost:8080",      // Backend same-origin
                "https://healthdata-platform.vercel.app",
                "https://healthdata-platform.com",
                "https://*.healthdata.com"    // Production subdomains
        ));

        // Configure allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Configure allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "X-CSRF-Token",
                "X-API-Key"
        ));

        // Configure exposed headers (for client access)
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-Total-Count",
                "X-Page-Number",
                "X-Page-Size"
        ));

        // Allow credentials in CORS requests
        configuration.setAllowCredentials(true);

        // Set max age for preflight cache (1 hour)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.info("CORS configuration enabled");
        return source;
    }

    /**
     * Authentication manager bean for password-based authentication
     *
     * Used by authentication endpoints to authenticate users.
     *
     * @param authConfig AuthenticationConfiguration
     * @return AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Password encoder bean using BCrypt algorithm
     *
     * BCrypt is a widely-used, adaptive hashing algorithm:
     * - Automatically salts passwords
     * - Configurable work factor for future-proofing
     * - Resistant to rainbow table attacks
     *
     * @return BCryptPasswordEncoder with strength 12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
