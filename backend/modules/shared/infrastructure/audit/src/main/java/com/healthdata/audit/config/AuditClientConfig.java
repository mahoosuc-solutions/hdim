package com.healthdata.audit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for audit module HTTP clients.
 * 
 * Provides RestTemplate and ObjectMapper beans for agent runtime client integration.
 * These beans are optional - services using the audit module can provide their own.
 */
@Configuration
public class AuditClientConfig {

    /**
     * RestTemplate bean for HTTP client operations.
     * Only created if not already provided by the service.
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        return new RestTemplate(factory);
    }

    /**
     * ObjectMapper bean for JSON processing.
     * Only created if not already provided by the service.
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
