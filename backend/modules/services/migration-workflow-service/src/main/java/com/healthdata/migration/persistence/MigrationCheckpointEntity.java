package com.healthdata.migration.persistence;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Migration Checkpoint Entity
 *
 * Stores checkpoint data for job resumability.
 * Each checkpoint captures the state needed to resume processing.
 */
@Entity
@Table(name = "migration_checkpoints")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MigrationCheckpointEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private MigrationJobEntity job;

    // Checkpoint sequence number
    @Column(name = "checkpoint_number", nullable = false)
    private int checkpointNumber;

    // Checkpoint data (source-specific)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checkpoint_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> checkpointData;

    // Progress at checkpoint time
    @Column(name = "records_processed", nullable = false)
    private long recordsProcessed;

    @Column(name = "records_success", nullable = false)
    private long recordsSuccess;

    @Column(name = "records_failure", nullable = false)
    private long recordsFailure;

    // Current file/position info
    @Column(name = "current_file")
    private String currentFile;

    @Column(name = "current_offset")
    private Long currentOffset;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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
     * Create a checkpoint from current job state
     */
    public static MigrationCheckpointEntity create(
            MigrationJobEntity job,
            int checkpointNumber,
            Map<String, Object> checkpointData,
            String currentFile,
            Long currentOffset) {

        return MigrationCheckpointEntity.builder()
                .job(job)
                .checkpointNumber(checkpointNumber)
                .checkpointData(checkpointData)
                .recordsProcessed(job.getProcessedCount())
                .recordsSuccess(job.getSuccessCount())
                .recordsFailure(job.getFailureCount())
                .currentFile(currentFile)
                .currentOffset(currentOffset)
                .build();
    }
}
