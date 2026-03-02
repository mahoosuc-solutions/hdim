package com.healthdata.payer.config;

import com.healthdata.authentication.filter.TrustedHeaderAuthFilter;
import com.healthdata.authentication.security.TrustedTenantAccessFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
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

/**
 * Security configuration for Payer Workflows Service.
 *
 * Public endpoints (no auth required):
 * - ROI Calculator: /api/v1/payer/roi/calculate, /api/v1/payer/roi/{id}, /api/v1/payer/roi/{id}/pdf
 * - Health/actuator, Swagger docs
 *
 * Protected endpoints (gateway auth required):
 * - ROI recent list: /api/v1/payer/roi/recent
 * - All pilot/phase2/payer workflow endpoints
 */
@Configuration
@EnableMethodSecurity
public class PayerSecurityConfig {

    @Value("${gateway.auth.signing-secret:}")
    private String signingSecret;

    @Value("${gateway.auth.dev-mode:true}")
    private boolean devMode;

    @Bean
    @Profile("!test & !wave1-local-validation")
    public TrustedHeaderAuthFilter trustedHeaderAuthFilter(MeterRegistry meterRegistry) {
        TrustedHeaderAuthFilter.TrustedHeaderAuthConfig config;
        if (devMode) {
            config = TrustedHeaderAuthFilter.TrustedHeaderAuthConfig.development();
        } else {
            config = TrustedHeaderAuthFilter.TrustedHeaderAuthConfig.production(signingSecret);
        }
        return new TrustedHeaderAuthFilter(config, meterRegistry);
    }

    @Bean
    @Profile("!test & !wave1-local-validation")
    public TrustedTenantAccessFilter trustedTenantAccessFilter(MeterRegistry meterRegistry) {
        return new TrustedTenantAccessFilter(meterRegistry);
    }

    @Bean
    @Profile("test")
    @Order(1)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Profile("!test & !wave1-local-validation")
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(EndpointRequest.toAnyEndpoint())
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Profile("!test & !wave1-local-validation")
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            TrustedHeaderAuthFilter trustedHeaderAuthFilter,
            TrustedTenantAccessFilter trustedTenantAccessFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // Public: health, swagger, API docs
                .requestMatchers(
                    "/actuator/health", "/actuator/health/**",
                    "/actuator/prometheus", "/actuator/info",
                    "/swagger-ui/**", "/v3/api-docs/**",
                    "/swagger-resources/**", "/webjars/**"
                ).permitAll()

                // Public: ROI calculator endpoints for lead capture
                .requestMatchers(
                    "/api/v1/payer/roi/calculate",
                    "/api/v1/payer/roi/recent"   // will be caught by @PreAuthorize
                ).permitAll()
                .requestMatchers("/api/v1/payer/roi/{id}").permitAll()
                .requestMatchers("/api/v1/payer/roi/{id}/pdf").permitAll()

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(trustedHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(trustedTenantAccessFilter, TrustedHeaderAuthFilter.class);

        return http.build();
    }
}
