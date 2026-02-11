package com.healthdata.agentvalidation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for Jaeger API integration.
 * Uses WebClient for reactive HTTP communication with Jaeger's REST API.
 */
@Configuration
public class JaegerIntegrationConfig {

    @Bean("jaegerWebClient")
    public WebClient jaegerWebClient(ValidationProperties properties) {
        return WebClient.builder()
            .baseUrl(properties.getJaeger().getApiUrl())
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(16 * 1024 * 1024)) // 16MB for large traces
            .build();
    }
}
