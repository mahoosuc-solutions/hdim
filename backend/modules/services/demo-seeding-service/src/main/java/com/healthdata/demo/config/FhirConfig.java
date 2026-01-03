package com.healthdata.demo.config;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FHIR configuration for the demo seeding service.
 *
 * Provides a singleton FhirContext for FHIR R4 resources.
 */
@Configuration
public class FhirConfig {

    /**
     * Creates a FhirContext for FHIR R4.
     *
     * The FhirContext is thread-safe and should be reused.
     * It is relatively expensive to create, so we create it as a singleton.
     */
    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }
}
