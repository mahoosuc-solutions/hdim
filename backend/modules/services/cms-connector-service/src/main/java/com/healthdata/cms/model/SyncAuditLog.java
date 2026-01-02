package com.healthdata.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
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

    @Column(name = "sync_source")
    private String syncSource;

    @Column(name = "sync_type")
    private String syncType;

    @Column(name = "sync_status")
    private String syncStatus;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "total_claims")
    private Integer totalClaims;

    @Column(name = "successful_claims")
    private Integer successfulClaims;

    @Column(name = "failed_claims")
    private Integer failedClaims;

    @Column(name = "duplicate_claims")
    private Integer duplicateClaims;
}
