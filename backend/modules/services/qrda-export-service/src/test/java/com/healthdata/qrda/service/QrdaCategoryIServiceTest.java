package com.healthdata.qrda.service;

import com.healthdata.qrda.dto.QrdaExportRequest;
import com.healthdata.qrda.persistence.QrdaExportJobEntity;
import com.healthdata.qrda.persistence.QrdaExportJobEntity.QrdaJobStatus;
import com.healthdata.qrda.persistence.QrdaExportJobEntity.QrdaJobType;
import com.healthdata.qrda.persistence.QrdaExportJobRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QrdaCategoryIService.
 * Tests QRDA Category I (patient-level) document generation.
 */
@ExtendWith(MockitoExtension.class)
class QrdaCategoryIServiceTest {

    @Mock
    private QrdaExportJobRepository jobRepository;

    @Mock
    private CdaDocumentBuilder cdaDocumentBuilder;

    @Mock
    private QrdaValidationService validationService;

    @InjectMocks
    private QrdaCategoryIService categoryIService;

    @Captor
    private ArgumentCaptor<QrdaExportJobEntity> jobCaptor;

    private static final String TENANT_ID = "test-tenant";
    private static final String REQUESTED_BY = "testuser";
    private static final UUID JOB_ID = UUID.randomUUID();

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
            QrdaExportJobEntity result = categoryIService.initiateExport(TENANT_ID, request, REQUESTED_BY);

            // Assert
            assertThat(result).isNotNull();
            verify(jobRepository).save(jobCaptor.capture());
            QrdaExportJobEntity savedJob = jobCaptor.getValue();
            assertThat(savedJob.getStatus()).isEqualTo(QrdaJobStatus.PENDING);
            assertThat(savedJob.getJobType()).isEqualTo(QrdaJobType.QRDA_I);
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
            categoryIService.initiateExport(TENANT_ID, request, REQUESTED_BY);

            // Assert
            verify(jobRepository).save(jobCaptor.capture());
            assertThat(jobCaptor.getValue().getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("Should set measure IDs on created job")
        void initiateExport_validRequest_setsMeasureIds() {
            // Arrange
            List<String> measureIds = List.of("CMS125v12", "CMS130v11");
            QrdaExportRequest request = createRequestWithMeasures(measureIds);
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> {
                QrdaExportJobEntity job = inv.getArgument(0);
                job.setId(JOB_ID);
                return job;
            });

            // Act
            categoryIService.initiateExport(TENANT_ID, request, REQUESTED_BY);

            // Assert
            verify(jobRepository).save(jobCaptor.capture());
            assertThat(jobCaptor.getValue().getMeasureIds()).containsExactlyElementsOf(measureIds);
        }

        @Test
        @DisplayName("Should set patient IDs when provided")
        void initiateExport_withPatientIds_setsPatientIds() {
            // Arrange
            List<UUID> patientIds = List.of(UUID.randomUUID(), UUID.randomUUID());
            QrdaExportRequest request = createRequestWithPatients(patientIds);
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> {
                QrdaExportJobEntity job = inv.getArgument(0);
                job.setId(JOB_ID);
                return job;
            });

            // Act
            categoryIService.initiateExport(TENANT_ID, request, REQUESTED_BY);

            // Assert
            verify(jobRepository).save(jobCaptor.capture());
            assertThat(jobCaptor.getValue().getPatientIds()).hasSize(2);
        }

        @Test
        @DisplayName("Should set period dates on created job")
        void initiateExport_validRequest_setsPeriodDates() {
            // Arrange
            LocalDate periodStart = LocalDate.of(2024, 1, 1);
            LocalDate periodEnd = LocalDate.of(2024, 12, 31);
            QrdaExportRequest request = createRequestWithDates(periodStart, periodEnd);
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> {
                QrdaExportJobEntity job = inv.getArgument(0);
                job.setId(JOB_ID);
                return job;
            });

            // Act
            categoryIService.initiateExport(TENANT_ID, request, REQUESTED_BY);

            // Assert
            verify(jobRepository).save(jobCaptor.capture());
            assertThat(jobCaptor.getValue().getPeriodStart()).isEqualTo(periodStart);
            assertThat(jobCaptor.getValue().getPeriodEnd()).isEqualTo(periodEnd);
        }

        @Test
        @DisplayName("Should set requestedBy on created job")
        void initiateExport_validRequest_setsRequestedBy() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> {
                QrdaExportJobEntity job = inv.getArgument(0);
                job.setId(JOB_ID);
                return job;
            });

            // Act
            categoryIService.initiateExport(TENANT_ID, request, "specific-user");

            // Assert
            verify(jobRepository).save(jobCaptor.capture());
            assertThat(jobCaptor.getValue().getRequestedBy()).isEqualTo("specific-user");
        }

        @Test
        @DisplayName("Should return saved job entity")
        void initiateExport_validRequest_returnsSavedJob() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            QrdaExportJobEntity savedJob = QrdaExportJobEntity.builder()
                .id(JOB_ID)
                .tenantId(TENANT_ID)
                .jobType(QrdaJobType.QRDA_I)
                .status(QrdaJobStatus.PENDING)
                .build();
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenReturn(savedJob);

            // Act
            QrdaExportJobEntity result = categoryIService.initiateExport(TENANT_ID, request, REQUESTED_BY);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(JOB_ID);
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
            QrdaExportJobEntity job = createPendingJob();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

            // Track status at each save call since job is mutated in place
            List<QrdaJobStatus> capturedStatuses = new java.util.ArrayList<>();
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> {
                QrdaExportJobEntity savedJob = inv.getArgument(0);
                capturedStatuses.add(savedJob.getStatus());
                return savedJob;
            });

            // Act
            categoryIService.processExportAsync(JOB_ID, request);

            // Assert - RUNNING should be one of the statuses captured during processing
            assertThat(capturedStatuses).contains(QrdaJobStatus.RUNNING);
        }

        @Test
        @DisplayName("Should set startedAt timestamp when starting")
        void processExportAsync_startsJob_setsStartedAt() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            QrdaExportJobEntity job = createPendingJob();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            categoryIService.processExportAsync(JOB_ID, request);

            // Assert
            verify(jobRepository, atLeast(1)).save(jobCaptor.capture());
            List<QrdaExportJobEntity> savedJobs = jobCaptor.getAllValues();
            assertThat(savedJobs).anyMatch(j -> j.getStartedAt() != null);
        }

        @Test
        @DisplayName("Should handle job not found gracefully")
        void processExportAsync_jobNotFound_handledGracefully() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            UUID randomJobId = UUID.randomUUID();
            when(jobRepository.findById(randomJobId)).thenReturn(Optional.empty());

            // Act - Should not throw exception (method handles it internally)
            categoryIService.processExportAsync(randomJobId, request);

            // Assert - Method catches exception, tries to update non-existent job
            // Since job doesn't exist, no save should occur
            verify(jobRepository, never()).save(any(QrdaExportJobEntity.class));
        }

        @Test
        @DisplayName("Should update status to COMPLETED on success")
        void processExportAsync_success_updatesStatusToCompleted() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            request.setValidateDocuments(false);
            QrdaExportJobEntity job = createPendingJob();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            categoryIService.processExportAsync(JOB_ID, request);

            // Assert
            verify(jobRepository, atLeast(1)).save(jobCaptor.capture());
            List<QrdaExportJobEntity> savedJobs = jobCaptor.getAllValues();
            assertThat(savedJobs).anyMatch(j -> j.getStatus() == QrdaJobStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should set completedAt timestamp on completion")
        void processExportAsync_success_setsCompletedAt() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            request.setValidateDocuments(false);
            QrdaExportJobEntity job = createPendingJob();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            categoryIService.processExportAsync(JOB_ID, request);

            // Assert
            verify(jobRepository, atLeast(1)).save(jobCaptor.capture());
            List<QrdaExportJobEntity> savedJobs = jobCaptor.getAllValues();
            assertThat(savedJobs).anyMatch(j -> j.getCompletedAt() != null && j.getStatus() == QrdaJobStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should call validation when validateDocuments is true")
        void processExportAsync_validationEnabled_callsValidation() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            request.setValidateDocuments(true);
            QrdaExportJobEntity job = createPendingJob();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(validationService.validateCategoryI(any())).thenReturn(List.of());

            // Act
            categoryIService.processExportAsync(JOB_ID, request);

            // Assert
            verify(validationService).validateCategoryI(any());
        }

        @Test
        @DisplayName("Should set validation errors when validation fails")
        void processExportAsync_validationFails_setsValidationErrors() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            request.setValidateDocuments(true);
            QrdaExportJobEntity job = createPendingJob();
            List<String> validationErrors = List.of("Error 1", "Error 2");
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(validationService.validateCategoryI(any())).thenReturn(validationErrors);

            // Act
            categoryIService.processExportAsync(JOB_ID, request);

            // Assert
            verify(jobRepository, atLeast(1)).save(jobCaptor.capture());
            List<QrdaExportJobEntity> savedJobs = jobCaptor.getAllValues();
            assertThat(savedJobs).anyMatch(j -> j.getValidationErrors() != null && j.getValidationErrors().size() == 2);
        }
    }

    // Helper methods

    private QrdaExportRequest createValidRequest() {
        return QrdaExportRequest.builder()
            .jobType(QrdaJobType.QRDA_I)
            .measureIds(List.of("CMS125v12"))
            .periodStart(LocalDate.of(2024, 1, 1))
            .periodEnd(LocalDate.of(2024, 12, 31))
            .validateDocuments(false)
            .build();
    }

    private QrdaExportRequest createRequestWithMeasures(List<String> measureIds) {
        QrdaExportRequest request = createValidRequest();
        request.setMeasureIds(measureIds);
        return request;
    }

    private QrdaExportRequest createRequestWithPatients(List<UUID> patientIds) {
        QrdaExportRequest request = createValidRequest();
        request.setPatientIds(patientIds);
        return request;
    }

    private QrdaExportRequest createRequestWithDates(LocalDate start, LocalDate end) {
        QrdaExportRequest request = createValidRequest();
        request.setPeriodStart(start);
        request.setPeriodEnd(end);
        return request;
    }

    private QrdaExportJobEntity createPendingJob() {
        return QrdaExportJobEntity.builder()
            .id(JOB_ID)
            .tenantId(TENANT_ID)
            .jobType(QrdaJobType.QRDA_I)
            .status(QrdaJobStatus.PENDING)
            .measureIds(List.of("CMS125v12"))
            .periodStart(LocalDate.of(2024, 1, 1))
            .periodEnd(LocalDate.of(2024, 12, 31))
            .requestedBy(REQUESTED_BY)
            .build();
    }
}
