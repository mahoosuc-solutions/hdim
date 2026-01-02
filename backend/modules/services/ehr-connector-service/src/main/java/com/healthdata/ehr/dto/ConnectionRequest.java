package com.healthdata.ehr.dto;

import com.healthdata.ehr.model.EhrVendorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for creating a new EHR connection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionRequest {

    @NotBlank(message = "Connection ID is required")
    private String connectionId;

    @NotNull(message = "Vendor type is required")
    private EhrVendorType vendorType;

    @NotBlank(message = "Base URL is required")
    private String baseUrl;

    @NotBlank(message = "Client ID is required")
    private String clientId;

    @NotBlank(message = "Client secret is required")
    private String clientSecret;

    private String tokenUrl;
    private String scope;
    private Integer timeoutMs;
    private Integer maxRetries;
    private Boolean enableCircuitBreaker;
    private Map<String, Object> additionalProperties;
}
