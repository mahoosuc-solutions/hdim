package com.healthdata.ehr.connector.cerner.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class CernerFhirClientConfig {

    private final CernerConnectionConfig connectionConfig;

    @Bean
    public IGenericClient cernerFhirClient(FhirContext fhirContext) {
        IGenericClient client = fhirContext.newRestfulGenericClient(connectionConfig.getBaseUrl());
        
        client.setEncoding(ca.uhn.fhir.rest.api.EncodingEnum.JSON);
        
        if (connectionConfig.getConnectionTimeout() != null) {
            fhirContext.getRestfulClientFactory()
                    .setConnectTimeout(connectionConfig.getConnectionTimeout());
        }
        
        if (connectionConfig.getReadTimeout() != null) {
            fhirContext.getRestfulClientFactory()
                    .setSocketTimeout(connectionConfig.getReadTimeout());
        }
        
        if (connectionConfig.isSandboxMode()) {
            LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
            loggingInterceptor.setLogRequestSummary(true);
            loggingInterceptor.setLogResponseSummary(true);
            client.registerInterceptor(loggingInterceptor);
        }
        
        return client;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("cernerTokens");
    }
}
