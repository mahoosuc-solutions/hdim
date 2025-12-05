package com.healthdata.patient.client;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for FHIR Service Feign Client
 *
 * Configures logging, request/response handling, and error handling
 * for communication with the FHIR Service.
 */
@Configuration
public class FhirServiceClientConfiguration {

    /**
     * Configure Feign logging level
     */
    @Bean
    Logger.Level fhirServiceLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Add Accept header for FHIR+JSON content type
     */
    @Bean
    public RequestInterceptor fhirAcceptHeaderInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Accept", "application/fhir+json");
            requestTemplate.header("Content-Type", "application/fhir+json");
        };
    }
}
