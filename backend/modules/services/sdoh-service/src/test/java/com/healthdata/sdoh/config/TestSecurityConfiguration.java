package com.healthdata.sdoh.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test Security Configuration for SDOH Service Tests.
 *
 * This configuration:
 * - Disables CSRF protection for tests
 * - Permits all requests without authentication
 * - Provides mock KafkaTemplate beans
 * - Only active in 'test' profile
 *
 * Usage:
 * Import this configuration in your tests with:
 * @Import(TestSecurityConfiguration.class)
 * OR include it in the @SpringBootTest classes attribute
 */
@TestConfiguration
@Profile("test")
@EnableWebSecurity
public class TestSecurityConfiguration {

    /**
     * Mock KafkaTemplate for String key and Object value.
     * Used by services that publish domain events.
     */
    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplateObject;

    /**
     * Mock KafkaTemplate for String key and String value.
     * Used by some Kafka consumers and producers.
     */
    @MockBean
    private KafkaTemplate<String, String> kafkaTemplateString;

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
