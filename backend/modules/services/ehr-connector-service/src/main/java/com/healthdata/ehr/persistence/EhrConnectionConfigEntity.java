package com.healthdata.ehr.persistence;

import com.healthdata.ehr.dto.EhrConnectionConfig;
import com.healthdata.ehr.model.EhrVendorType;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Persists EHR connection configurations so they survive service restarts.
 *
 * <p>The in-memory ConcurrentHashMap in EhrConnectionManager provides fast
 * runtime access; this entity provides durability. On startup, active configs
 * are loaded from the database and used to reconstruct live connectors.</p>
 */
@Entity
@Table(name = "ehr_connections", indexes = {
    @Index(name = "idx_ehr_conn_tenant", columnList = "tenant_id"),
    @Index(name = "idx_ehr_conn_tenant_active", columnList = "tenant_id,active")
})
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EhrConnectionConfigEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "connection_id", nullable = false, unique = true, length = 255)
    private String connectionId;

    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vendor_type", nullable = false, length = 50)
    private EhrVendorType vendorType;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "client_id", nullable = false, length = 255)
    private String clientId;

    @Column(name = "client_secret", nullable = false, length = 500)
    private String clientSecret;

    @Column(name = "token_url", length = 500)
    private String tokenUrl;

    @Column(name = "scope", length = 500)
    private String scope;

    @Column(name = "timeout_ms")
    @Builder.Default
    private Integer timeoutMs = 30000;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "enable_circuit_breaker")
    @Builder.Default
    private Boolean enableCircuitBreaker = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_properties", columnDefinition = "jsonb")
    private Map<String, Object> additionalProperties;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Convert this entity to the DTO used by the connector system.
     */
    public EhrConnectionConfig toConfig() {
        return EhrConnectionConfig.builder()
                .connectionId(connectionId)
                .tenantId(tenantId)
                .vendorType(vendorType)
                .baseUrl(baseUrl)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tokenUrl(tokenUrl)
                .scope(scope)
                .timeoutMs(timeoutMs)
                .maxRetries(maxRetries)
                .enableCircuitBreaker(enableCircuitBreaker)
                .additionalProperties(additionalProperties)
                .active(active)
                .build();
    }

    /**
     * Create an entity from the DTO.
     */
    public static EhrConnectionConfigEntity fromConfig(EhrConnectionConfig config) {
        return EhrConnectionConfigEntity.builder()
                .connectionId(config.getConnectionId())
                .tenantId(config.getTenantId())
                .vendorType(config.getVendorType())
                .baseUrl(config.getBaseUrl())
                .clientId(config.getClientId())
                .clientSecret(config.getClientSecret())
                .tokenUrl(config.getTokenUrl())
                .scope(config.getScope())
                .timeoutMs(config.getTimeoutMs())
                .maxRetries(config.getMaxRetries())
                .enableCircuitBreaker(config.getEnableCircuitBreaker())
                .additionalProperties(config.getAdditionalProperties())
                .active(config.getActive() != null ? config.getActive() : true)
                .build();
    }
}
