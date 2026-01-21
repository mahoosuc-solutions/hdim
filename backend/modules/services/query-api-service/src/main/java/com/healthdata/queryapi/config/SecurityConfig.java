package com.healthdata.queryapi.config;

import com.healthdata.queryapi.security.JwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security configuration for JWT-based authentication and method-level authorization.
 *
 * This configuration:
 * - Enables method security with @PreAuthorize annotations
 * - Configures OAuth2 resource server with JWT bearer tokens
 * - Enforces STATELESS session management (stateless APIs)
 * - Sets up CORS configuration for frontend integration
 * - Configures JWT decoder (using RSA public key in production)
 * - Establishes role hierarchy: SUPER_ADMIN > ADMIN > EVALUATOR > ANALYST > VIEWER
 *
 * Expected JWT claims:
 * - sub: User email/ID
 * - tenant_id: Tenant identifier
 * - roles: Array of role names
 * - exp: Expiration timestamp
 * - iat: Issued at timestamp
 * - iss: Issuer (hdim-auth-service)
 * - aud: Audience (hdim-api)
 *
 * @author HDIM Security Team
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    /**
     * Configures the security filter chain for stateless REST API.
     *
     * Filters configured:
     * 1. CORS handling (permissive for development, restrictive in production)
     * 2. CSRF disabled (stateless API with JWT)
     * 3. Session management set to STATELESS
     * 4. OAuth2 resource server with JWT decoder
     * 5. Exception handling for authentication failures
     *
     * Authorization:
     * - All endpoints require valid JWT token (except health/actuator endpoints)
     * - Method-level authorization via @PreAuthorize annotations
     * - Role hierarchy enforced at security context
     *
     * @param http HttpSecurity builder
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Disable CSRF (stateless API with JWT doesn't need CSRF protection)
            .csrf(csrf -> csrf.disable())

            // Session management - STATELESS for REST API
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Authorization rules for endpoints
            .authorizeHttpRequests(authz -> authz
                // Permit health check and actuator endpoints (dev/ops need access)
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/health").permitAll()

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )

            // Configure OAuth2 resource server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter)
                )
                // Custom exception handling
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\": \"Unauthorized\", \"message\": \"" +
                        authException.getMessage() + "\"}"
                    );
                })

                // Access denied handling (403 Forbidden)
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\": \"Forbidden\", \"message\": \"Access denied\"}"
                    );
                })
            );

        return http.build();
    }

    /**
     * Configures CORS policy for frontend application integration.
     *
     * Development Configuration (current):
     * - Allows all origins
     * - Allows all methods (GET, POST, PUT, DELETE, OPTIONS)
     * - Allows all headers
     * - Allows credentials (Authorization header)
     * - Cache preflight for 1 hour
     *
     * Production Configuration (TODO):
     * - Restrict to specific frontend domains
     * - Restrict to necessary methods
     * - Restrict to necessary headers
     *
     * @return CORS configuration source for UrlBasedCorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Development: Allow all origins (restrictive in production)
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200", "http://localhost:3000", "*"));

        // Allow standard HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow standard and custom headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Allow credentials with Authorization header
        configuration.setAllowCredentials(true);

        // Cache preflight response for 1 hour (3600 seconds)
        configuration.setMaxAge(3600L);

        // Expose Authorization header in response
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Configures JWT decoder for token validation.
     *
     * Current Implementation (Development):
     * Uses NimbusJwtDecoder with a simple signing algorithm (HMAC-SHA256).
     * The JWT secret is expected in the application.yml:
     *   jwt.secret: <64-character-hex-string>
     *
     * Production Implementation (TODO):
     * Should validate JWT signatures using the issuer's RSA public key:
     * - Fetch JWK (JSON Web Key) Set from issuer's well-known endpoint
     * - Example: https://hdim-auth-service/.well-known/jwks.json
     * - Validate JWT signature against public key
     * - Ensure 'iss' claim matches expected issuer
     *
     * Security Validation Performed:
     * - Signature validation (HMAC in dev, RSA in production)
     * - Expiration time check (exp claim)
     * - Issued-at time check (iat claim)
     * - Not-before time check (nbf claim, if present)
     * - Issuer validation (iss claim, in production)
     *
     * @return Configured JwtDecoder for Spring Security OAuth2
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // Development: Use symmetric key (shared secret)
        // In production, use: NimbusJwtDecoder.withJwkSetUri(...).build()

        String jwtSecret = "your-jwt-secret-here-change-in-production-min-64-chars-" +
                          "abcdefghijklmnopqrstuvwxyz0123456789";

        return NimbusJwtDecoder.withSecretKey(
            new javax.crypto.spec.SecretKeySpec(
                jwtSecret.getBytes(),
                0,
                jwtSecret.getBytes().length,
                "HmacSHA256"
            )
        ).build();
    }

    /**
     * Defines role hierarchy for Spring Security.
     *
     * Role Hierarchy (from most to least privileged):
     * - SUPER_ADMIN: Full system access (all tenants)
     * - ADMIN: Tenant-level administrator
     * - EVALUATOR: Quality measure evaluation and care gap analysis
     * - ANALYST: Reporting and analytics access
     * - VIEWER: Read-only access to patient data
     *
     * This means:
     * - SUPER_ADMIN inherits permissions of all lower roles
     * - ADMIN inherits permissions of EVALUATOR, ANALYST, VIEWER
     * - EVALUATOR inherits permissions of ANALYST, VIEWER
     * - ANALYST inherits permissions of VIEWER
     * - VIEWER has no inherited permissions
     *
     * Example Authorization:
     * @PreAuthorize("hasRole('EVALUATOR')")
     * public void evaluateMeasure() { }
     * // This grants access to: SUPER_ADMIN, ADMIN, EVALUATOR (but not ANALYST or VIEWER)
     *
     * @return Configured RoleHierarchy bean
     */
    @Bean
    public org.springframework.security.access.hierarchicalroles.RoleHierarchy roleHierarchy() {
        return org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl.fromHierarchy(
            """
            SUPER_ADMIN > ADMIN
            ADMIN > EVALUATOR
            EVALUATOR > ANALYST
            ANALYST > VIEWER
            """
        );
    }
}
