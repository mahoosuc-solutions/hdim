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
 * Configuration DTO for EHR connection settings.
 * Contains all necessary information to establish and maintain a connection to an EHR system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EhrConnectionConfig {

    /**
     * Unique identifier for this connection configuration
     */
    private String connectionId;

    /**
     * Tenant identifier for multi-tenancy support
     */
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    /**
     * EHR vendor type (EPIC, CERNER, ATHENA, GENERIC)
     */
    @NotNull(message = "Vendor type is required")
    private EhrVendorType vendorType;

    /**
     * Base URL of the EHR API endpoint
     */
    @NotBlank(message = "Base URL is required")
    private String baseUrl;

    /**
     * Client ID for OAuth2 authentication
     */
    @NotBlank(message = "Client ID is required")
    private String clientId;

    /**
     * Client secret or private key for authentication
     */
    @NotBlank(message = "Client secret is required")
    private String clientSecret;

    /**
     * OAuth2 token endpoint URL
     */
    private String tokenUrl;

    /**
     * OAuth2 scope(s) required for API access
     */
    private String scope;

    /**
     * Connection timeout in milliseconds
     */
    @Builder.Default
    private Integer timeoutMs = 30000;

    /**
     * Maximum retry attempts for failed requests
     */
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Whether to enable circuit breaker for fault tolerance
     */
    @Builder.Default
    private Boolean enableCircuitBreaker = true;

    /**
     * Additional vendor-specific configuration properties
     */
    private Map<String, Object> additionalProperties;

    /**
     * Whether the connection is active
     */
    @Builder.Default
    private Boolean active = true;
}
