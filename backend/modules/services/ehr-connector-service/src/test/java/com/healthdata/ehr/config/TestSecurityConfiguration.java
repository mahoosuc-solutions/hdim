package com.healthdata.ehr.config;

import com.healthdata.authentication.service.JwtTokenService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test Security Configuration for EHR Connector Service Tests.
 *
 * This configuration:
 * - Provides mock JwtTokenService to avoid JWT infrastructure
 * - Disables CSRF protection for tests
 * - Permits all requests without authentication
 * - Only active in 'test' profile
 */
@TestConfiguration
@Profile("test")
@EnableWebSecurity
public class TestSecurityConfiguration {

    /**
     * Mock JwtTokenService to avoid needing real JWT infrastructure.
     */
    @MockBean
    private JwtTokenService jwtTokenService;

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
