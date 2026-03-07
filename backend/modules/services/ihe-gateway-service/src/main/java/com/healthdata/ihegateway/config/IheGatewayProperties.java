package com.healthdata.ihegateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "external.ihe")
public class IheGatewayProperties {
    private boolean enabled;
    private String healthixFhirUrl;
    private String healthixMpiUrl;
    private String healthixDocumentUrl;
    private int timeoutMs;
    private int connectTimeoutMs;
    private Mtls mtls = new Mtls();

    @Data
    public static class Mtls {
        private boolean enabled;
        private String keystorePath;
        private String keystorePassword;
        private String truststorePath;
        private String truststorePassword;
    }
}
