package com.healthdata.cms.config;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FHIR Configuration
 *
 * Provides FhirContext bean for FHIR R4 resource parsing and serialization.
 */
@Configuration
public class FhirConfig {

    /**
     * FhirContext for FHIR R4 parsing
     *
     * Note: FhirContext is thread-safe and expensive to create,
     * so it should be a singleton bean.
     */
    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }
}
