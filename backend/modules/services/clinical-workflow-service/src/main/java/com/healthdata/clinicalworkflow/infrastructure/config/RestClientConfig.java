package com.healthdata.clinicalworkflow.infrastructure.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * REST Client Configuration
 *
 * Configures RestTemplate bean for making HTTP requests to other services.
 * Includes timeout configuration and error handling setup.
 */
@Configuration
public class RestClientConfig {

    /**
     * Create RestTemplate bean with sensible defaults
     *
     * Timeouts:
     * - Connect timeout: 5 seconds
     * - Read timeout: 10 seconds
     *
     * @param builder RestTemplate builder
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}
