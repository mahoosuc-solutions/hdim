package com.healthdata.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sync_audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncAuditLog {
    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    // DB: varchar(50) NOT NULL with CHECK (sync_source IN ('BCDA', 'DPC', 'AB2D'))
    @Column(name = "sync_source", nullable = false, length = 50)
    private String syncSource;

    // DB: varchar(50) NOT NULL with CHECK (sync_type IN ('BULK_EXPORT', 'POINT_OF_CARE', 'MANUAL'))
    @Column(name = "sync_type", nullable = false, length = 50)
    private String syncType;

    // DB: varchar(50) NOT NULL with CHECK (sync_status IN ('INITIATED', 'IN_PROGRESS', 'COMPLETED', 'FAILED'))
    @Column(name = "sync_status", nullable = false, length = 50)
    private String syncStatus;

    @Column(name = "total_claims")
    private Integer totalClaims;

    @Column(name = "successful_claims")
    private Integer successfulClaims;

    @Column(name = "failed_claims")
    private Integer failedClaims;

    @Column(name = "duplicate_claims")
    private Integer duplicateClaims;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    // DB: timestamp with time zone NOT NULL
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    // DB: timestamp with time zone (nullable)
    @Column(name = "completed_at")
    private Instant completedAt;

    // DB: integer (not bigint)
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "export_id")
    private String exportId;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private Instant createdAt;
}
