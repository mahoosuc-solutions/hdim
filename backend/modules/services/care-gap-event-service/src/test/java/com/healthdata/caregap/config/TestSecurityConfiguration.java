package com.healthdata.caregap.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test Security Configuration for Care Gap Event Service Tests.
 *
 * Provides a permits-all filter chain so that existing functional
 * and integration tests continue to pass without needing gateway
 * headers. Security-specific tests (SecurityRegressionTest) may
 * use @WithMockUser or provide mock headers directly.
 *
 * Active only in 'test' profile; replaces the production SecurityConfig
 * which is annotated @Profile("!test").
 */
@TestConfiguration
@Profile("test")
@EnableWebSecurity
public class TestSecurityConfiguration {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
