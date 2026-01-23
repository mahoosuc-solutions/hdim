package com.healthdata.ingestion.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for RestTemplate used to communicate with downstream services.
 *
 * Configures timeouts, error handling, and authentication headers for HTTP clients.
 * Includes LoadTestAuthInterceptor to add mock X-Auth-* headers for service calls.
 */
@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    private final LoadTestAuthInterceptor loadTestAuthInterceptor;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .additionalInterceptors(loadTestAuthInterceptor)
                .build();
    }
}
