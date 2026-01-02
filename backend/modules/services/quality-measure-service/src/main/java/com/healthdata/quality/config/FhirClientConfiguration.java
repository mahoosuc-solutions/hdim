package com.healthdata.quality.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for HAPI FHIR client.
 * Connects to the FHIR server to fetch patient data.
 */
@Configuration
public class FhirClientConfiguration {

    @Value("${fhir.server.url:http://localhost:8085/fhir}")
    private String fhirServerUrl;

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Bean
    public IGenericClient fhirClient(FhirContext fhirContext) {
        return fhirContext.newRestfulGenericClient(fhirServerUrl);
    }
}
