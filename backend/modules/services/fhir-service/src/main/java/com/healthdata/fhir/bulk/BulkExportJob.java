package com.healthdata.fhir.bulk;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity for tracking FHIR Bulk Export Jobs
 *
 * Tracks the status and metadata of bulk export operations
 * following the FHIR Bulk Data Access specification.
 */
@Entity
@Table(name = "bulk_export_jobs", indexes = {
    @Index(name = "idx_bulk_export_tenant_status", columnList = "tenant_id,status"),
    @Index(name = "idx_bulk_export_requested_at", columnList = "requested_at"),
    @Index(name = "idx_bulk_export_status", columnList = "status")
})
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BulkExportJob {

    @Id
    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID jobId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ExportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_level", nullable = false, length = 32)
    private ExportLevel exportLevel;

    @Column(name = "resource_id", length = 64)
    private String resourceId; // For Patient or Group level exports

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_types", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> resourceTypes = new ArrayList<>();

    @Column(name = "output_format", nullable = false, length = 32)
    @Builder.Default
    private String outputFormat = "ndjson";

    @Column(name = "since_param")
    private Instant sinceParam;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "type_filters", columnDefinition = "jsonb")
    private List<String> typeFilters;

    @Column(name = "request_url", length = 512)
    private String requestUrl;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "transaction_time")
    private Instant transactionTime;

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_files", columnDefinition = "jsonb")
    @Builder.Default
    private List<OutputFile> outputFiles = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_files", columnDefinition = "jsonb")
    @Builder.Default
    private List<OutputFile> errorFiles = new ArrayList<>();

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "total_resources")
    private Long totalResources;

    @Column(name = "exported_resources")
    private Long exportedResources;

    @PrePersist
    void onCreate() {
        if (this.jobId == null) {
            this.jobId = UUID.randomUUID();
        }
        if (this.requestedAt == null) {
            this.requestedAt = Instant.now();
        }
        if (this.status == null) {
            this.status = ExportStatus.PENDING;
        }
    }

    /**
     * Export status enumeration
     */
    public enum ExportStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * Export level enumeration
     */
    public enum ExportLevel {
        SYSTEM,     // GET /fhir/$export
        PATIENT,    // GET /fhir/Patient/$export
        GROUP       // GET /fhir/Group/{id}/$export
    }

    /**
     * Output file metadata
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OutputFile {
        private String type;        // Resource type (e.g., "Patient", "Observation")
        private String url;         // Download URL
        private String filePath;    // Local file path
        private Long count;         // Number of resources in file
    }
}
