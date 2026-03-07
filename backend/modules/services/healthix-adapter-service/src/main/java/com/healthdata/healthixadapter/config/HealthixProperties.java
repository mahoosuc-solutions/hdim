package com.healthdata.healthixadapter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "external.healthix")
public class HealthixProperties {
    private boolean enabled = false;
    private String gatewayUrl = "http://localhost:3000";
    private String fhirUrl = "http://localhost:8080";
    private String mpiUrl = "http://localhost:8000";
    private String documentServiceUrl = "http://localhost:3010";
    private String hl7ServiceUrl = "http://localhost:3020";
    private int timeoutMs = 10000;
    private int connectTimeoutMs = 5000;

    private Mtls mtls = new Mtls();

    @Data
    public static class Mtls {
        private boolean enabled = false;
        private String keystorePath = "";
        private String keystorePassword = "";
        private String truststorePath = "";
        private String truststorePassword = "";
    }
}
