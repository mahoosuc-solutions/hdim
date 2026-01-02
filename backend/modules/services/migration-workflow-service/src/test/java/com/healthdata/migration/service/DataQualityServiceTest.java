package com.healthdata.migration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.healthdata.migration.dto.DataQualityReport;
import com.healthdata.migration.dto.DataType;
import com.healthdata.migration.dto.JobStatus;
import com.healthdata.migration.dto.MigrationErrorCategory;
import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceType;
import com.healthdata.migration.persistence.MigrationErrorEntity;
import com.healthdata.migration.persistence.MigrationJobEntity;
import com.healthdata.migration.repository.MigrationErrorRepository;
import com.healthdata.migration.repository.MigrationJobRepository;

/**
 * Unit tests for DataQualityService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataQualityService")
class DataQualityServiceTest {

    @Mock
    private MigrationJobRepository jobRepository;

    @Mock
    private MigrationErrorRepository errorRepository;

    @InjectMocks
    private DataQualityService service;

    private static final UUID JOB_ID = UUID.randomUUID();
    private static final String TENANT_ID = "test-tenant";

    @Nested
    @DisplayName("Generate Report")
    class GenerateReportTests {

        @Test
        @DisplayName("Should generate report for completed job")
        void shouldGenerateReportForCompletedJob() {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setStatus(JobStatus.COMPLETED);
            job.setTotalRecords(1000L);
            job.setProcessedCount(1000L);
            job.setSuccessCount(980L);
            job.setFailureCount(20L);
            job.setStartedAt(Instant.now().minusSeconds(100));
            job.setCompletedAt(Instant.now());

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(List.of());
            when(errorRepository.findTopErrorsByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(List.of());

            // When
            DataQualityReport report = service.generateReport(JOB_ID);

            // Then
            assertThat(report).isNotNull();
            assertThat(report.getJobId()).isEqualTo(JOB_ID);
            assertThat(report.getJobName()).isEqualTo("Test Job");
            assertThat(report.getSummary()).isNotNull();
            assertThat(report.getSummary().getTotalRecords()).isEqualTo(1000L);
            assertThat(report.getSummary().getSuccessCount()).isEqualTo(980L);
            assertThat(report.getSummary().getFailureCount()).isEqualTo(20L);
            assertThat(report.getQualityScore()).isGreaterThanOrEqualTo(0.0);
        }

        @Test
        @DisplayName("Should throw exception when job not found")
        void shouldThrowExceptionWhenJobNotFound() {
            // Given
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.generateReport(JOB_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Job not found");
        }

        @Test
        @DisplayName("Should calculate success rate correctly")
        void shouldCalculateSuccessRateCorrectly() {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setTotalRecords(100L);
            job.setProcessedCount(100L);
            job.setSuccessCount(95L);
            job.setFailureCount(5L);
            job.setStartedAt(Instant.now().minusSeconds(10));
            job.setCompletedAt(Instant.now());

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(List.of());
            when(errorRepository.findTopErrorsByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(List.of());

            // When
            DataQualityReport report = service.generateReport(JOB_ID);

            // Then
            assertThat(report.getSummary().getSuccessRate()).isEqualTo(95.0);
            assertThat(report.getSummary().getFailureRate()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("Should include error breakdown by category")
        void shouldIncludeErrorBreakdown() {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setTotalRecords(100L);
            job.setSuccessCount(90L);
            job.setFailureCount(10L);

            List<Object[]> errorCounts = java.util.Arrays.<Object[]>asList(
                new Object[]{MigrationErrorCategory.PARSE_ERROR, 5L},
                new Object[]{MigrationErrorCategory.VALIDATION_ERROR, 3L},
                new Object[]{MigrationErrorCategory.MAPPING_ERROR, 2L}
            );

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(errorCounts);
            when(errorRepository.findTopErrorsByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(List.of());

            // When
            DataQualityReport report = service.generateReport(JOB_ID);

            // Then
            assertThat(report.getErrorsByCategory()).isNotNull();
            assertThat(report.getErrorsByCategory().get(MigrationErrorCategory.PARSE_ERROR)).isEqualTo(5L);
            assertThat(report.getErrorsByCategory().get(MigrationErrorCategory.VALIDATION_ERROR)).isEqualTo(3L);
            assertThat(report.getErrorsByCategory().get(MigrationErrorCategory.MAPPING_ERROR)).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("Data Quality Issues")
    class DataQualityIssuesTests {

        @Test
        @DisplayName("Should detect high parse error rate")
        void shouldDetectHighParseErrorRate() {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setTotalRecords(100L);
            job.setSuccessCount(70L);
            job.setFailureCount(30L);

            List<Object[]> errorCounts = java.util.Arrays.<Object[]>asList(
                new Object[]{MigrationErrorCategory.PARSE_ERROR, 25L}
            );

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(errorCounts);
            when(errorRepository.findTopErrorsByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(List.of());

            // When
            DataQualityReport report = service.generateReport(JOB_ID);

            // Then
            assertThat(report.getDataQualityIssues()).isNotEmpty();
            boolean hasParseErrorIssue = report.getDataQualityIssues().stream()
                .anyMatch(issue -> issue.getField().equals("Source Data"));
            assertThat(hasParseErrorIssue).isTrue();
        }

        @Test
        @DisplayName("Should detect validation errors")
        void shouldDetectValidationErrors() {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setTotalRecords(100L);
            job.setSuccessCount(90L);
            job.setFailureCount(10L);

            List<Object[]> errorCounts = java.util.Arrays.<Object[]>asList(
                new Object[]{MigrationErrorCategory.VALIDATION_ERROR, 10L}
            );

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(errorCounts);
            when(errorRepository.findTopErrorsByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(List.of());

            // When
            DataQualityReport report = service.generateReport(JOB_ID);

            // Then
            assertThat(report.getDataQualityIssues()).isNotEmpty();
            boolean hasValidationIssue = report.getDataQualityIssues().stream()
                .anyMatch(issue -> issue.getField().equals("FHIR Validation"));
            assertThat(hasValidationIssue).isTrue();
        }

        @Test
        @DisplayName("Should detect missing required fields")
        void shouldDetectMissingRequiredFields() {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setTotalRecords(100L);
            job.setSuccessCount(95L);
            job.setFailureCount(5L);

            List<Object[]> errorCounts = java.util.Arrays.<Object[]>asList(
                new Object[]{MigrationErrorCategory.MISSING_REQUIRED, 5L}
            );

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(errorCounts);
            when(errorRepository.findTopErrorsByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(List.of());

            // When
            DataQualityReport report = service.generateReport(JOB_ID);

            // Then
            assertThat(report.getDataQualityIssues()).isNotEmpty();
            boolean hasMissingFieldsIssue = report.getDataQualityIssues().stream()
                .anyMatch(issue -> issue.getField().equals("Required Fields"));
            assertThat(hasMissingFieldsIssue).isTrue();
        }

        @Test
        @DisplayName("Should detect high overall failure rate")
        void shouldDetectHighOverallFailureRate() {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setTotalRecords(100L);
            job.setSuccessCount(60L);
            job.setFailureCount(40L);

            List<Object[]> errorCounts = java.util.Arrays.<Object[]>asList(
                new Object[]{MigrationErrorCategory.PARSE_ERROR, 20L},
                new Object[]{MigrationErrorCategory.VALIDATION_ERROR, 20L}
            );

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(errorCounts);
            when(errorRepository.findTopErrorsByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(List.of());

            // When
            DataQualityReport report = service.generateReport(JOB_ID);

            // Then
            assertThat(report.getDataQualityIssues()).isNotEmpty();
            boolean hasOverallFailureIssue = report.getDataQualityIssues().stream()
                .anyMatch(issue -> issue.getField().equals("Overall"));
            assertThat(hasOverallFailureIssue).isTrue();
        }

        @Test
        @DisplayName("Should include mapping, code, and duplicate issues and sort by severity")
        void shouldIncludeAdditionalIssueTypes() {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setTotalRecords(100L);
            job.setFailureCount(25L);

            List<Object[]> errorCounts = java.util.Arrays.<Object[]>asList(
                new Object[]{MigrationErrorCategory.MAPPING_ERROR, 5L},
                new Object[]{MigrationErrorCategory.INVALID_CODE, 4L},
                new Object[]{MigrationErrorCategory.DUPLICATE_RECORD, 3L},
                new Object[]{MigrationErrorCategory.PARSE_ERROR, 30L}
            );

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(errorCounts);
            when(errorRepository.findTopErrorsByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(List.of());

            // When
            DataQualityReport report = service.generateReport(JOB_ID);

            // Then
            assertThat(report.getDataQualityIssues()).isNotEmpty();
            assertThat(report.getDataQualityIssues().get(0).getSeverity())
                .isIn(DataQualityReport.DataQualityIssue.Severity.ERROR,
                      DataQualityReport.DataQualityIssue.Severity.CRITICAL);
        }
    }

    @Nested
    @DisplayName("Error Operations")
    class ErrorOperationsTests {

        @Test
        @DisplayName("Should get errors by category")
        void shouldGetErrorsByCategory() {
            // Given
            List<Object[]> errorCounts = java.util.Arrays.<Object[]>asList(
                new Object[]{MigrationErrorCategory.PARSE_ERROR, 10L},
                new Object[]{MigrationErrorCategory.VALIDATION_ERROR, 5L}
            );

            when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(errorCounts);

            // When
            Map<MigrationErrorCategory, Long> result = service.getErrorsByCategory(JOB_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get(MigrationErrorCategory.PARSE_ERROR)).isEqualTo(10L);
            assertThat(result.get(MigrationErrorCategory.VALIDATION_ERROR)).isEqualTo(5L);
            assertThat(result.get(MigrationErrorCategory.MAPPING_ERROR)).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should get paginated errors")
        void shouldGetPaginatedErrors() {
            // Given
            List<MigrationErrorEntity> errors = List.of(
                createErrorEntity("Error 1"),
                createErrorEntity("Error 2")
            );
            Page<MigrationErrorEntity> page = new PageImpl<>(errors);

            when(errorRepository.findByJobIdWithFilters(
                eq(JOB_ID), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(page);

            // When
            Page<MigrationErrorEntity> result = service.getErrors(JOB_ID, null, null, 0, 10);

            // Then
            assertThat(result.getContent()).hasSize(2);
            verify(errorRepository).findByJobIdWithFilters(
                eq(JOB_ID), eq(null), eq(null), any(Pageable.class));
        }

        @Test
        @DisplayName("Should get recent errors")
        void shouldGetRecentErrors() {
            // Given
            List<MigrationErrorEntity> errors = List.of(
                createErrorEntity("Recent Error")
            );

            when(errorRepository.findRecentByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(errors);

            // When
            List<MigrationErrorEntity> result = service.getRecentErrors(JOB_ID, 10);

            // Then
            assertThat(result).hasSize(1);
            verify(errorRepository).findRecentByJobId(eq(JOB_ID), any(PageRequest.class));
        }
    }

    @Nested
    @DisplayName("CSV Export")
    class CsvExportTests {

        @Test
        @DisplayName("Should export errors to CSV")
        void shouldExportErrorsToCsv() throws Exception {
            // Given
            List<MigrationErrorEntity> errors = List.of(
                createErrorEntity("Error 1"),
                createErrorEntity("Error 2")
            );
            Page<MigrationErrorEntity> page = new PageImpl<>(errors);

            when(errorRepository.findByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(page);

            // When
            String csv = service.exportErrorsToCsv(JOB_ID);

            // Then
            assertThat(csv).isNotNull();
            assertThat(csv).contains("Record ID");
            assertThat(csv).contains("Source File");
            assertThat(csv).contains("Category");
            assertThat(csv).contains("Error 1");
            assertThat(csv).contains("Error 2");
        }

        @Test
        @DisplayName("Should write errors to CSV writer")
        void shouldWriteErrorsToCsvWriter() throws Exception {
            // Given
            List<MigrationErrorEntity> errors = List.of(
                createErrorEntity("Test Error")
            );
            Page<MigrationErrorEntity> page = new PageImpl<>(errors);

            when(errorRepository.findByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(page);

            StringWriter writer = new StringWriter();

            // When
            service.writeErrorsCsv(JOB_ID, writer);

            // Then
            String csv = writer.toString();
            assertThat(csv).contains("Record ID");
            assertThat(csv).contains("Test Error");
        }

        @Test
        @DisplayName("Should page through errors when exporting CSV")
        void shouldPageThroughErrorsWhenExportingCsv() throws Exception {
            List<MigrationErrorEntity> errors = List.of(
                createErrorEntity("Page 1 Error")
            );
            Page<MigrationErrorEntity> page1 = new PageImpl<>(
                errors, PageRequest.of(0, 1), 2);
            Page<MigrationErrorEntity> page2 = new PageImpl<>(
                List.of(), PageRequest.of(1, 1), 2);

            when(errorRepository.findByJobId(eq(JOB_ID), eq(PageRequest.of(0, 1000))))
                .thenReturn(page1);
            when(errorRepository.findByJobId(eq(JOB_ID), eq(PageRequest.of(1, 1000))))
                .thenReturn(page2);

            String csv = service.exportErrorsToCsv(JOB_ID);

            assertThat(csv).contains("Page 1 Error");
        }

        @Test
        @DisplayName("Should export quality report to CSV")
        void shouldExportQualityReportToCsv() {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setTotalRecords(100L);
            job.setSuccessCount(95L);
            job.setFailureCount(5L);

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(List.of());
            when(errorRepository.findTopErrorsByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(List.of());

            // When
            String csv = service.exportReportToCsv(JOB_ID);

            // Then
            assertThat(csv).isNotNull();
            assertThat(csv).contains("Data Quality Report");
            assertThat(csv).contains("Job ID");
            assertThat(csv).contains("Quality Score");
            assertThat(csv).contains("Statistics");
            assertThat(csv).contains("Total Records");
        }

        @Test
        @DisplayName("Should include non-zero error categories and FHIR resources in CSV")
        void shouldIncludeNonZeroErrorsAndFhirResourcesInCsv() {
            MigrationJobEntity job = createJobEntity();
            job.setTotalRecords(100L);
            job.setSuccessCount(95L);
            job.setFailureCount(5L);
            job.setFhirResourcesCreated(Map.of("Patient", 2L));

            List<Object[]> errorCounts = java.util.Arrays.<Object[]>asList(
                new Object[]{MigrationErrorCategory.PARSE_ERROR, 5L},
                new Object[]{MigrationErrorCategory.MAPPING_ERROR, 0L}
            );

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(errorCounts);
            when(errorRepository.findTopErrorsByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(List.of());

            String csv = service.exportReportToCsv(JOB_ID);

            assertThat(csv).contains("FHIR Resources Created");
            assertThat(csv).contains("Patient,2");
            assertThat(csv).contains("PARSE_ERROR,5");
            assertThat(csv).doesNotContain("MAPPING_ERROR,0");
        }

        @Test
        @DisplayName("Should escape CSV special characters")
        void shouldEscapeCsvSpecialCharacters() throws Exception {
            // Given
            MigrationErrorEntity error = createErrorEntity("Error with, comma and \"quote\"");
            error.setRecordIdentifier("ID,with,commas");
            Page<MigrationErrorEntity> page = new PageImpl<>(List.of(error));

            when(errorRepository.findByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(page);

            // When
            String csv = service.exportErrorsToCsv(JOB_ID);

            // Then
            assertThat(csv).contains("\"ID,with,commas\"");
            assertThat(csv).contains("\"Error with, comma and \"\"quote\"\"\"");
        }

        @Test
        @DisplayName("Should include top errors with samples and truncate long messages")
        void shouldIncludeTopErrorsWithSamples() {
            // Given
            MigrationJobEntity job = createJobEntity();
            String longMessage = "X".repeat(250);
            List<Object[]> topErrors = java.util.Arrays.<Object[]>asList(
                new Object[]{MigrationErrorCategory.PARSE_ERROR, longMessage, 5L}
            );

            MigrationErrorEntity sample = createErrorEntity("Sample Error");
            sample.setRecordIdentifier("rec-1");
            sample.setSourceFile("file1");

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(List.of());
            when(errorRepository.findTopErrorsByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(topErrors);
            when(errorRepository.findByJobIdWithFilters(eq(JOB_ID), eq(MigrationErrorCategory.PARSE_ERROR),
                    eq(longMessage), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(sample)));

            // When
            DataQualityReport report = service.generateReport(JOB_ID);

            // Then
            assertThat(report.getTopErrors()).hasSize(1);
            DataQualityReport.ErrorSample errorSample = report.getTopErrors().get(0);
            assertThat(errorSample.getSampleRecordId()).isEqualTo("rec-1");
            assertThat(errorSample.getSampleSourceFile()).isEqualTo("file1");
            assertThat(errorSample.getErrorMessage()).endsWith("...");
        }
    }

    @Nested
    @DisplayName("Quality Metrics")
    class QualityMetricsTests {

        @Test
        @DisplayName("Should calculate quality score")
        void shouldCalculateQualityScore() {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setTotalRecords(100L);
            job.setProcessedCount(100L);
            job.setSuccessCount(95L);
            job.setFailureCount(5L);

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(List.of());
            when(errorRepository.findTopErrorsByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(List.of());

            // When
            DataQualityReport report = service.generateReport(JOB_ID);

            // Then
            assertThat(report.getQualityScore()).isGreaterThan(0.0);
            assertThat(report.getQualityScore()).isLessThanOrEqualTo(100.0);
        }

        @Test
        @DisplayName("Should assign quality grade")
        void shouldAssignQualityGrade() {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setTotalRecords(100L);
            job.setProcessedCount(100L);
            job.setSuccessCount(98L);
            job.setFailureCount(2L);

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(List.of());
            when(errorRepository.findTopErrorsByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(List.of());

            // When
            DataQualityReport report = service.generateReport(JOB_ID);

            // Then
            assertThat(report.getQualityGrade()).isNotNull();
        }

        @Test
        @DisplayName("Should handle zero records case")
        void shouldHandleZeroRecords() {
            // Given
            MigrationJobEntity job = createJobEntity();
            job.setTotalRecords(0L);
            job.setProcessedCount(0L);
            job.setSuccessCount(0L);
            job.setFailureCount(0L);

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(List.of());
            when(errorRepository.findTopErrorsByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(List.of());

            // When
            DataQualityReport report = service.generateReport(JOB_ID);

            // Then
            assertThat(report).isNotNull();
            assertThat(report.getSummary().getSuccessRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should set average processing time to zero when no records processed")
        void shouldSetAvgProcessingTimeToZeroWhenNoProcessed() {
            MigrationJobEntity job = createJobEntity();
            job.setTotalRecords(10L);
            job.setProcessedCount(0L);
            job.setSuccessCount(0L);
            job.setFailureCount(0L);
            job.setStartedAt(Instant.now().minusSeconds(5));
            job.setCompletedAt(Instant.now());

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(List.of());
            when(errorRepository.findTopErrorsByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(List.of());

            DataQualityReport report = service.generateReport(JOB_ID);

            assertThat(report.getSummary().getAvgProcessingTimeMs()).isEqualTo(0.0);
        }
    }

    @Test
    @DisplayName("Should handle missing start time in summary")
    void shouldHandleMissingStartTime() {
        MigrationJobEntity job = createJobEntity();
        job.setStartedAt(null);
        job.setCompletedAt(Instant.now());
        job.setTotalRecords(10L);

        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(List.of());
        when(errorRepository.findTopErrorsByJobId(eq(JOB_ID), any(PageRequest.class)))
            .thenReturn(List.of());

        DataQualityReport report = service.generateReport(JOB_ID);

        assertThat(report.getSummary().getProcessingTimeMs()).isEqualTo(0L);
        assertThat(report.getSummary().getAvgProcessingTimeMs()).isEqualTo(0.0);
    }

    @Nested
    @DisplayName("Top Errors")
    class TopErrorsTests {

        @Test
        @DisplayName("Should get top errors")
        void shouldGetTopErrors() {
            // Given
            MigrationJobEntity job = createJobEntity();

            List<Object[]> topErrorResults = java.util.Arrays.asList(
                new Object[]{MigrationErrorCategory.PARSE_ERROR, "Invalid HL7 message", 10L},
                new Object[]{MigrationErrorCategory.VALIDATION_ERROR, "Missing patient ID", 5L}
            );

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID)).thenReturn(List.of());
            when(errorRepository.findTopErrorsByJobId(eq(JOB_ID), any(PageRequest.class)))
                .thenReturn(topErrorResults);
            when(errorRepository.findByJobIdWithFilters(
                any(UUID.class), any(MigrationErrorCategory.class), any(String.class), any(PageRequest.class)))
                .thenReturn(Page.empty());

            // When
            DataQualityReport report = service.generateReport(JOB_ID);

            // Then
            assertThat(report.getTopErrors()).isNotEmpty();
            assertThat(report.getTopErrors()).hasSize(2);
            assertThat(report.getTopErrors().get(0).getCount()).isEqualTo(10L);
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
            .status(JobStatus.COMPLETED)
            .sourceType(SourceType.FILE)
            .sourceConfig(sourceConfig)
            .dataType(DataType.HL7V2)
            .totalRecords(0L)
            .processedCount(0L)
            .successCount(0L)
            .failureCount(0L)
            .skippedCount(0L)
            .build();
    }

    private MigrationErrorEntity createErrorEntity(String message) {
        MigrationJobEntity job = createJobEntity();
        return MigrationErrorEntity.builder()
            .id(UUID.randomUUID())
            .job(job)
            .tenantId(TENANT_ID)
            .recordIdentifier("record-1")
            .sourceFile("test.hl7")
            .errorCategory(MigrationErrorCategory.PARSE_ERROR)
            .errorMessage(message)
            .createdAt(Instant.now())
            .build();
    }
}
