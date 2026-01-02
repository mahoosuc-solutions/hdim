package com.healthdata.quality.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate Configuration
 *
 * Provides RestTemplate bean for making HTTP requests to external services (e.g., FHIR server)
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Create RestTemplate bean for HTTP client operations
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
