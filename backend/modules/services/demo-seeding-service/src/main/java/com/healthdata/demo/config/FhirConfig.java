package com.healthdata.demo.config;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * FHIR configuration for the demo seeding service.
 *
 * Provides:
 * - Singleton FhirContext for FHIR R4 resources
 * - RestTemplate for calling downstream services
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

    /**
     * Creates a RestTemplate for calling downstream services (FHIR, Care Gap, etc.).
     *
     * Configured with reasonable timeouts for batch operations.
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(30000);    // 30 seconds
        return new RestTemplate(factory);
    }
}
