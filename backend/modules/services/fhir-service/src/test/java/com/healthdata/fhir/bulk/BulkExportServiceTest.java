package com.healthdata.fhir.bulk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("Bulk Export Service Tests")
class BulkExportServiceTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private BulkExportRepository exportRepository;

    @Mock
    private BulkExportProcessor exportProcessor;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("Should kick off export with defaults and start processing")
    void shouldKickOffExport() {
        BulkExportConfig config = new BulkExportConfig();
        config.setMaxConcurrentExports(5);
        BulkExportService service = new BulkExportService(exportRepository, exportProcessor, config, kafkaTemplate);
        when(exportRepository.countActiveJobsByTenant(TENANT_ID)).thenReturn(0L);
        when(exportRepository.save(any(BulkExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID jobId = service.kickOffExport(
                TENANT_ID,
                BulkExportJob.ExportLevel.SYSTEM,
                null,
                null,
                null,
                List.of(),
                "/fhir/$export",
                "user");

        assertThat(jobId).isNotNull();
        ArgumentCaptor<BulkExportJob> captor = ArgumentCaptor.forClass(BulkExportJob.class);
        verify(exportRepository).save(captor.capture());
        assertThat(captor.getValue().getResourceTypes()).contains("Patient", "Observation");
        verify(exportProcessor).processExport(jobId);
        verify(kafkaTemplate).send(eq("fhir.bulk-export.initiated"), eq(jobId.toString()), any());
    }

    @Test
    @DisplayName("Should kick off export with provided resource types")
    void shouldKickOffExportWithProvidedResourceTypes() {
        BulkExportConfig config = new BulkExportConfig();
        config.setMaxConcurrentExports(5);
        BulkExportService service = new BulkExportService(exportRepository, exportProcessor, config, kafkaTemplate);
        when(exportRepository.countActiveJobsByTenant(TENANT_ID)).thenReturn(0L);
        when(exportRepository.save(any(BulkExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<String> types = List.of("Patient", "Condition");
        UUID jobId = service.kickOffExport(
                TENANT_ID,
                BulkExportJob.ExportLevel.SYSTEM,
                null,
                types,
                null,
                List.of("Observation?code=abc"),
                "/fhir/$export",
                "user");

        ArgumentCaptor<BulkExportJob> captor = ArgumentCaptor.forClass(BulkExportJob.class);
        verify(exportRepository).save(captor.capture());
        assertThat(captor.getValue().getResourceTypes()).containsExactlyElementsOf(types);
        verify(exportProcessor).processExport(jobId);
    }

    @Test
    @DisplayName("Should default resource types when empty list provided")
    void shouldUseDefaultResourceTypesWhenEmptyList() {
        BulkExportConfig config = new BulkExportConfig();
        config.setMaxConcurrentExports(5);
        BulkExportService service = new BulkExportService(exportRepository, exportProcessor, config, kafkaTemplate);
        when(exportRepository.countActiveJobsByTenant(TENANT_ID)).thenReturn(0L);
        when(exportRepository.save(any(BulkExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID jobId = service.kickOffExport(
                TENANT_ID,
                BulkExportJob.ExportLevel.SYSTEM,
                null,
                List.of(),
                null,
                List.of(),
                "/fhir/$export",
                "user");

        ArgumentCaptor<BulkExportJob> captor = ArgumentCaptor.forClass(BulkExportJob.class);
        verify(exportRepository).save(captor.capture());
        assertThat(captor.getValue().getResourceTypes()).contains("Patient", "Observation");
        verify(exportProcessor).processExport(jobId);
    }

    @Test
    @DisplayName("Should reject when export limit exceeded")
    void shouldRejectWhenLimitExceeded() {
        BulkExportConfig config = new BulkExportConfig();
        config.setMaxConcurrentExports(1);
        BulkExportService service = new BulkExportService(exportRepository, exportProcessor, config, kafkaTemplate);
        when(exportRepository.countActiveJobsByTenant(TENANT_ID)).thenReturn(1L);

        assertThatThrownBy(() -> service.kickOffExport(
                TENANT_ID,
                BulkExportJob.ExportLevel.SYSTEM,
                null,
                null,
                null,
                List.of(),
                "/fhir/$export",
                "user"))
            .isInstanceOf(BulkExportService.ExportLimitExceededException.class);
    }

    @Test
    @DisplayName("Should cancel export job")
    void shouldCancelJob() {
        BulkExportConfig config = new BulkExportConfig();
        BulkExportService service = new BulkExportService(exportRepository, exportProcessor, config, kafkaTemplate);
        UUID jobId = UUID.randomUUID();
        BulkExportJob job = BulkExportJob.builder()
                .jobId(jobId)
                .tenantId(TENANT_ID)
                .status(BulkExportJob.ExportStatus.PENDING)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .build();
        when(exportRepository.findByJobIdAndTenantId(jobId, TENANT_ID)).thenReturn(Optional.of(job));
        when(exportRepository.save(any(BulkExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelJob(TENANT_ID, jobId, "user");

        verify(exportRepository).save(any(BulkExportJob.class));
        verify(kafkaTemplate).send(eq("fhir.bulk-export.cancelled"), eq(jobId.toString()), any());
    }

    @Test
    @DisplayName("Should ignore cancel when already cancelled")
    void shouldIgnoreCancelWhenAlreadyCancelled() {
        BulkExportConfig config = new BulkExportConfig();
        BulkExportService service = new BulkExportService(exportRepository, exportProcessor, config, kafkaTemplate);
        UUID jobId = UUID.randomUUID();
        BulkExportJob job = BulkExportJob.builder()
                .jobId(jobId)
                .tenantId(TENANT_ID)
                .status(BulkExportJob.ExportStatus.CANCELLED)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .build();
        when(exportRepository.findByJobIdAndTenantId(jobId, TENANT_ID)).thenReturn(Optional.of(job));

        service.cancelJob(TENANT_ID, jobId, "user");

        verify(exportRepository, org.mockito.Mockito.never()).save(any(BulkExportJob.class));
        verify(kafkaTemplate, org.mockito.Mockito.never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should reject cancel on completed job")
    void shouldRejectCancelOnCompleted() {
        BulkExportConfig config = new BulkExportConfig();
        BulkExportService service = new BulkExportService(exportRepository, exportProcessor, config, kafkaTemplate);
        UUID jobId = UUID.randomUUID();
        BulkExportJob job = BulkExportJob.builder()
                .jobId(jobId)
                .tenantId(TENANT_ID)
                .status(BulkExportJob.ExportStatus.COMPLETED)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .build();
        when(exportRepository.findByJobIdAndTenantId(jobId, TENANT_ID)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.cancelJob(TENANT_ID, jobId, "user"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Should reject cancel on failed job")
    void shouldRejectCancelOnFailed() {
        BulkExportConfig config = new BulkExportConfig();
        BulkExportService service = new BulkExportService(exportRepository, exportProcessor, config, kafkaTemplate);
        UUID jobId = UUID.randomUUID();
        BulkExportJob job = BulkExportJob.builder()
                .jobId(jobId)
                .tenantId(TENANT_ID)
                .status(BulkExportJob.ExportStatus.FAILED)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .build();
        when(exportRepository.findByJobIdAndTenantId(jobId, TENANT_ID)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.cancelJob(TENANT_ID, jobId, "user"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Should reject cancel when job not found")
    void shouldRejectCancelWhenJobNotFound() {
        BulkExportConfig config = new BulkExportConfig();
        BulkExportService service = new BulkExportService(exportRepository, exportProcessor, config, kafkaTemplate);
        UUID jobId = UUID.randomUUID();
        when(exportRepository.findByJobIdAndTenantId(jobId, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelJob(TENANT_ID, jobId, "user"))
                .isInstanceOf(BulkExportService.ExportJobNotFoundException.class);
    }

    @Test
    @DisplayName("Should fetch job status when present")
    void shouldFetchJobStatus() {
        BulkExportConfig config = new BulkExportConfig();
        BulkExportService service = new BulkExportService(exportRepository, exportProcessor, config, kafkaTemplate);
        UUID jobId = UUID.randomUUID();
        BulkExportJob job = BulkExportJob.builder()
                .jobId(jobId)
                .tenantId(TENANT_ID)
                .status(BulkExportJob.ExportStatus.PENDING)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .build();
        when(exportRepository.findByJobIdAndTenantId(jobId, TENANT_ID)).thenReturn(Optional.of(job));

        Optional<BulkExportJob> result = service.getJobStatus(TENANT_ID, jobId);

        assertThat(result).contains(job);
    }

    @Test
    @DisplayName("Should build manifest for completed job")
    void shouldBuildManifest() {
        BulkExportConfig config = new BulkExportConfig();
        config.setRequireAccessToken(true);
        BulkExportService service = new BulkExportService(exportRepository, exportProcessor, config, kafkaTemplate);
        BulkExportJob job = BulkExportJob.builder()
                .jobId(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .status(BulkExportJob.ExportStatus.COMPLETED)
                .transactionTime(Instant.now())
                .requestUrl("/fhir/$export")
                .outputFiles(List.of())
                .errorFiles(List.of())
                .build();

        BulkExportService.ExportManifest manifest = service.buildManifest(job);

        assertThat(manifest.request()).isEqualTo("/fhir/$export");
        assertThat(manifest.output()).isEmpty();
        assertThat(manifest.requiresAccessToken()).isTrue();
    }

    @Test
    @DisplayName("Should reject manifest creation when job not completed")
    void shouldRejectManifestWhenNotCompleted() {
        BulkExportConfig config = new BulkExportConfig();
        BulkExportService service = new BulkExportService(exportRepository, exportProcessor, config, kafkaTemplate);
        BulkExportJob job = BulkExportJob.builder()
                .jobId(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .status(BulkExportJob.ExportStatus.IN_PROGRESS)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .build();

        assertThatThrownBy(() -> service.buildManifest(job))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Should clean up old exports")
    void shouldCleanupOldExports() {
        BulkExportConfig config = new BulkExportConfig();
        BulkExportService service = new BulkExportService(exportRepository, exportProcessor, config, kafkaTemplate);
        BulkExportJob job = BulkExportJob.builder()
                .jobId(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .status(BulkExportJob.ExportStatus.COMPLETED)
                .completedAt(Instant.now().minusSeconds(900000))
                .build();
        when(exportRepository.findByCompletedAtBefore(any())).thenReturn(List.of(job));

        service.cleanupOldExports();

        verify(exportProcessor).deleteExportFiles(job);
        verify(exportRepository).delete(job);
    }
}
