package com.healthdata.fhir.bulk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class BulkExportJobTest {

    @Test
    void shouldInitializeDefaultsOnCreate() {
        BulkExportJob job = BulkExportJob.builder()
                .tenantId("tenant-1")
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .resourceTypes(List.of("Patient"))
                .build();

        job.onCreate();

        assertThat(job.getJobId()).isNotNull();
        assertThat(job.getRequestedAt()).isNotNull();
        assertThat(job.getStatus()).isEqualTo(BulkExportJob.ExportStatus.PENDING);
        assertThat(job.getResourceTypes()).containsExactly("Patient");
        assertThat(job.getOutputFiles()).isNotNull();
        assertThat(job.getErrorFiles()).isNotNull();
    }

    @Test
    void shouldBuildOutputFileMetadata() {
        BulkExportJob.OutputFile file = BulkExportJob.OutputFile.builder()
                .type("Observation")
                .url("https://example.test/obs.ndjson")
                .filePath("/tmp/obs.ndjson")
                .count(12L)
                .build();

        assertThat(file.getType()).isEqualTo("Observation");
        assertThat(file.getUrl()).isEqualTo("https://example.test/obs.ndjson");
        assertThat(file.getFilePath()).isEqualTo("/tmp/obs.ndjson");
        assertThat(file.getCount()).isEqualTo(12L);
    }

    @Test
    void shouldPreserveProvidedValues() {
        Instant requestedAt = Instant.parse("2025-01-01T00:00:00Z");
        BulkExportJob job = BulkExportJob.builder()
                .jobId(java.util.UUID.randomUUID())
                .tenantId("tenant-1")
                .status(BulkExportJob.ExportStatus.IN_PROGRESS)
                .exportLevel(BulkExportJob.ExportLevel.PATIENT)
                .requestedAt(requestedAt)
                .build();

        job.onCreate();

        assertThat(job.getRequestedAt()).isEqualTo(requestedAt);
        assertThat(job.getStatus()).isEqualTo(BulkExportJob.ExportStatus.IN_PROGRESS);
    }
}
