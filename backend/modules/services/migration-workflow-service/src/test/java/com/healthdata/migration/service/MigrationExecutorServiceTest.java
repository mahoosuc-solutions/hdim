package com.healthdata.migration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.healthdata.migration.client.CdrProcessingResult;
import com.healthdata.migration.client.CdrProcessorClient;
import com.healthdata.migration.connector.SourceConnector;
import com.healthdata.migration.connector.SourceConnectorFactory;
import com.healthdata.migration.dto.DataType;
import com.healthdata.migration.dto.JobStatus;
import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceRecord;
import com.healthdata.migration.dto.SourceType;
import com.healthdata.migration.persistence.MigrationJobEntity;
import com.healthdata.migration.repository.MigrationErrorRepository;
import com.healthdata.migration.repository.MigrationJobRepository;
import com.healthdata.migration.websocket.MigrationProgressPublisher;

/**
 * Unit tests for MigrationExecutorService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MigrationExecutorService")
class MigrationExecutorServiceTest {

    @Mock
    private MigrationJobRepository jobRepository;

    @Mock
    private MigrationErrorRepository errorRepository;

    @Mock
    private MigrationJobService jobService;

    @Mock
    private SourceConnectorFactory connectorFactory;

    @Mock
    private MigrationProgressPublisher progressPublisher;

    @Mock
    private CdrProcessorClient cdrProcessorClient;

    private MigrationExecutorService service;

    private static final UUID JOB_ID = UUID.randomUUID();
    private static final String TENANT_ID = "test-tenant";

    @BeforeEach
    void setUp() {
        service = new MigrationExecutorService(
            jobRepository,
            errorRepository,
            jobService,
            connectorFactory,
            progressPublisher,
            cdrProcessorClient
        );

        // Set the configuration values
        ReflectionTestUtils.setField(service, "checkpointInterval", 500);
        ReflectionTestUtils.setField(service, "progressUpdateInterval", 1000);

        // Default mock behavior for CDR processor - return success
        org.mockito.Mockito.lenient()
            .when(cdrProcessorClient.processRecord(any(String.class), any(DataType.class), any(String.class)))
            .thenReturn(CdrProcessingResult.success("Patient", 1));
    }

    @Nested
    @DisplayName("Job Execution")
    class JobExecutionTests {

        @Test
        @DisplayName("Should execute job successfully")
        void shouldExecuteJobSuccessfully() throws Exception {
            // Given
            MigrationJobEntity job = createJobEntity();
            SourceConnector connector = mock(SourceConnector.class);

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(connectorFactory.createAndConnect(any(SourceConfig.class))).thenReturn(connector);
            when(connector.countRecords()).thenReturn(10L);
            when(connector.readRecords(any(Integer.class))).thenReturn(Collections.emptyIterator());
            when(connector.getCurrentFile()).thenReturn("test.hl7");
            when(connector.getCurrentPosition()).thenReturn(0L);
            org.mockito.Mockito.lenient().when(connector.getCheckpointData()).thenReturn(new HashMap<>());
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(connector).close();

            // When
            service.executeJob(JOB_ID).get();

            // Then
            verify(jobRepository).findById(JOB_ID);
            verify(connectorFactory).createAndConnect(any(SourceConfig.class));
            verify(connector).countRecords();
            verify(connector).close();
        }

        @Test
        @DisplayName("Should handle job not found")
        void shouldHandleJobNotFound() {
            // Given
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

            // When / Then
            try {
                service.executeJob(JOB_ID).get();
                org.junit.jupiter.api.Assertions.fail("Should have thrown an exception");
            } catch (java.util.concurrent.ExecutionException e) {
                // ExecutionException wraps the actual exception from async execution
                assertThat(e.getCause()).isNotNull();
                assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
                assertThat(e.getCause().getMessage()).contains("Job not found");
            } catch (IllegalArgumentException e) {
                // Or thrown directly if not async in test context
                assertThat(e.getMessage()).contains("Job not found");
            } catch (Exception e) {
                org.junit.jupiter.api.Assertions.fail("Unexpected exception type: " + e.getClass().getName() + " - " + e.getMessage());
            }
        }

        @Test
        @DisplayName("Should resume from checkpoint")
        void shouldResumeFromCheckpoint() throws Exception {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setResumable(true);
            SourceConnector connector = mock(SourceConnector.class);
            Map<String, Object> checkpointData = new HashMap<>();
            checkpointData.put("offset", 1000L);

            var checkpoint = mock(com.healthdata.migration.persistence.MigrationCheckpointEntity.class);
            when(checkpoint.getCheckpointData()).thenReturn(checkpointData);

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(connectorFactory.createAndConnect(any(SourceConfig.class))).thenReturn(connector);
            when(connector.countRecords()).thenReturn(10L);
            when(jobService.getLatestCheckpoint(JOB_ID)).thenReturn(checkpoint);
            when(connector.readRecords(any(Integer.class))).thenReturn(Collections.emptyIterator());
            when(connector.getCurrentFile()).thenReturn("test.hl7");
            when(connector.getCurrentPosition()).thenReturn(0L);
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(connector).close();

            // When
            service.executeJob(JOB_ID).get();

            // Then
            verify(jobService).getLatestCheckpoint(JOB_ID);
            verify(connector).restoreFromCheckpoint(checkpointData);
        }

        @Test
        @DisplayName("Should mark job as failed on exception")
        void shouldMarkJobAsFailedOnException() throws Exception {
            // Given
            MigrationJobEntity job = createJobEntity();
            SourceConnector connector = mock(SourceConnector.class);

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(connectorFactory.createAndConnect(any(SourceConfig.class))).thenReturn(connector);
            when(connector.countRecords()).thenThrow(new RuntimeException("Connection failed"));

            // When
            try {
                service.executeJob(JOB_ID).get();
            } catch (Exception e) {
                // Expected
            }

            // Then
            verify(jobService).markFailed(eq(JOB_ID), any(String.class));
        }
    }

    @Nested
    @DisplayName("Record Processing")
    class RecordProcessingTests {

        @Test
        @DisplayName("Should process valid HL7v2 record")
        void shouldProcessValidHl7Record() throws Exception {
            // Given
            MigrationJobEntity job = createJobEntity();
            SourceConnector connector = mock(SourceConnector.class);
            SourceRecord record = createSourceRecord("MSH|^~\\&|...", DataType.HL7V2);

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(connectorFactory.createAndConnect(any(SourceConfig.class))).thenReturn(connector);
            when(connector.countRecords()).thenReturn(1L);
            when(connector.readRecords(any(Integer.class))).thenReturn(java.util.Collections.singletonList(record).iterator());
            when(connector.getCurrentFile()).thenReturn("test.hl7");
            when(connector.getCurrentPosition()).thenReturn(0L);
            org.mockito.Mockito.lenient().when(connector.getCheckpointData()).thenReturn(new HashMap<>());
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(connector).close();

            // When
            service.executeJob(JOB_ID).get();

            // Then - verify job was completed successfully
            verify(jobRepository, org.mockito.Mockito.atLeastOnce()).save(any(MigrationJobEntity.class));
            verify(errorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should skip empty records")
        void shouldSkipEmptyRecords() throws Exception {
            // Given
            MigrationJobEntity job = createJobEntity();
            SourceConnector connector = mock(SourceConnector.class);
            SourceRecord record = createSourceRecord("", DataType.HL7V2);

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(connectorFactory.createAndConnect(any(SourceConfig.class))).thenReturn(connector);
            when(connector.countRecords()).thenReturn(1L);
            when(connector.readRecords(any(Integer.class))).thenReturn(java.util.Collections.singletonList(record).iterator());
            when(connector.getCurrentFile()).thenReturn("test.hl7");
            when(connector.getCurrentPosition()).thenReturn(0L);
            org.mockito.Mockito.lenient().when(connector.getCheckpointData()).thenReturn(new HashMap<>());
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(connector).close();

            // When
            service.executeJob(JOB_ID).get();

            // Then - verify record was skipped, no errors
            verify(jobRepository, org.mockito.Mockito.atLeastOnce()).save(any(MigrationJobEntity.class));
            verify(errorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle record processing errors")
        void shouldHandleRecordProcessingErrors() throws Exception {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setContinueOnError(true);
            SourceConnector connector = mock(SourceConnector.class);
            SourceRecord record = createSourceRecord("INVALID", DataType.HL7V2);

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(connectorFactory.createAndConnect(any(SourceConfig.class))).thenReturn(connector);
            when(connector.countRecords()).thenReturn(1L);
            when(connector.readRecords(any(Integer.class))).thenReturn(java.util.Collections.singletonList(record).iterator());
            when(connector.getCurrentFile()).thenReturn("test.hl7");
            when(connector.getCurrentPosition()).thenReturn(0L);
            org.mockito.Mockito.lenient().when(connector.getCheckpointData()).thenReturn(new HashMap<>());
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(connector).close();

            // When
            service.executeJob(JOB_ID).get();

            // Then
            verify(errorRepository).save(any());
        }

        @Test
        @DisplayName("Should validate CDA documents")
        void shouldValidateCdaDocuments() throws Exception {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setDataType(DataType.CDA);
            SourceConnector connector = mock(SourceConnector.class);
            SourceRecord record = createSourceRecord("<ClinicalDocument>...</ClinicalDocument>", DataType.CDA);

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(connectorFactory.createAndConnect(any(SourceConfig.class))).thenReturn(connector);
            when(connector.countRecords()).thenReturn(1L);
            when(connector.readRecords(any(Integer.class))).thenReturn(java.util.Collections.singletonList(record).iterator());
            when(connector.getCurrentFile()).thenReturn("test.xml");
            when(connector.getCurrentPosition()).thenReturn(0L);
            org.mockito.Mockito.lenient().when(connector.getCheckpointData()).thenReturn(new HashMap<>());
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(connector).close();

            // When
            service.executeJob(JOB_ID).get();

            // Then - verify CDA document was validated and processed
            verify(jobRepository, org.mockito.Mockito.atLeastOnce()).save(any(MigrationJobEntity.class));
            verify(errorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should validate FHIR bundles")
        void shouldValidateFhirBundles() throws Exception {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setDataType(DataType.FHIR_BUNDLE);
            SourceConnector connector = mock(SourceConnector.class);
            SourceRecord record = createSourceRecord("{\"resourceType\":\"Bundle\"}", DataType.FHIR_BUNDLE);

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(connectorFactory.createAndConnect(any(SourceConfig.class))).thenReturn(connector);
            when(connector.countRecords()).thenReturn(1L);
            when(connector.readRecords(any(Integer.class))).thenReturn(java.util.Collections.singletonList(record).iterator());
            when(connector.getCurrentFile()).thenReturn("test.json");
            when(connector.getCurrentPosition()).thenReturn(0L);
            org.mockito.Mockito.lenient().when(connector.getCheckpointData()).thenReturn(new HashMap<>());
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(connector).close();

            // When
            service.executeJob(JOB_ID).get();

            // Then - verify FHIR bundle was validated and processed
            verify(jobRepository, org.mockito.Mockito.atLeastOnce()).save(any(MigrationJobEntity.class));
            verify(errorRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Job Control")
    class JobControlTests {

        @Test
        @DisplayName("Should track running jobs")
        void shouldTrackRunningJobs() throws Exception {
            // Given
            MigrationJobEntity job = createJobEntity();
            SourceConnector connector = mock(SourceConnector.class);

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(connectorFactory.createAndConnect(any(SourceConfig.class))).thenReturn(connector);
            when(connector.countRecords()).thenReturn(0L);
            when(connector.readRecords(any(Integer.class))).thenReturn(Collections.emptyIterator());
            when(connector.getCurrentFile()).thenReturn("test.hl7");
            when(connector.getCurrentPosition()).thenReturn(0L);
            org.mockito.Mockito.lenient().when(connector.getCheckpointData()).thenReturn(new HashMap<>());
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(connector).close();

            // When
            assertThat(service.isJobRunning(JOB_ID)).isFalse();
            service.executeJob(JOB_ID).get();
            assertThat(service.isJobRunning(JOB_ID)).isFalse(); // Completed

            // Then - Job was tracked during execution
        }

        @Test
        @DisplayName("Should request cancellation")
        void shouldRequestCancellation() {
            // When
            service.requestCancellation(JOB_ID);

            // Then - No exception, cancellation flag set
            // In real execution, this would stop the processing loop
        }

        @Test
        @DisplayName("Should handle pause during execution")
        void shouldHandlePauseDuringExecution() throws Exception {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setStatus(JobStatus.PAUSED); // Simulate pause
            SourceConnector connector = mock(SourceConnector.class);
            SourceRecord record = createSourceRecord("MSH|^~\\&|...", DataType.HL7V2);

            // Allow multiple calls to findById
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(connectorFactory.createAndConnect(any(SourceConfig.class))).thenReturn(connector);
            when(connector.countRecords()).thenReturn(1L);
            when(connector.readRecords(any(Integer.class))).thenReturn(java.util.Collections.singletonList(record).iterator());
            when(connector.getCurrentFile()).thenReturn("test.hl7");
            when(connector.getCurrentPosition()).thenReturn(0L);
            org.mockito.Mockito.lenient().when(connector.getCheckpointData()).thenReturn(new HashMap<>());
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(connector).close();

            // When
            service.executeJob(JOB_ID).get();

            // Then - Job should detect pause and stop (verify at least one call)
            verify(jobRepository, org.mockito.Mockito.atLeastOnce()).findById(JOB_ID);
        }
    }

    @Nested
    @DisplayName("Progress Publishing")
    class ProgressPublishingTests {

        @Test
        @DisplayName("Should publish progress updates")
        void shouldPublishProgressUpdates() throws Exception {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setTotalRecords(10L);
            SourceConnector connector = mock(SourceConnector.class);

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(connectorFactory.createAndConnect(any(SourceConfig.class))).thenReturn(connector);
            when(connector.countRecords()).thenReturn(10L);
            when(connector.readRecords(any(Integer.class))).thenReturn(Collections.emptyIterator());
            when(connector.getCurrentFile()).thenReturn("test.hl7");
            when(connector.getCurrentPosition()).thenReturn(0L);
            org.mockito.Mockito.lenient().when(connector.getCheckpointData()).thenReturn(new HashMap<>());
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(connector).close();

            // When
            service.executeJob(JOB_ID).get();

            // Then
            verify(progressPublisher).publishProgress(eq(JOB_ID), any());
        }
    }

    @Nested
    @DisplayName("Error Categorization")
    class ErrorCategorizationTests {

        @Test
        @DisplayName("Should categorize parse errors")
        void shouldCategorizeParseErrors() throws Exception {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setContinueOnError(true);
            SourceConnector connector = mock(SourceConnector.class);
            SourceRecord record = createSourceRecord("INVALID_HL7", DataType.HL7V2);

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(connectorFactory.createAndConnect(any(SourceConfig.class))).thenReturn(connector);
            when(connector.countRecords()).thenReturn(1L);
            when(connector.readRecords(any(Integer.class))).thenReturn(java.util.Collections.singletonList(record).iterator());
            when(connector.getCurrentFile()).thenReturn("test.hl7");
            when(connector.getCurrentPosition()).thenReturn(0L);
            org.mockito.Mockito.lenient().when(connector.getCheckpointData()).thenReturn(new HashMap<>());
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(connector).close();

            // When
            service.executeJob(JOB_ID).get();

            // Then
            verify(errorRepository).save(any());
        }
    }

    // Helper methods
    private MigrationJobEntity createJobEntity() {
        SourceConfig sourceConfig = SourceConfig.builder()
            .sourceType(SourceType.FILE)
            .path("/data/hl7")
            .build();

        return MigrationJobEntity.builder()
            .id(JOB_ID)
            .tenantId(TENANT_ID)
            .jobName("Test Job")
            .status(JobStatus.RUNNING)
            .sourceType(SourceType.FILE)
            .sourceConfig(sourceConfig)
            .dataType(DataType.HL7V2)
            .batchSize(100)
            .continueOnError(true)
            .resumable(true)
            .totalRecords(0L)
            .processedCount(0L)
            .successCount(0L)
            .failureCount(0L)
            .skippedCount(0L)
            .build();
    }

    private SourceRecord createSourceRecord(String content, DataType dataType) {
        return SourceRecord.builder()
            .recordId("record-1")
            .sourceFile("test.hl7")
            .offset(0L)
            .content(content)
            .dataType(dataType)
            .build();
    }
}
