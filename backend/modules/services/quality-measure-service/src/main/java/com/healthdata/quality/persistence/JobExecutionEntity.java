package com.healthdata.quality.persistence;

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
 * Job Execution Entity
 *
 * Tracks scheduled/batch job runs for auditing and reliability.
 */
@Entity
@Table(name = "job_executions", indexes = {
    @Index(name = "idx_je_job_history", columnList = "tenant_id, job_name, started_at"),
    @Index(name = "idx_je_running", columnList = "tenant_id, job_name, status"),
    @Index(name = "idx_je_performance", columnList = "job_name, status, duration_ms"),
    @Index(name = "idx_je_tenant", columnList = "tenant_id"),
    @Index(name = "idx_je_cleanup", columnList = "tenant_id, completed_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "result_message", columnDefinition = "text")
    private String resultMessage;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics", columnDefinition = "jsonb")
    private Map<String, Object> metrics;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (startedAt == null) {
            startedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
