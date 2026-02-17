package com.healthdata.costanalysis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class CostAnalysisSecurityConfig {

    @Bean
    public TenantHeaderAuthFilter tenantHeaderAuthFilter() {
        return new TenantHeaderAuthFilter();
    }

    @Bean
    @Profile("test")
    @Order(1)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http, TenantHeaderAuthFilter tenantHeaderAuthFilter)
        throws Exception {
        return baseFilterChain(http, tenantHeaderAuthFilter);
    }

    @Bean
    @Profile("!test")
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TenantHeaderAuthFilter tenantHeaderAuthFilter)
        throws Exception {
        return baseFilterChain(http, tenantHeaderAuthFilter);
    }

    private SecurityFilterChain baseFilterChain(HttpSecurity http, TenantHeaderAuthFilter tenantHeaderAuthFilter)
        throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(tenantHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
