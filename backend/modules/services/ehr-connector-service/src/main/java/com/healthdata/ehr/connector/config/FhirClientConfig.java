package com.healthdata.ehr.connector.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import com.healthdata.ehr.connector.epic.EpicAuthProvider;
import com.healthdata.ehr.connector.epic.EpicConnectionConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for FHIR client.
 */
@Configuration
public class FhirClientConfig {

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public IGenericClient fhirClient(
            FhirContext fhirContext,
            EpicConnectionConfig config,
            EpicAuthProvider authProvider) {

        // Configure socket timeout
        fhirContext.getRestfulClientFactory().setSocketTimeout(config.getRequestTimeoutSeconds() * 1000);

        IGenericClient client = fhirContext.newRestfulGenericClient(config.getBaseUrl());

        // Add bearer token interceptor
        BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(
                authProvider.getAccessToken()
        );
        client.registerInterceptor(authInterceptor);

        return client;
    }
}
