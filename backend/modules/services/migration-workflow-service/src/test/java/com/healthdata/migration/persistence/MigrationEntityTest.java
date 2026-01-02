package com.healthdata.migration.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.healthdata.migration.dto.JobStatus;
import com.healthdata.migration.dto.MigrationErrorCategory;

@DisplayName("Migration Entities")
class MigrationEntityTest {

    @Test
    @DisplayName("Should manage job status and progress")
    void shouldManageJobStatusAndProgress() {
        MigrationJobEntity job = MigrationJobEntity.builder()
                .tenantId("tenant")
                .jobName("Job")
                .status(JobStatus.PENDING)
                .build();

        assertThat(job.canStart()).isTrue();
        job.markStarted();
        assertThat(job.getStatus()).isEqualTo(JobStatus.RUNNING);

        job.incrementSuccess();
        job.incrementFailure();
        job.incrementSkipped();
        assertThat(job.getProcessedCount()).isEqualTo(3);
        assertThat(job.getSuccessCount()).isEqualTo(1);
        assertThat(job.getFailureCount()).isEqualTo(1);
        assertThat(job.getSkippedCount()).isEqualTo(1);

        job.markCompleted();
        assertThat(job.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("Should handle retry logic")
    void shouldHandleRetryLogic() {
        MigrationJobEntity job = MigrationJobEntity.builder()
                .status(JobStatus.FAILED)
                .retryCount(0)
                .maxRetries(3)
                .build();

        assertThat(job.canRetry()).isTrue();
        Instant retryAt = job.calculateNextRetry();
        assertThat(retryAt).isAfter(Instant.now().minusSeconds(1));

        job.markForRetry();
        assertThat(job.getStatus()).isEqualTo(JobStatus.RETRYING);
        assertThat(job.getRetryCount()).isEqualTo(1);
        assertThat(job.getNextRetryAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create error entities from exceptions and messages")
    void shouldCreateErrorEntities() {
        MigrationJobEntity job = MigrationJobEntity.builder()
                .tenantId("tenant")
                .build();

        RuntimeException exception = new RuntimeException("boom");
        MigrationErrorEntity fromException = MigrationErrorEntity.fromException(
                job,
                "rec-1",
                "file1",
                10L,
                MigrationErrorCategory.PARSE_ERROR,
                exception,
                "DATA"
        );

        assertThat(fromException.getTenantId()).isEqualTo("tenant");
        assertThat(fromException.getErrorCategory()).isEqualTo(MigrationErrorCategory.PARSE_ERROR);
        assertThat(fromException.getErrorMessage()).contains("boom");

        MigrationErrorEntity fromMessage = MigrationErrorEntity.fromMessage(
                job,
                "rec-2",
                "file2",
                20L,
                MigrationErrorCategory.MAPPING_ERROR,
                "bad mapping",
                Map.of("field", "value")
        );

        assertThat(fromMessage.getErrorCategory()).isEqualTo(MigrationErrorCategory.MAPPING_ERROR);
        assertThat(fromMessage.getErrorDetails()).containsEntry("field", "value");
    }

    @Test
    @DisplayName("Should create checkpoints from job state")
    void shouldCreateCheckpoint() {
        MigrationJobEntity job = MigrationJobEntity.builder()
                .processedCount(10)
                .successCount(8)
                .failureCount(2)
                .build();

        MigrationCheckpointEntity checkpoint = MigrationCheckpointEntity.create(
                job,
                2,
                Map.of("position", 10),
                "file1",
                10L
        );

        assertThat(checkpoint.getCheckpointNumber()).isEqualTo(2);
        assertThat(checkpoint.getRecordsProcessed()).isEqualTo(10);
        assertThat(checkpoint.getCurrentFile()).isEqualTo("file1");
    }

    @Test
    @DisplayName("Should populate IDs and timestamps on create/update")
    void shouldPopulateIdsAndTimestamps() {
        MigrationJobEntity job = new MigrationJobEntity();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(job, "onCreate");
        assertThat(job.getId()).isNotNull();
        assertThat(job.getCreatedAt()).isNotNull();
        assertThat(job.getUpdatedAt()).isNotNull();

        job.setUpdatedAt(null);
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(job, "onUpdate");
        assertThat(job.getUpdatedAt()).isNotNull();

        MigrationCheckpointEntity checkpoint = new MigrationCheckpointEntity();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(checkpoint, "onCreate");
        assertThat(checkpoint.getId()).isNotNull();
        assertThat(checkpoint.getCreatedAt()).isNotNull();

        MigrationErrorEntity error = new MigrationErrorEntity();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(error, "onCreate");
        assertThat(error.getId()).isNotNull();
        assertThat(error.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should truncate large source data")
    void shouldTruncateSourceData() {
        MigrationJobEntity job = MigrationJobEntity.builder()
                .tenantId("tenant")
                .build();

        String largeData = "X".repeat(12_000);
        MigrationErrorEntity entity = MigrationErrorEntity.fromException(
                job,
                "rec",
                "file",
                1L,
                MigrationErrorCategory.PARSE_ERROR,
                new RuntimeException("boom"),
                largeData
        );

        assertThat(entity.getSourceData()).isNotNull();
        assertThat(entity.getSourceData().length()).isLessThanOrEqualTo(10_000);
    }
}
