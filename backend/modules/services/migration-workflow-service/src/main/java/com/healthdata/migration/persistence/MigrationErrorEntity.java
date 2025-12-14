package com.healthdata.migration.persistence;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.healthdata.migration.dto.MigrationErrorCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Migration Error Entity
 *
 * Tracks individual record failures during migration for quality reporting.
 */
@Entity
@Table(name = "migration_errors")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MigrationErrorEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private MigrationJobEntity job;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "record_identifier")
    private String recordIdentifier;

    @Column(name = "source_file")
    private String sourceFile;

    @Column(name = "record_offset")
    private Long recordOffset;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_category", nullable = false, length = 50)
    private MigrationErrorCategory errorCategory;

    @Column(name = "error_message", columnDefinition = "text", nullable = false)
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_details", columnDefinition = "jsonb")
    private Map<String, Object> errorDetails;

    @Column(name = "source_data", columnDefinition = "text")
    private String sourceData;

    @Column(name = "stack_trace", columnDefinition = "text")
    private String stackTrace;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Fields extracted from source data for searchability
    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "message_type")
    private String messageType;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Create an error entity from an exception
     */
    public static MigrationErrorEntity fromException(
            MigrationJobEntity job,
            String recordId,
            String sourceFile,
            Long offset,
            MigrationErrorCategory category,
            Exception e,
            String sourceData) {

        return MigrationErrorEntity.builder()
                .job(job)
                .tenantId(job.getTenantId())
                .recordIdentifier(recordId)
                .sourceFile(sourceFile)
                .recordOffset(offset)
                .errorCategory(category)
                .errorMessage(e.getMessage())
                .stackTrace(getStackTraceString(e))
                .sourceData(truncateSourceData(sourceData))
                .build();
    }

    /**
     * Create an error entity with custom message
     */
    public static MigrationErrorEntity fromMessage(
            MigrationJobEntity job,
            String recordId,
            String sourceFile,
            Long offset,
            MigrationErrorCategory category,
            String message,
            Map<String, Object> details) {

        return MigrationErrorEntity.builder()
                .job(job)
                .tenantId(job.getTenantId())
                .recordIdentifier(recordId)
                .sourceFile(sourceFile)
                .recordOffset(offset)
                .errorCategory(category)
                .errorMessage(message)
                .errorDetails(details)
                .build();
    }

    private static String getStackTraceString(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        String trace = sw.toString();
        // Limit stack trace to 4000 chars
        return trace.length() > 4000 ? trace.substring(0, 4000) : trace;
    }

    private static String truncateSourceData(String data) {
        if (data == null) {
            return null;
        }
        // Limit source data to 10000 chars for storage
        return data.length() > 10000 ? data.substring(0, 10000) : data;
    }
}
