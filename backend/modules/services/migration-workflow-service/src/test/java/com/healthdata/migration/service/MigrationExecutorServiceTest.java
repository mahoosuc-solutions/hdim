package com.healthdata.migration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.healthdata.migration.client.CdrProcessingResult;
import com.healthdata.migration.client.CdrProcessorClient;
import com.healthdata.migration.connector.SourceConnector;
import com.healthdata.migration.connector.SourceConnectorFactory;
import com.healthdata.migration.dto.DataType;
import com.healthdata.migration.dto.JobStatus;
import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.persistence.MigrationCheckpointEntity;
import com.healthdata.migration.persistence.MigrationErrorEntity;
import com.healthdata.migration.persistence.MigrationJobEntity;
import com.healthdata.migration.repository.MigrationErrorRepository;
import com.healthdata.migration.repository.MigrationJobRepository;
import com.healthdata.migration.websocket.MigrationProgressPublisher;

@DisplayName("MigrationExecutorService")
class MigrationExecutorServiceTest {

    @Test
    @DisplayName("Should execute job, save checkpoints, and publish progress")
    void shouldExecuteJobAndPublishProgress() throws Exception {
        UUID jobId = UUID.randomUUID();
        MigrationJobEntity job = baseJob(jobId).toBuilder()
                .status(JobStatus.RUNNING)
                .batchSize(1)
                .build();

        MigrationJobRepository jobRepository = mock(MigrationJobRepository.class);
        MigrationErrorRepository errorRepository = mock(MigrationErrorRepository.class);
        MigrationJobService jobService = mock(MigrationJobService.class);
        SourceConnectorFactory connectorFactory = mock(SourceConnectorFactory.class);
        MigrationProgressPublisher publisher = mock(MigrationProgressPublisher.class);
        CdrProcessorClient cdrProcessorClient = mock(CdrProcessorClient.class);

        SourceConnector connector = mock(SourceConnector.class);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(connectorFactory.createAndConnect(eq(job.getSourceConfig()))).thenReturn(connector);
        when(connector.countRecords()).thenReturn(2L);
        when(connector.getCheckpointData()).thenReturn(Map.of("position", 1L));
        when(connector.getCurrentFile()).thenReturn("file1.hl7");
        when(connector.getCurrentPosition()).thenReturn(1L);

        Iterator<com.healthdata.migration.dto.SourceRecord> iterator = List.of(
                com.healthdata.migration.dto.SourceRecord.of("MSH|^~\\&|", DataType.HL7V2, "file1.hl7", 0),
                com.healthdata.migration.dto.SourceRecord.of("MSH|^~\\&|", DataType.HL7V2, "file1.hl7", 1)
        ).iterator();
        when(connector.readRecords(eq(1))).thenReturn(iterator);

        when(cdrProcessorClient.processRecord(any(), any(), any()))
                .thenReturn(CdrProcessingResult.success("Patient", 1));
        when(jobService.getLatestCheckpoint(jobId)).thenReturn(null);

        MigrationExecutorService service = new MigrationExecutorService(
                jobRepository,
                errorRepository,
                jobService,
                connectorFactory,
                publisher,
                cdrProcessorClient
        );
        ReflectionTestUtils.setField(service, "checkpointInterval", 1);
        ReflectionTestUtils.setField(service, "progressUpdateInterval", 0);

        CompletableFuture<Void> future = service.executeJob(jobId);

        assertThat(future).isNotNull();
        assertThat(future.isCompletedExceptionally()).isFalse();
        verify(jobRepository, atLeastOnce()).save(eq(job));
        verify(jobService, atLeastOnce()).saveCheckpoint(eq(jobId), any(), any(), any());
        verify(publisher, atLeastOnce()).publishProgress(eq(jobId), any());
    }

    @Test
    @DisplayName("Should restore from checkpoint when resumable")
    void shouldRestoreFromCheckpointWhenResumable() throws Exception {
        UUID jobId = UUID.randomUUID();
        MigrationJobEntity job = baseJob(jobId).toBuilder()
                .status(JobStatus.RUNNING)
                .resumable(true)
                .build();

        MigrationJobRepository jobRepository = mock(MigrationJobRepository.class);
        MigrationErrorRepository errorRepository = mock(MigrationErrorRepository.class);
        MigrationJobService jobService = mock(MigrationJobService.class);
        SourceConnectorFactory connectorFactory = mock(SourceConnectorFactory.class);
        MigrationProgressPublisher publisher = mock(MigrationProgressPublisher.class);
        CdrProcessorClient cdrProcessorClient = mock(CdrProcessorClient.class);

        SourceConnector connector = mock(SourceConnector.class);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(connectorFactory.createAndConnect(eq(job.getSourceConfig()))).thenReturn(connector);
        when(connector.countRecords()).thenReturn(0L);
        when(connector.readRecords(eq(job.getBatchSize()))).thenReturn(List.<com.healthdata.migration.dto.SourceRecord>of().iterator());

        MigrationCheckpointEntity checkpoint = MigrationCheckpointEntity.builder()
                .job(job)
                .checkpointData(Map.of("position", 7L))
                .recordsProcessed(7L)
                .build();
        when(jobService.getLatestCheckpoint(jobId)).thenReturn(checkpoint);

        MigrationExecutorService service = new MigrationExecutorService(
                jobRepository,
                errorRepository,
                jobService,
                connectorFactory,
                publisher,
                cdrProcessorClient
        );

        service.executeJob(jobId);

        verify(connector).restoreFromCheckpoint(eq(checkpoint.getCheckpointData()));
    }

    @Test
    @DisplayName("Should mark job failed when processing fails and continueOnError is false")
    void shouldMarkFailedWhenProcessingFails() throws Exception {
        UUID jobId = UUID.randomUUID();
        MigrationJobEntity job = baseJob(jobId).toBuilder()
                .status(JobStatus.RUNNING)
                .continueOnError(false)
                .batchSize(1)
                .build();

        MigrationJobRepository jobRepository = mock(MigrationJobRepository.class);
        MigrationErrorRepository errorRepository = mock(MigrationErrorRepository.class);
        MigrationJobService jobService = mock(MigrationJobService.class);
        SourceConnectorFactory connectorFactory = mock(SourceConnectorFactory.class);
        MigrationProgressPublisher publisher = mock(MigrationProgressPublisher.class);
        CdrProcessorClient cdrProcessorClient = mock(CdrProcessorClient.class);

        SourceConnector connector = mock(SourceConnector.class);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(connectorFactory.createAndConnect(eq(job.getSourceConfig()))).thenReturn(connector);
        when(connector.countRecords()).thenReturn(1L);
        when(connector.getCurrentFile()).thenReturn("file1.hl7");
        when(connector.getCurrentPosition()).thenReturn(0L);

        Iterator<com.healthdata.migration.dto.SourceRecord> iterator = List.of(
                com.healthdata.migration.dto.SourceRecord.of("MSH|^~\\&|", DataType.HL7V2, "file1.hl7", 0)
        ).iterator();
        when(connector.readRecords(eq(1))).thenReturn(iterator);
        when(cdrProcessorClient.processRecord(any(), any(), any()))
                .thenReturn(CdrProcessingResult.failure("boom"));
        when(errorRepository.save(any(MigrationErrorEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MigrationExecutorService service = new MigrationExecutorService(
                jobRepository,
                errorRepository,
                jobService,
                connectorFactory,
                publisher,
                cdrProcessorClient
        );
        ReflectionTestUtils.setField(service, "checkpointInterval", 10);
        ReflectionTestUtils.setField(service, "progressUpdateInterval", 10);

        assertThatThrownBy(() -> service.executeJob(jobId).join())
                .isInstanceOf(RuntimeException.class);

        verify(errorRepository, atLeastOnce()).save(any(MigrationErrorEntity.class));
        verify(jobService, atLeastOnce()).markFailed(eq(jobId), any());
    }

    @Test
    @DisplayName("Should report running jobs and allow cancellation")
    void shouldReportRunningJobsAndCancel() {
        MigrationExecutorService service = new MigrationExecutorService(
                mock(MigrationJobRepository.class),
                mock(MigrationErrorRepository.class),
                mock(MigrationJobService.class),
                mock(SourceConnectorFactory.class),
                mock(MigrationProgressPublisher.class),
                mock(CdrProcessorClient.class)
        );

        @SuppressWarnings("unchecked")
        Map<UUID, AtomicBoolean> runningJobs =
                (Map<UUID, AtomicBoolean>) ReflectionTestUtils.getField(service, "runningJobs");
        UUID jobId = UUID.randomUUID();
        AtomicBoolean token = new AtomicBoolean(true);
        runningJobs.put(jobId, token);

        assertThat(service.isJobRunning(jobId)).isTrue();

        service.requestCancellation(jobId);

        assertThat(token.get()).isFalse();
        assertThat(service.isJobRunning(jobId)).isTrue();
        runningJobs.remove(jobId);
        assertThat(service.isJobRunning(jobId)).isFalse();
    }

    @Test
    @DisplayName("Should continue on errors and categorize failures")
    void shouldContinueOnErrorsAndCategorizeFailures() throws Exception {
        UUID jobId = UUID.randomUUID();
        MigrationJobEntity job = baseJob(jobId).toBuilder()
                .status(JobStatus.RUNNING)
                .continueOnError(true)
                .batchSize(1)
                .build();

        MigrationJobRepository jobRepository = mock(MigrationJobRepository.class);
        MigrationErrorRepository errorRepository = mock(MigrationErrorRepository.class);
        MigrationJobService jobService = mock(MigrationJobService.class);
        SourceConnectorFactory connectorFactory = mock(SourceConnectorFactory.class);
        MigrationProgressPublisher publisher = mock(MigrationProgressPublisher.class);
        CdrProcessorClient cdrProcessorClient = mock(CdrProcessorClient.class);

        SourceConnector connector = mock(SourceConnector.class);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(connectorFactory.createAndConnect(eq(job.getSourceConfig()))).thenReturn(connector);
        when(connector.countRecords()).thenReturn(3L);
        when(connector.getCurrentFile()).thenReturn("file1.hl7");
        when(connector.getCurrentPosition()).thenReturn(0L);

        List<com.healthdata.migration.dto.SourceRecord> records = List.of(
                com.healthdata.migration.dto.SourceRecord.of("BAD", DataType.HL7V2, "file1.hl7", 0),
                com.healthdata.migration.dto.SourceRecord.of("MSH|^~\\&|", DataType.HL7V2, "file1.hl7", 1),
                com.healthdata.migration.dto.SourceRecord.of("{\"resourceType\":\"Bundle\"}", DataType.FHIR_BUNDLE, "file1.hl7", 2)
        );
        when(connector.readRecords(eq(1))).thenReturn(records.iterator());

        when(cdrProcessorClient.processRecord(any(), any(), any()))
                .thenThrow(new RuntimeException("mapping failed"))
                .thenThrow(new RuntimeException("fhir validation failed"));
        when(errorRepository.save(any(MigrationErrorEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MigrationExecutorService service = new MigrationExecutorService(
                jobRepository,
                errorRepository,
                jobService,
                connectorFactory,
                publisher,
                cdrProcessorClient
        );
        ReflectionTestUtils.setField(service, "checkpointInterval", 10);
        ReflectionTestUtils.setField(service, "progressUpdateInterval", 10);

        service.executeJob(jobId);

        org.mockito.ArgumentCaptor<MigrationErrorEntity> captor =
                org.mockito.ArgumentCaptor.forClass(MigrationErrorEntity.class);
        verify(errorRepository, atLeastOnce()).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(MigrationErrorEntity::getErrorCategory)
                .contains(
                        com.healthdata.migration.dto.MigrationErrorCategory.PARSE_ERROR,
                        com.healthdata.migration.dto.MigrationErrorCategory.MAPPING_ERROR,
                        com.healthdata.migration.dto.MigrationErrorCategory.VALIDATION_ERROR
                );
    }

    @Test
    @DisplayName("Should skip empty content records")
    void shouldSkipEmptyContentRecords() throws Exception {
        MigrationExecutorService service = new MigrationExecutorService(
                mock(MigrationJobRepository.class),
                mock(MigrationErrorRepository.class),
                mock(MigrationJobService.class),
                mock(SourceConnectorFactory.class),
                mock(MigrationProgressPublisher.class),
                mock(CdrProcessorClient.class)
        );

        MigrationJobEntity job = baseJob(UUID.randomUUID());
        com.healthdata.migration.dto.SourceRecord record =
                com.healthdata.migration.dto.SourceRecord.of("  ", DataType.HL7V2, "file.hl7", 0);

        Boolean result = ReflectionTestUtils.invokeMethod(service, "processRecord", job, record);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should categorize errors based on message")
    void shouldCategorizeErrors() {
        MigrationExecutorService service = new MigrationExecutorService(
                mock(MigrationJobRepository.class),
                mock(MigrationErrorRepository.class),
                mock(MigrationJobService.class),
                mock(SourceConnectorFactory.class),
                mock(MigrationProgressPublisher.class),
                mock(CdrProcessorClient.class)
        );

        assertThat(categorize(service, "parse failure")).isEqualTo(
                com.healthdata.migration.dto.MigrationErrorCategory.PARSE_ERROR);
        assertThat(categorize(service, "fhir validation failed")).isEqualTo(
                com.healthdata.migration.dto.MigrationErrorCategory.VALIDATION_ERROR);
        assertThat(categorize(service, "mapping error")).isEqualTo(
                com.healthdata.migration.dto.MigrationErrorCategory.MAPPING_ERROR);
        assertThat(categorize(service, "duplicate record")).isEqualTo(
                com.healthdata.migration.dto.MigrationErrorCategory.DUPLICATE_RECORD);
        assertThat(categorize(service, "missing required field")).isEqualTo(
                com.healthdata.migration.dto.MigrationErrorCategory.MISSING_REQUIRED);
        assertThat(categorize(service, "loinc mismatch")).isEqualTo(
                com.healthdata.migration.dto.MigrationErrorCategory.INVALID_CODE);
        assertThat(categorize(service, "connection timeout")).isEqualTo(
                com.healthdata.migration.dto.MigrationErrorCategory.CONNECTIVITY_ERROR);
        assertThat(categorize(service, "other error")).isEqualTo(
                com.healthdata.migration.dto.MigrationErrorCategory.SYSTEM_ERROR);
    }

    private com.healthdata.migration.dto.MigrationErrorCategory categorize(
            MigrationExecutorService service, String message) {
        return ReflectionTestUtils.invokeMethod(service, "categorizeError",
                new RuntimeException(message));
    }

    private MigrationJobEntity baseJob(UUID jobId) {
        return MigrationJobEntity.builder()
                .id(jobId)
                .tenantId("tenant-1")
                .jobName("Test Job")
                .sourceType(com.healthdata.migration.dto.SourceType.FILE)
                .sourceConfig(SourceConfig.forFile("/tmp/file.hl7", "*.hl7", false))
                .dataType(DataType.HL7V2)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
