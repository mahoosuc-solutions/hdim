package com.healthdata.agentvalidation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Development security configuration for local testing.
 *
 * WARNING: This configuration should ONLY be active in the 'dev' profile.
 * It allows testing APIs without full gateway authentication by:
 * - Accepting X-User-ID, X-Tenant-ID, X-User-Roles headers directly
 * - Defaulting to ADMIN role if no roles are provided
 *
 * NEVER use this in production!
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("dev")
public class DevSecurityConfig {

    @Bean
    @Primary
    @Order(1)  // High priority - process before other security filter chains
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        log.warn("*** DEV SECURITY CONFIG ACTIVE - NOT FOR PRODUCTION ***");

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // In dev mode, permit all requests - authentication is handled by filter
                .anyRequest().permitAll()
            )
            .addFilterBefore(new DevHeaderAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Simple filter that creates authentication from dev headers.
     * Accepts X-User-ID, X-Tenant-ID, X-User-Roles headers directly.
     */
    static class DevHeaderAuthFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
        ) throws ServletException, IOException {

            String userId = request.getHeader("X-User-ID");
            String tenantId = request.getHeader("X-Tenant-ID");
            String rolesHeader = request.getHeader("X-User-Roles");

            // Default values for local testing
            if (userId == null || userId.isBlank()) {
                userId = "dev-user";
            }
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = "dev-tenant";
            }
            if (rolesHeader == null || rolesHeader.isBlank()) {
                rolesHeader = "ADMIN";  // Default to ADMIN for dev
            }

            // Parse roles
            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

            // Create authentication
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Store tenant for downstream use
            request.setAttribute("tenantId", tenantId);
            request.setAttribute("userId", userId);

            filterChain.doFilter(request, response);
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String path = request.getRequestURI();
            return path.contains("/actuator") ||
                   path.contains("/swagger-ui") ||
                   path.contains("/v3/api-docs");
        }
    }
}
