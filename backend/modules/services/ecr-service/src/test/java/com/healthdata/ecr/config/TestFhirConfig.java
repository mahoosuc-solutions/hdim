package com.healthdata.ecr.config;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration providing FHIR context for integration tests.
 */
@TestConfiguration
public class TestFhirConfig {

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }
}
