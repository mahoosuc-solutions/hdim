package com.healthdata.fhir.bulk;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for tracking FHIR Bulk Import Jobs.
 *
 * Mirrors the BulkExportJob async pattern — clients submit an import,
 * receive a job ID, and poll for status. NDJSON files are streamed
 * line-by-line and batched into 200-resource transactions.
 */
@Entity
@Table(name = "bulk_import_jobs", indexes = {
    @Index(name = "idx_bulk_import_tenant_status", columnList = "tenant_id,status"),
    @Index(name = "idx_bulk_import_submitted_at", columnList = "submitted_at"),
    @Index(name = "idx_bulk_import_status", columnList = "status")
})
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BulkImportJob {

    @Id
    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID jobId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ImportStatus status;

    @Column(name = "source_type", nullable = false, length = 32)
    @Builder.Default
    private String sourceType = "file";

    @Column(name = "source_url", length = 1024)
    private String sourceUrl;

    @Column(name = "total_records")
    private Long totalRecords;

    @Column(name = "processed_records")
    @Builder.Default
    private Long processedRecords = 0L;

    @Column(name = "failed_records")
    @Builder.Default
    private Long failedRecords = 0L;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "submitted_by", length = 100)
    private String submittedBy;

    @PrePersist
    void onCreate() {
        if (this.jobId == null) {
            this.jobId = UUID.randomUUID();
        }
        if (this.submittedAt == null) {
            this.submittedAt = Instant.now();
        }
        if (this.status == null) {
            this.status = ImportStatus.PENDING;
        }
    }

    public enum ImportStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
