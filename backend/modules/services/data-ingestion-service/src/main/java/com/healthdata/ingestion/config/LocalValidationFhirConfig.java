package com.healthdata.ingestion.config;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Local-only FHIR context bootstrap for Wave-1 validation profile.
 */
@Configuration
@Profile("wave1-local-validation")
public class LocalValidationFhirConfig {

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }
}
