package com.healthdata.migration.persistence;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.healthdata.migration.dto.DataType;
import com.healthdata.migration.dto.JobStatus;
import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Migration Job Entity
 *
 * Tracks migration job state, progress, and checkpoints for resumability.
 */
@Entity
@Table(name = "migration_jobs")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MigrationJobEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "job_name", nullable = false, length = 255)
    private String jobName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_config", columnDefinition = "jsonb", nullable = false)
    private SourceConfig sourceConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private DataType dataType;

    @Column(name = "convert_to_fhir", nullable = false)
    @Builder.Default
    private boolean convertToFhir = true;

    @Column(name = "continue_on_error", nullable = false)
    @Builder.Default
    private boolean continueOnError = true;

    @Column(name = "batch_size", nullable = false)
    @Builder.Default
    private int batchSize = 100;

    @Column(name = "resumable", nullable = false)
    @Builder.Default
    private boolean resumable = true;

    // Progress tracking
    @Column(name = "total_records")
    @Builder.Default
    private long totalRecords = 0;

    @Column(name = "processed_count")
    @Builder.Default
    private long processedCount = 0;

    @Column(name = "success_count")
    @Builder.Default
    private long successCount = 0;

    @Column(name = "failure_count")
    @Builder.Default
    private long failureCount = 0;

    @Column(name = "skipped_count")
    @Builder.Default
    private long skippedCount = 0;

    // Timing
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_checkpoint_at")
    private Instant lastCheckpointAt;

    // Retry tracking
    @Column(name = "retry_count")
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private int maxRetries = 3;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    // Checkpoint data for resumability
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checkpoint", columnDefinition = "jsonb")
    private Map<String, Object> checkpoint;

    // FHIR resources created (by type)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fhir_resources_created", columnDefinition = "jsonb")
    private Map<String, Long> fhirResourcesCreated;

    // Optional external references
    @Column(name = "target_fhir_url")
    private String targetFhirUrl;

    @Column(name = "callback_url")
    private String callbackUrl;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Mark job as started
     */
    public void markStarted() {
        this.status = JobStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    /**
     * Mark job as completed
     */
    public void markCompleted() {
        this.status = JobStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /**
     * Mark job as failed
     */
    public void markFailed() {
        this.status = JobStatus.FAILED;
        this.completedAt = Instant.now();
    }

    /**
     * Mark job as paused
     */
    public void markPaused() {
        this.status = JobStatus.PAUSED;
    }

    /**
     * Mark job as cancelled
     */
    public void markCancelled() {
        this.status = JobStatus.CANCELLED;
        this.completedAt = Instant.now();
    }

    /**
     * Check if job can be started
     */
    public boolean canStart() {
        return status == JobStatus.PENDING || status == JobStatus.PAUSED;
    }

    /**
     * Check if job is in a terminal state
     */
    public boolean isTerminal() {
        return status == JobStatus.COMPLETED ||
               status == JobStatus.FAILED ||
               status == JobStatus.CANCELLED;
    }

    /**
     * Update progress counts
     */
    public void updateProgress(long processed, long success, long failure, long skipped) {
        this.processedCount = processed;
        this.successCount = success;
        this.failureCount = failure;
        this.skippedCount = skipped;
    }

    /**
     * Increment success count
     */
    public void incrementSuccess() {
        this.processedCount++;
        this.successCount++;
    }

    /**
     * Increment failure count
     */
    public void incrementFailure() {
        this.processedCount++;
        this.failureCount++;
    }

    /**
     * Increment skipped count
     */
    public void incrementSkipped() {
        this.processedCount++;
        this.skippedCount++;
    }

    /**
     * Save checkpoint
     */
    public void saveCheckpoint(Map<String, Object> checkpointData) {
        this.checkpoint = checkpointData;
        this.lastCheckpointAt = Instant.now();
    }

    /**
     * Calculate next retry time using exponential backoff
     */
    public Instant calculateNextRetry() {
        // Exponential backoff: 1min, 5min, 30min, 2hr, 12hr
        long[] backoffMinutes = {1, 5, 30, 120, 720};
        int index = Math.min(retryCount, backoffMinutes.length - 1);
        return Instant.now().plusSeconds(backoffMinutes[index] * 60);
    }

    /**
     * Check if retry is allowed
     */
    public boolean canRetry() {
        return retryCount < maxRetries &&
               (status == JobStatus.FAILED || status == JobStatus.RETRYING);
    }

    /**
     * Mark for retry
     */
    public void markForRetry() {
        this.retryCount++;
        this.status = JobStatus.RETRYING;
        this.nextRetryAt = calculateNextRetry();
    }
}
