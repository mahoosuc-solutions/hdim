package com.healthdata.healthixadapter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    @Bean(name = "pooledRestTemplate")
    public RestTemplate pooledRestTemplate(
            @Value("${external.healthix.connection-pool.max-connections:200}") int maxTotal,
            @Value("${external.healthix.connection-pool.max-connections-per-route:50}") int maxPerRoute,
            @Value("${external.healthix.connection-pool.connection-timeout-ms:3000}") int connectTimeout,
            @Value("${external.healthix.connection-pool.socket-timeout-ms:10000}") int socketTimeout) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(socketTimeout);

        return new RestTemplate(factory);
    }
}
