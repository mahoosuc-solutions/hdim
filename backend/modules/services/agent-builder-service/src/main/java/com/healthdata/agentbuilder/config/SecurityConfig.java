package com.healthdata.agentbuilder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Agent Builder Service.
 *
 * This service uses Pattern 3 (No Auth) - it is accessed through the gateway
 * which handles authentication and passes user identity via X-User-ID and X-Tenant-ID headers.
 *
 * All requests are permitted at the security filter level. Tenant isolation is enforced
 * at the service layer using the X-Tenant-ID header.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (gateway handles this)
            .csrf(AbstractHttpConfigurer::disable)
            // Stateless session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Permit all requests - gateway handles authentication
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll());

        return http.build();
    }
}
