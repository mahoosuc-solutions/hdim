package com.healthdata.gateway.config;

import com.healthdata.gateway.auth.GatewayAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * Gateway Security Configuration
 *
 * Central authentication and authorization for all backend services.
 * Validates JWT tokens at the gateway and injects trusted headers for downstream services.
 *
 * Security Model:
 * - Gateway validates all incoming JWT tokens
 * - Valid tokens result in X-Auth-* headers being injected
 * - Backend services trust these headers (no re-validation needed)
 * - PublicPathRegistry defines which paths skip authentication
 */
@Slf4j
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class GatewaySecurityConfig {

    private final UserDetailsService userDetailsService;
    private final GatewayAuthProperties authProperties;

    @PostConstruct
    public void validateConfiguration() {
        List<String> errors = authProperties.validateForProduction();
        if (!errors.isEmpty()) {
            errors.forEach(error -> log.warn("Security configuration warning: {}", error));
        }
        log.info("Gateway security initialized: enabled={}, enforced={}",
            authProperties.getEnabled(), authProperties.getEnforced());
        if (Boolean.FALSE.equals(authProperties.getEnforced())) {
            log.info("Gateway demo user tenants: {}", authProperties.getDemoUser().getTenantIds());
        }
    }

    /**
     * Password encoder bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication Manager bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Authentication Provider bean
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * CORS configuration for frontend applications
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",  // Admin Portal
            "http://localhost:4201",  // Admin Portal alt
            "http://localhost:4202"   // Clinical Portal
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
     * Test profile security - permits all requests
     */
    @Bean
    @Profile("test")
    @Order(1)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * Production security filter chain with centralized gateway authentication.
     *
     * Authentication flow:
     * 1. GatewayAuthenticationFilter validates JWT and injects trusted headers
     * 2. Spring Security permits all (actual auth handled by filter)
     * 3. Filter chain continues to route request to backend services
     *
     * The GatewayAuthenticationFilter handles:
     * - Stripping external X-Auth-* headers (security)
     * - JWT validation
     * - Public path checking via PublicPathRegistry
     * - Injecting trusted headers for downstream services
     */
    @Bean
    @Profile("!test")
    @Order(2)
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        GatewayAuthenticationFilter gatewayAuthFilter
    ) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - handled by PublicPathRegistry in GatewayAuthenticationFilter
                .requestMatchers("/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/auth/register").permitAll()
                .requestMatchers("/api/v1/auth/refresh").permitAll()
                .requestMatchers("/api/v1/auth/logout").permitAll()
                .requestMatchers("/api/v1/auth/mfa/verify").permitAll()  // MFA verification
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // All other requests - authentication handled by GatewayAuthenticationFilter
                // The filter validates JWT and returns 401 for protected paths without valid tokens
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // Add gateway auth filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(gatewayAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
