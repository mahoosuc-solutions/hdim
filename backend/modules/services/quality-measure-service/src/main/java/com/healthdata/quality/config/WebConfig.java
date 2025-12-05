package com.healthdata.quality.config;

import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web Configuration
 *
 * Configures Tomcat to allow encoded slashes in URLs for FHIR patient IDs like "Patient/123"
 */
@Configuration
public class WebConfig {

    /**
     * Allow encoded slashes in URLs
     * Required for FHIR-style patient IDs (e.g., Patient/123, Patient%2F123)
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            connector.setEncodedSolidusHandling(EncodedSolidusHandling.DECODE.getValue());
        });
    }
}
