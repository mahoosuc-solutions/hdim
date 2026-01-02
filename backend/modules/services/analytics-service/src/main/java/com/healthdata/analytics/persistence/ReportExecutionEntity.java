package com.healthdata.analytics.persistence;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Report Execution Entity
 *
 * Tracks the execution history of reports including status, results, and errors.
 */
@Entity
@Table(name = "report_executions",
       indexes = {
           @Index(name = "idx_execution_report", columnList = "report_id"),
           @Index(name = "idx_execution_tenant", columnList = "tenant_id"),
           @Index(name = "idx_execution_status", columnList = "status"),
           @Index(name = "idx_execution_started", columnList = "started_at")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExecutionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "report_id", nullable = false)
    private UUID reportId;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED

    @Type(JsonBinaryType.class)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private Map<String, Object> parameters;

    @Type(JsonBinaryType.class)
    @Column(name = "result_data", columnDefinition = "jsonb")
    private Map<String, Object> resultData;

    @Column(name = "result_file_path", length = 500)
    private String resultFilePath;

    @Column(name = "result_file_size")
    private Long resultFileSize;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "triggered_by", length = 100)
    private String triggeredBy; // user ID or "SCHEDULER"

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = "PENDING";
        }
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    public boolean isRunning() {
        return "RUNNING".equals(status);
    }
}
