package com.healthdata.gateway.clinical.compliance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compliance_errors", indexes = {
    @Index(name = "idx_compliance_tenant_timestamp", columnList = "tenant_id,timestamp"),
    @Index(name = "idx_compliance_severity", columnList = "severity"),
    @Index(name = "idx_compliance_service", columnList = "service"),
    @Index(name = "idx_compliance_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceErrorEntity {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "tenant_id", length = 255, nullable = false)
    private String tenantId;

    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "service", length = 100, nullable = false)
    private String service;

    @Column(name = "endpoint", length = 500)
    private String endpoint;

    @Column(name = "operation", length = 500, nullable = false)
    private String operation;

    @Column(name = "error_code", length = 50, nullable = false)
    private String errorCode;

    @Column(name = "severity", length = 20, nullable = false)
    private String severity;

    @Column(name = "message", columnDefinition = "text", nullable = false)
    private String message;

    @Column(name = "stack", columnDefinition = "text")
    private String stack;

    @Column(name = "additional_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String additionalData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
