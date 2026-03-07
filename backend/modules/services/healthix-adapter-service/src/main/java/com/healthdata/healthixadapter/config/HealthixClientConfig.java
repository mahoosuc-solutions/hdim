package com.healthdata.healthixadapter.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "external.healthix.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class HealthixClientConfig {

    private final HealthixProperties properties;

    @Bean("healthixFhirRestTemplate")
    public RestTemplate healthixFhirRestTemplate(RestTemplateBuilder builder) {
        log.info("Configuring Healthix FHIR client: {}", properties.getFhirUrl());
        return builder
                .rootUri(properties.getFhirUrl())
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .build();
    }

    @Bean("healthixMpiRestTemplate")
    public RestTemplate healthixMpiRestTemplate(RestTemplateBuilder builder) {
        log.info("Configuring Healthix MPI client: {}", properties.getMpiUrl());
        return builder
                .rootUri(properties.getMpiUrl())
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .build();
    }

    @Bean("healthixDocumentRestTemplate")
    public RestTemplate healthixDocumentRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(properties.getDocumentServiceUrl())
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .build();
    }
}
