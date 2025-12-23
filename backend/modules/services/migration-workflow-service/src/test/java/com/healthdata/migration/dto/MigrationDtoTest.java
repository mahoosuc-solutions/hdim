package com.healthdata.migration.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Migration DTOs")
class MigrationDtoTest {

    @Test
    @DisplayName("Should compute completion and success rates")
    void shouldComputeRates() {
        MigrationJobResponse response = MigrationJobResponse.builder()
                .totalRecords(100)
                .processedCount(80)
                .successCount(60)
                .build();

        assertThat(response.getCompletionPercentage()).isEqualTo(80.0);
        assertThat(response.getSuccessRate()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("Should format progress time remaining")
    void shouldFormatProgressTimeRemaining() {
        MigrationProgress progress = MigrationProgress.builder()
                .totalRecords(100)
                .processedCount(50)
                .estimatedTimeRemainingMs(65_000)
                .build();

        assertThat(progress.getCompletionPercentage()).isEqualTo(50.0);
        assertThat(progress.getFormattedTimeRemaining()).contains("min");

        MigrationProgress shortProgress = MigrationProgress.builder()
                .estimatedTimeRemainingMs(5_000)
                .build();
        assertThat(shortProgress.getFormattedTimeRemaining()).contains("seconds");

        MigrationProgress noEstimate = MigrationProgress.builder()
                .estimatedTimeRemainingMs(0)
                .build();
        assertThat(noEstimate.getFormattedTimeRemaining()).isEqualTo("Calculating...");
    }

    @Test
    @DisplayName("Should build progress snapshot from factory")
    void shouldBuildProgressFromFactory() {
        UUID jobId = UUID.randomUUID();
        MigrationProgress progress = MigrationProgress.from(jobId, JobStatus.RUNNING, 10, 3, 2, 1, 0);

        assertThat(progress.getJobId()).isEqualTo(jobId);
        assertThat(progress.getStatus()).isEqualTo(JobStatus.RUNNING);
        assertThat(progress.getProcessedCount()).isEqualTo(3);
        assertThat(progress.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should format migration summary duration and totals")
    void shouldFormatMigrationSummaryDuration() {
        MigrationSummary summary = MigrationSummary.builder()
                .totalDurationMs(3_700_000)
                .fhirResourcesCreated(Map.of("Patient", 2L, "Observation", 1L))
                .build();

        assertThat(summary.getFormattedDuration()).contains("hr");
        assertThat(summary.getTotalFhirResourcesCreated()).isEqualTo(3);

        MigrationSummary empty = MigrationSummary.builder()
                .totalDurationMs(0)
                .build();
        assertThat(empty.getFormattedDuration()).isEqualTo("N/A");
        assertThat(empty.getTotalFhirResourcesCreated()).isEqualTo(0);

        MigrationSummary minutes = MigrationSummary.builder()
                .totalDurationMs(65_000)
                .build();
        assertThat(minutes.getFormattedDuration()).contains("min");

        MigrationSummary seconds = MigrationSummary.builder()
                .totalDurationMs(5_000)
                .build();
        assertThat(seconds.getFormattedDuration()).contains("sec");
    }

    @Test
    @DisplayName("Should compute quality score and grade")
    void shouldComputeQualityScoreAndGrade() {
        DataQualityReport.QualitySummary summary = DataQualityReport.QualitySummary.builder()
                .totalRecords(100)
                .successCount(98)
                .failureCount(2)
                .successRate(98.0)
                .build();

        DataQualityReport report = DataQualityReport.builder()
                .jobId(UUID.randomUUID())
                .summary(summary)
                .dataQualityIssues(List.of(
                        DataQualityReport.DataQualityIssue.builder()
                                .severity(DataQualityReport.DataQualityIssue.Severity.CRITICAL)
                                .build()
                ))
                .generatedAt(Instant.now())
                .build();

        assertThat(report.getQualityScore()).isLessThan(98.0);
        assertThat(report.getQualityGrade()).isNotEmpty();

        DataQualityReport empty = DataQualityReport.builder().build();
        assertThat(empty.getQualityScore()).isEqualTo(0.0);

        DataQualityReport.QualitySummary summaryA = DataQualityReport.QualitySummary.builder()
                .totalRecords(100)
                .successRate(96.0)
                .build();
        DataQualityReport reportA = DataQualityReport.builder()
                .summary(summaryA)
                .build();
        assertThat(reportA.getQualityGrade()).isEqualTo("A");

        DataQualityReport.QualitySummary summaryF = DataQualityReport.QualitySummary.builder()
                .totalRecords(100)
                .successRate(50.0)
                .build();
        DataQualityReport reportF = DataQualityReport.builder()
                .summary(summaryF)
                .build();
        assertThat(reportF.getQualityGrade()).isEqualTo("F");
    }

    @Test
    @DisplayName("Should build source configs")
    void shouldBuildSourceConfigs() {
        SourceConfig file = SourceConfig.forFile("/tmp/input", "*.hl7", true);
        assertThat(file.getSourceType()).isEqualTo(SourceType.FILE);
        assertThat(file.isRecursive()).isTrue();

        SourceConfig sftpPassword = SourceConfig.forSftpPassword("host", 22, "user", "pass", "/remote");
        assertThat(sftpPassword.getSourceType()).isEqualTo(SourceType.SFTP);
        assertThat(sftpPassword.getAuthType()).isEqualTo(SourceConfig.AuthType.PASSWORD);

        SourceConfig sftpKey = SourceConfig.forSftpKey("host", 22, "user", "/key", "/remote");
        assertThat(sftpKey.getAuthType()).isEqualTo(SourceConfig.AuthType.KEY);

        SourceConfig mllp = SourceConfig.forMllp(2575, true);
        assertThat(mllp.getSourceType()).isEqualTo(SourceType.MLLP);
        assertThat(mllp.isTlsEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should handle zero totals for progress")
    void shouldHandleZeroTotalsForProgress() {
        MigrationProgress progress = MigrationProgress.builder()
                .totalRecords(0)
                .processedCount(0)
                .build();

        assertThat(progress.getCompletionPercentage()).isEqualTo(0.0);

        MigrationProgress hours = MigrationProgress.builder()
                .estimatedTimeRemainingMs(3_600_000)
                .build();
        assertThat(hours.getFormattedTimeRemaining()).contains("hr");
    }
}
