package com.healthdata.ecr.client;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Patient Service Feign Client
 *
 * Configures logging, request/response handling, and error handling
 * for communication with the Patient Service.
 */
@Configuration
public class PatientServiceClientConfiguration {

    /**
     * Configure Feign logging level
     */
    @Bean
    Logger.Level patientServiceLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Add Accept header for FHIR+JSON content type
     */
    @Bean
    public RequestInterceptor patientServiceAcceptHeaderInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Accept", "application/fhir+json");
            requestTemplate.header("Content-Type", "application/json");
        };
    }
}
