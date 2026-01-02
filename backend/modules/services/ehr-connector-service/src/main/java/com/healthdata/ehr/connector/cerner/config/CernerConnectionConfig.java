package com.healthdata.ehr.connector.cerner.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "cerner.fhir")
public class CernerConnectionConfig {
    @NotBlank(message = "Cerner FHIR base URL is required")
    private String baseUrl;
    @NotBlank(message = "Cerner OAuth2 token URL is required")
    private String tokenUrl;
    @NotBlank(message = "Cerner client ID is required")
    private String clientId;
    @NotBlank(message = "Cerner client secret is required")
    private String clientSecret;
    @NotNull(message = "Cerner tenant ID is required")
    private String tenantId;
    private String scope = "system/*.read";
    private Boolean sandboxMode = false;
    private Integer connectionTimeout = 30000;
    private Integer readTimeout = 30000;
    private Integer maxRetries = 3;
    private Long tokenCacheDuration = 3600L;
    
    public String getFhirEndpoint(String resource) {
        return baseUrl + "/" + resource;
    }
    
    public boolean isSandboxMode() {
        return Boolean.TRUE.equals(sandboxMode);
    }
}
