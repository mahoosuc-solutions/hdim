package com.healthdata.priorauth.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing Payer API endpoint configuration.
 *
 * Stores payer-specific API endpoints and authentication details
 * for Prior Authorization and Provider Access APIs.
 */
@Entity
@Table(name = "payer_endpoints", schema = "prior_auth",
    indexes = {
        @Index(name = "idx_payer_id", columnList = "payer_id"),
        @Index(name = "idx_payer_active", columnList = "is_active")
    })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayerEndpointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payer_id", nullable = false, unique = true, length = 255)
    private String payerId;

    @Column(name = "payer_name", nullable = false, length = 255)
    private String payerName;

    @Column(name = "pa_endpoint_url", length = 512)
    private String paEndpointUrl;

    @Column(name = "pa_fhir_base_url", length = 512)
    private String paFhirBaseUrl;

    @Column(name = "provider_access_endpoint_url", length = 512)
    private String providerAccessEndpointUrl;

    @Column(name = "provider_directory_url", length = 512)
    private String providerDirectoryUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", length = 32)
    private AuthType authType;

    @Column(name = "client_id", length = 255)
    private String clientId;

    @Column(name = "client_secret", length = 500)
    private String clientSecret;

    @Column(name = "token_endpoint_url", length = 512)
    private String tokenEndpointUrl;

    @Column(name = "scope", length = 500)
    private String scope;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_headers", columnDefinition = "jsonb")
    private Map<String, String> additionalHeaders;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supported_services", columnDefinition = "jsonb")
    private Map<String, Object> supportedServices;

    @Column(name = "connection_timeout_ms")
    @Builder.Default
    private Integer connectionTimeoutMs = 30000;

    @Column(name = "read_timeout_ms")
    @Builder.Default
    private Integer readTimeoutMs = 60000;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "supports_real_time")
    @Builder.Default
    private Boolean supportsRealTime = false;

    @Column(name = "supports_batch")
    @Builder.Default
    private Boolean supportsBatch = true;

    @Column(name = "last_health_check")
    private LocalDateTime lastHealthCheck;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", length = 32)
    @Builder.Default
    private HealthStatus healthStatus = HealthStatus.UNKNOWN;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum AuthType {
        OAUTH2_CLIENT_CREDENTIALS,
        OAUTH2_AUTHORIZATION_CODE,
        SMART_ON_FHIR,
        API_KEY,
        BASIC_AUTH,
        MUTUAL_TLS
    }

    public enum HealthStatus {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        UNKNOWN
    }
}
