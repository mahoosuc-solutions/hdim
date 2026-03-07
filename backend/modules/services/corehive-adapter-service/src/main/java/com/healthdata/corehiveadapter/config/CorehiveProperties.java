package com.healthdata.corehiveadapter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "external.corehive")
public class CorehiveProperties {
    private boolean enabled = false;
    private String baseUrl = "http://localhost:3067";
    private String apiKey = "";
    private int timeoutMs = 5000;
    private int connectTimeoutMs = 3000;
}
