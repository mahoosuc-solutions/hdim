package com.healthdata.qrda.service;

import com.healthdata.qrda.client.QualityMeasureClient;
import com.healthdata.qrda.dto.QrdaExportRequest;
import com.healthdata.qrda.persistence.QrdaExportJobEntity;
import com.healthdata.qrda.persistence.QrdaExportJobEntity.QrdaJobStatus;
import com.healthdata.qrda.persistence.QrdaExportJobEntity.QrdaJobType;
import com.healthdata.qrda.persistence.QrdaExportJobRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QrdaCategoryIIIService.
 * Tests QRDA Category III (aggregate) document generation.
 */
@ExtendWith(MockitoExtension.class)
class QrdaCategoryIIIServiceTest {

    @Mock
    private QrdaExportJobRepository jobRepository;

    @Mock
    private CdaDocumentBuilder cdaDocumentBuilder;

    @Mock
    private QrdaValidationService validationService;

    @Mock
    private QualityMeasureClient qualityMeasureClient;

    @InjectMocks
    private QrdaCategoryIIIService categoryIIIService;

    @Captor
    private ArgumentCaptor<QrdaExportJobEntity> jobCaptor;

    @TempDir
    Path tempDir;

    private static final String TENANT_ID = "test-tenant";
    private static final String REQUESTED_BY = "testuser";
    private static final UUID JOB_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(categoryIIIService, "storagePath", tempDir.toString());
    }

    @Nested
    @DisplayName("initiateExport() tests")
    class InitiateExportTests {

        @Test
        @DisplayName("Should create job with PENDING status")
        void initiateExport_validRequest_createsPendingJob() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> {
                QrdaExportJobEntity job = inv.getArgument(0);
                job.setId(JOB_ID);
                return job;
            });

            // Act
            QrdaExportJobEntity result = categoryIIIService.initiateExport(TENANT_ID, request, REQUESTED_BY);

            // Assert
            assertThat(result).isNotNull();
            verify(jobRepository).save(jobCaptor.capture());
            QrdaExportJobEntity savedJob = jobCaptor.getValue();
            assertThat(savedJob.getStatus()).isEqualTo(QrdaJobStatus.PENDING);
            assertThat(savedJob.getJobType()).isEqualTo(QrdaJobType.QRDA_III);
        }

        @Test
        @DisplayName("Should set tenant ID on created job")
        void initiateExport_validRequest_setsTenantId() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> {
                QrdaExportJobEntity job = inv.getArgument(0);
                job.setId(JOB_ID);
                return job;
            });

            // Act
            categoryIIIService.initiateExport(TENANT_ID, request, REQUESTED_BY);

            // Assert
            verify(jobRepository).save(jobCaptor.capture());
            assertThat(jobCaptor.getValue().getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("Should NOT set patient IDs for Category III")
        void initiateExport_categoryIII_doesNotSetPatientIds() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> {
                QrdaExportJobEntity job = inv.getArgument(0);
                job.setId(JOB_ID);
                return job;
            });

            // Act
            categoryIIIService.initiateExport(TENANT_ID, request, REQUESTED_BY);

            // Assert - Category III is aggregate, no patient IDs
            verify(jobRepository).save(jobCaptor.capture());
            assertThat(jobCaptor.getValue().getPatientIds()).isNull();
        }
    }

    @Nested
    @DisplayName("processExportAsync() tests")
    class ProcessExportAsyncTests {

        @Test
        @DisplayName("Should update job status to RUNNING when starting")
        void processExportAsync_startsJob_updatesStatusToRunning() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            request.setValidateDocuments(false);
            QrdaExportJobEntity job = createPendingJob();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

            // Track status at each save call since job is mutated in place
            List<QrdaJobStatus> capturedStatuses = new java.util.ArrayList<>();
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> {
                QrdaExportJobEntity savedJob = inv.getArgument(0);
                capturedStatuses.add(savedJob.getStatus());
                return savedJob;
            });
            when(qualityMeasureClient.getAggregateResults(any(), anyList(), any(), any()))
                .thenReturn(List.of());
            when(cdaDocumentBuilder.buildQrdaCategoryIIIWithData(any(), any(), anyList()))
                .thenReturn("<ClinicalDocument></ClinicalDocument>");

            // Act
            categoryIIIService.processExportAsync(JOB_ID, request);

            // Assert - RUNNING should be one of the statuses captured during processing
            assertThat(capturedStatuses).contains(QrdaJobStatus.RUNNING);
        }

        @Test
        @DisplayName("Should handle job not found gracefully")
        void processExportAsync_jobNotFound_handledGracefully() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            UUID randomJobId = UUID.randomUUID();
            when(jobRepository.findById(randomJobId)).thenReturn(Optional.empty());

            // Act - Should not throw exception (method handles it internally)
            categoryIIIService.processExportAsync(randomJobId, request);

            // Assert - Method catches exception, tries to update non-existent job
            // Since job doesn't exist, no save should occur
            verify(jobRepository, never()).save(any(QrdaExportJobEntity.class));
        }

        @Test
        @DisplayName("Should fetch aggregate results from quality measure client")
        void processExportAsync_fetchesAggregateResults() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            request.setValidateDocuments(false);
            QrdaExportJobEntity job = createPendingJob();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(qualityMeasureClient.getAggregateResults(any(), anyList(), any(), any()))
                .thenReturn(List.of());
            when(cdaDocumentBuilder.buildQrdaCategoryIIIWithData(any(), any(), anyList()))
                .thenReturn("<ClinicalDocument></ClinicalDocument>");

            // Act
            categoryIIIService.processExportAsync(JOB_ID, request);

            // Assert
            verify(qualityMeasureClient).getAggregateResults(
                eq(TENANT_ID),
                eq(request.getMeasureIds()),
                eq(request.getPeriodStart()),
                eq(request.getPeriodEnd())
            );
        }

        @Test
        @DisplayName("Should call CDA document builder with data")
        void processExportAsync_callsCdaBuilder() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            request.setValidateDocuments(false);
            QrdaExportJobEntity job = createPendingJob();
            List<QualityMeasureClient.MeasureAggregateDTO> measureResults = List.of();

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(qualityMeasureClient.getAggregateResults(any(), anyList(), any(), any()))
                .thenReturn(measureResults);
            when(cdaDocumentBuilder.buildQrdaCategoryIIIWithData(any(), any(), anyList()))
                .thenReturn("<ClinicalDocument></ClinicalDocument>");

            // Act
            categoryIIIService.processExportAsync(JOB_ID, request);

            // Assert
            verify(cdaDocumentBuilder).buildQrdaCategoryIIIWithData(
                eq(request.getPeriodStart()),
                eq(request.getPeriodEnd()),
                eq(measureResults)
            );
        }

        @Test
        @DisplayName("Should set document location after successful generation")
        void processExportAsync_success_setsDocumentLocation() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            request.setValidateDocuments(false);
            QrdaExportJobEntity job = createPendingJob();

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(qualityMeasureClient.getAggregateResults(any(), anyList(), any(), any()))
                .thenReturn(List.of());
            when(cdaDocumentBuilder.buildQrdaCategoryIIIWithData(any(), any(), anyList()))
                .thenReturn("<ClinicalDocument></ClinicalDocument>");

            // Act
            categoryIIIService.processExportAsync(JOB_ID, request);

            // Assert
            verify(jobRepository, atLeast(1)).save(jobCaptor.capture());
            List<QrdaExportJobEntity> savedJobs = jobCaptor.getAllValues();
            assertThat(savedJobs).anyMatch(j ->
                j.getDocumentLocation() != null &&
                j.getDocumentLocation().contains("qrda-iii"));
        }

        @Test
        @DisplayName("Should set document count to 1 for Category III")
        void processExportAsync_success_setsDocumentCountToOne() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            request.setValidateDocuments(false);
            QrdaExportJobEntity job = createPendingJob();

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(qualityMeasureClient.getAggregateResults(any(), anyList(), any(), any()))
                .thenReturn(List.of());
            when(cdaDocumentBuilder.buildQrdaCategoryIIIWithData(any(), any(), anyList()))
                .thenReturn("<ClinicalDocument></ClinicalDocument>");

            // Act
            categoryIIIService.processExportAsync(JOB_ID, request);

            // Assert
            verify(jobRepository, atLeast(1)).save(jobCaptor.capture());
            List<QrdaExportJobEntity> savedJobs = jobCaptor.getAllValues();
            assertThat(savedJobs).anyMatch(j ->
                j.getStatus() == QrdaJobStatus.COMPLETED &&
                j.getDocumentCount() != null &&
                j.getDocumentCount() == 1);
        }

        @Test
        @DisplayName("Should update status to FAILED on error")
        void processExportAsync_onError_updatesStatusToFailed() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            QrdaExportJobEntity job = createPendingJob();

            when(jobRepository.findById(JOB_ID))
                .thenReturn(Optional.of(job))
                .thenReturn(Optional.of(job));
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(qualityMeasureClient.getAggregateResults(any(), anyList(), any(), any()))
                .thenThrow(new RuntimeException("Service unavailable"));

            // Act
            categoryIIIService.processExportAsync(JOB_ID, request);

            // Assert
            verify(jobRepository, atLeast(1)).save(jobCaptor.capture());
            List<QrdaExportJobEntity> savedJobs = jobCaptor.getAllValues();
            assertThat(savedJobs).anyMatch(j -> j.getStatus() == QrdaJobStatus.FAILED);
        }

        @Test
        @DisplayName("Should set error message on failure")
        void processExportAsync_onError_setsErrorMessage() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            QrdaExportJobEntity job = createPendingJob();

            when(jobRepository.findById(JOB_ID))
                .thenReturn(Optional.of(job))
                .thenReturn(Optional.of(job));
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(qualityMeasureClient.getAggregateResults(any(), anyList(), any(), any()))
                .thenThrow(new RuntimeException("Service unavailable"));

            // Act
            categoryIIIService.processExportAsync(JOB_ID, request);

            // Assert
            verify(jobRepository, atLeast(1)).save(jobCaptor.capture());
            List<QrdaExportJobEntity> savedJobs = jobCaptor.getAllValues();
            assertThat(savedJobs).anyMatch(j ->
                j.getStatus() == QrdaJobStatus.FAILED &&
                j.getErrorMessage() != null);
        }
    }

    @Nested
    @DisplayName("calculateAggregateResults() tests")
    class CalculateAggregateResultsTests {

        @Test
        @DisplayName("Should return result with measure ID")
        void calculateAggregateResults_returnsResultWithMeasureId() {
            // Arrange
            String measureId = "CMS125v12";
            QrdaExportRequest request = createValidRequest();

            // Act
            QrdaCategoryIIIService.MeasureAggregateResult result =
                categoryIIIService.calculateAggregateResults(TENANT_ID, measureId, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getMeasureId()).isEqualTo(measureId);
        }
    }

    @Nested
    @DisplayName("MeasureAggregateResult tests")
    class MeasureAggregateResultTests {

        @Test
        @DisplayName("Should calculate performance rate correctly")
        void getPerformanceRate_calculatesCorrectly() {
            // Arrange
            QrdaCategoryIIIService.MeasureAggregateResult result = QrdaCategoryIIIService.MeasureAggregateResult.builder()
                .measureId("CMS125v12")
                .initialPopulation(100)
                .denominator(90)
                .numerator(72)
                .denominatorExclusions(5)
                .denominatorExceptions(5)
                .build();

            // Act - Eligible = 90 - 5 - 5 = 80, Rate = 72/80 = 90%
            double rate = result.getPerformanceRate();

            // Assert
            assertThat(rate).isEqualTo(90.0);
        }

        @Test
        @DisplayName("Should return 0 when eligible population is zero")
        void getPerformanceRate_zeroEligible_returnsZero() {
            // Arrange
            QrdaCategoryIIIService.MeasureAggregateResult result = QrdaCategoryIIIService.MeasureAggregateResult.builder()
                .measureId("CMS125v12")
                .denominator(10)
                .denominatorExclusions(5)
                .denominatorExceptions(5)
                .numerator(0)
                .build();

            // Act - Eligible = 10 - 5 - 5 = 0
            double rate = result.getPerformanceRate();

            // Assert
            assertThat(rate).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0 when denominator is zero")
        void getPerformanceRate_zeroDenominator_returnsZero() {
            // Arrange
            QrdaCategoryIIIService.MeasureAggregateResult result = QrdaCategoryIIIService.MeasureAggregateResult.builder()
                .measureId("CMS125v12")
                .denominator(0)
                .numerator(0)
                .build();

            // Act
            double rate = result.getPerformanceRate();

            // Assert
            assertThat(rate).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle negative eligible population")
        void getPerformanceRate_negativeEligible_returnsZero() {
            // Arrange - Edge case: exclusions > denominator
            QrdaCategoryIIIService.MeasureAggregateResult result = QrdaCategoryIIIService.MeasureAggregateResult.builder()
                .measureId("CMS125v12")
                .denominator(10)
                .denominatorExclusions(15) // More exclusions than denominator
                .numerator(5)
                .build();

            // Act
            double rate = result.getPerformanceRate();

            // Assert
            assertThat(rate).isEqualTo(0.0);
        }
    }

    // Helper methods

    private QrdaExportRequest createValidRequest() {
        return QrdaExportRequest.builder()
            .jobType(QrdaJobType.QRDA_III)
            .measureIds(List.of("CMS125v12", "CMS130v11"))
            .periodStart(LocalDate.of(2024, 1, 1))
            .periodEnd(LocalDate.of(2024, 12, 31))
            .validateDocuments(false)
            .build();
    }

    private QrdaExportJobEntity createPendingJob() {
        return QrdaExportJobEntity.builder()
            .id(JOB_ID)
            .tenantId(TENANT_ID)
            .jobType(QrdaJobType.QRDA_III)
            .status(QrdaJobStatus.PENDING)
            .measureIds(List.of("CMS125v12", "CMS130v11"))
            .periodStart(LocalDate.of(2024, 1, 1))
            .periodEnd(LocalDate.of(2024, 12, 31))
            .requestedBy(REQUESTED_BY)
            .build();
    }
}
