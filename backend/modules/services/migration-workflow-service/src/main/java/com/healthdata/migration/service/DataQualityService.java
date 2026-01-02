package com.healthdata.migration.service;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthdata.migration.dto.DataQualityReport;
import com.healthdata.migration.dto.DataQualityReport.DataQualityIssue;
import com.healthdata.migration.dto.DataQualityReport.DataQualityIssue.Severity;
import com.healthdata.migration.dto.DataQualityReport.ErrorSample;
import com.healthdata.migration.dto.DataQualityReport.QualitySummary;
import com.healthdata.migration.dto.MigrationErrorCategory;
import com.healthdata.migration.persistence.MigrationErrorEntity;
import com.healthdata.migration.persistence.MigrationJobEntity;
import com.healthdata.migration.repository.MigrationErrorRepository;
import com.healthdata.migration.repository.MigrationJobRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for data quality analysis and reporting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataQualityService {

    private final MigrationJobRepository jobRepository;
    private final MigrationErrorRepository errorRepository;

    /**
     * Generate a full data quality report for a migration job
     */
    @Transactional(readOnly = true)
    public DataQualityReport generateReport(UUID jobId) {
        MigrationJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        return generateReport(job);
    }

    /**
     * Generate a full data quality report for a migration job
     */
    @Transactional(readOnly = true)
    public DataQualityReport generateReport(MigrationJobEntity job) {
        log.info("Generating data quality report for job: {}", job.getId());

        // Build summary
        QualitySummary summary = buildSummary(job);

        // Get error breakdown by category
        Map<MigrationErrorCategory, Long> errorsByCategory = getErrorsByCategory(job.getId());

        // Analyze for data quality issues
        List<DataQualityIssue> dataQualityIssues = analyzeDataQualityIssues(job, errorsByCategory);

        // Get top errors
        List<ErrorSample> topErrors = getTopErrors(job.getId(), 10);

        return DataQualityReport.builder()
                .jobId(job.getId())
                .jobName(job.getJobName())
                .generatedAt(Instant.now())
                .summary(summary)
                .errorsByCategory(errorsByCategory)
                .fhirResourcesCreated(job.getFhirResourcesCreated())
                .dataQualityIssues(dataQualityIssues)
                .topErrors(topErrors)
                .build();
    }

    /**
     * Get error breakdown by category for a job
     */
    @Transactional(readOnly = true)
    public Map<MigrationErrorCategory, Long> getErrorsByCategory(UUID jobId) {
        List<Object[]> results = errorRepository.countByJobIdGroupByCategory(jobId);

        Map<MigrationErrorCategory, Long> errorsByCategory = new EnumMap<>(MigrationErrorCategory.class);

        // Initialize all categories with 0
        for (MigrationErrorCategory category : MigrationErrorCategory.values()) {
            errorsByCategory.put(category, 0L);
        }

        // Populate actual counts
        for (Object[] row : results) {
            MigrationErrorCategory category = (MigrationErrorCategory) row[0];
            Long count = (Long) row[1];
            errorsByCategory.put(category, count);
        }

        return errorsByCategory;
    }

    /**
     * Get paginated errors for a job
     */
    @Transactional(readOnly = true)
    public Page<MigrationErrorEntity> getErrors(UUID jobId, MigrationErrorCategory category,
                                                 String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return errorRepository.findByJobIdWithFilters(jobId, category, search, pageable);
    }

    /**
     * Get recent errors for live display
     */
    @Transactional(readOnly = true)
    public List<MigrationErrorEntity> getRecentErrors(UUID jobId, int limit) {
        return errorRepository.findRecentByJobId(jobId, PageRequest.of(0, limit));
    }

    /**
     * Export errors to CSV format
     */
    @Transactional(readOnly = true)
    public String exportErrorsToCsv(UUID jobId) throws IOException {
        StringWriter writer = new StringWriter();
        writeErrorsCsv(jobId, writer);
        return writer.toString();
    }

    /**
     * Write errors to CSV
     */
    @Transactional(readOnly = true)
    public void writeErrorsCsv(UUID jobId, Writer writer) throws IOException {
        // Header
        writer.write("Record ID,Source File,Category,Message,Patient ID,Message Type,Created At\n");

        // Process in batches
        int page = 0;
        int size = 1000;
        Page<MigrationErrorEntity> errors;

        do {
            errors = errorRepository.findByJobId(jobId, PageRequest.of(page, size));

            for (MigrationErrorEntity error : errors) {
                writer.write(String.format("%s,%s,%s,\"%s\",%s,%s,%s\n",
                        escapeCsv(error.getRecordIdentifier()),
                        escapeCsv(error.getSourceFile()),
                        error.getErrorCategory(),
                        escapeCsv(error.getErrorMessage()),
                        escapeCsv(error.getPatientId()),
                        escapeCsv(error.getMessageType()),
                        error.getCreatedAt()
                ));
            }

            page++;
        } while (errors.hasNext());
    }

    /**
     * Export quality report to CSV format
     */
    @Transactional(readOnly = true)
    public String exportReportToCsv(UUID jobId) {
        DataQualityReport report = generateReport(jobId);
        StringBuilder csv = new StringBuilder();

        // Summary section
        csv.append("Data Quality Report\n");
        csv.append("Job ID,").append(report.getJobId()).append("\n");
        csv.append("Job Name,").append(report.getJobName()).append("\n");
        csv.append("Generated,").append(report.getGeneratedAt()).append("\n");
        csv.append("Quality Score,").append(String.format("%.1f", report.getQualityScore())).append("\n");
        csv.append("Quality Grade,").append(report.getQualityGrade()).append("\n\n");

        // Statistics
        csv.append("Statistics\n");
        if (report.getSummary() != null) {
            csv.append("Total Records,").append(report.getSummary().getTotalRecords()).append("\n");
            csv.append("Successful,").append(report.getSummary().getSuccessCount()).append("\n");
            csv.append("Failed,").append(report.getSummary().getFailureCount()).append("\n");
            csv.append("Skipped,").append(report.getSummary().getSkippedCount()).append("\n");
            csv.append("Success Rate,").append(String.format("%.2f%%", report.getSummary().getSuccessRate())).append("\n");
            csv.append("Processing Time (ms),").append(report.getSummary().getProcessingTimeMs()).append("\n\n");
        }

        // Errors by category
        csv.append("Errors by Category\n");
        csv.append("Category,Count\n");
        if (report.getErrorsByCategory() != null) {
            for (Map.Entry<MigrationErrorCategory, Long> entry : report.getErrorsByCategory().entrySet()) {
                if (entry.getValue() > 0) {
                    csv.append(entry.getKey()).append(",").append(entry.getValue()).append("\n");
                }
            }
        }
        csv.append("\n");

        // FHIR resources created
        csv.append("FHIR Resources Created\n");
        csv.append("Resource Type,Count\n");
        if (report.getFhirResourcesCreated() != null) {
            for (Map.Entry<String, Long> entry : report.getFhirResourcesCreated().entrySet()) {
                csv.append(entry.getKey()).append(",").append(entry.getValue()).append("\n");
            }
        }
        csv.append("\n");

        // Data quality issues
        csv.append("Data Quality Issues\n");
        csv.append("Field,Issue,Count,Severity,Recommendation\n");
        if (report.getDataQualityIssues() != null) {
            for (DataQualityIssue issue : report.getDataQualityIssues()) {
                csv.append(escapeCsv(issue.getField())).append(",");
                csv.append(escapeCsv(issue.getIssue())).append(",");
                csv.append(issue.getCount()).append(",");
                csv.append(issue.getSeverity()).append(",");
                csv.append(escapeCsv(issue.getRecommendation())).append("\n");
            }
        }
        csv.append("\n");

        // Top errors
        csv.append("Top Errors\n");
        csv.append("Category,Message,Count\n");
        if (report.getTopErrors() != null) {
            for (ErrorSample error : report.getTopErrors()) {
                csv.append(error.getCategory()).append(",");
                csv.append(escapeCsv(error.getErrorMessage())).append(",");
                csv.append(error.getCount()).append("\n");
            }
        }

        return csv.toString();
    }

    private QualitySummary buildSummary(MigrationJobEntity job) {
        long duration = 0;
        if (job.getStartedAt() != null) {
            Instant endTime = job.getCompletedAt() != null ? job.getCompletedAt() : Instant.now();
            duration = endTime.toEpochMilli() - job.getStartedAt().toEpochMilli();
        }

        double successRate = job.getTotalRecords() > 0
                ? (double) job.getSuccessCount() / job.getTotalRecords() * 100
                : 0;

        double failureRate = job.getTotalRecords() > 0
                ? (double) job.getFailureCount() / job.getTotalRecords() * 100
                : 0;

        double avgTime = job.getProcessedCount() > 0 && duration > 0
                ? (double) duration / job.getProcessedCount()
                : 0;

        return QualitySummary.builder()
                .totalRecords(job.getTotalRecords())
                .successCount(job.getSuccessCount())
                .failureCount(job.getFailureCount())
                .skippedCount(job.getSkippedCount())
                .successRate(successRate)
                .failureRate(failureRate)
                .processingTimeMs(duration)
                .avgProcessingTimeMs(avgTime)
                .build();
    }

    private List<DataQualityIssue> analyzeDataQualityIssues(MigrationJobEntity job,
                                                            Map<MigrationErrorCategory, Long> errorsByCategory) {
        List<DataQualityIssue> issues = new ArrayList<>();

        // Check for high parse error rate
        Long parseErrors = errorsByCategory.getOrDefault(MigrationErrorCategory.PARSE_ERROR, 0L);
        if (parseErrors > 0 && job.getTotalRecords() > 0) {
            double parseErrorRate = (double) parseErrors / job.getTotalRecords() * 100;
            if (parseErrorRate > 5) {
                issues.add(DataQualityIssue.builder()
                        .field("Source Data")
                        .issue("High parse error rate: " + String.format("%.1f%%", parseErrorRate))
                        .count(parseErrors)
                        .severity(parseErrorRate > 20 ? Severity.CRITICAL : Severity.ERROR)
                        .recommendation("Review source data format and ensure consistent encoding")
                        .build());
            }
        }

        // Check for validation errors
        Long validationErrors = errorsByCategory.getOrDefault(MigrationErrorCategory.VALIDATION_ERROR, 0L);
        if (validationErrors > 0) {
            issues.add(DataQualityIssue.builder()
                    .field("FHIR Validation")
                    .issue("Failed FHIR resource validation")
                    .count(validationErrors)
                    .severity(Severity.WARNING)
                    .recommendation("Review field mappings and data types")
                    .build());
        }

        // Check for mapping errors
        Long mappingErrors = errorsByCategory.getOrDefault(MigrationErrorCategory.MAPPING_ERROR, 0L);
        if (mappingErrors > 0) {
            issues.add(DataQualityIssue.builder()
                    .field("Field Mapping")
                    .issue("Unable to map source fields to FHIR")
                    .count(mappingErrors)
                    .severity(Severity.WARNING)
                    .recommendation("Review mapping configuration and source field values")
                    .build());
        }

        // Check for invalid codes
        Long codeErrors = errorsByCategory.getOrDefault(MigrationErrorCategory.INVALID_CODE, 0L);
        if (codeErrors > 0) {
            issues.add(DataQualityIssue.builder()
                    .field("Terminology")
                    .issue("Invalid terminology codes (ICD-10, SNOMED, LOINC)")
                    .count(codeErrors)
                    .severity(Severity.WARNING)
                    .recommendation("Verify code systems and values in source data")
                    .build());
        }

        // Check for missing required fields
        Long missingRequired = errorsByCategory.getOrDefault(MigrationErrorCategory.MISSING_REQUIRED, 0L);
        if (missingRequired > 0) {
            issues.add(DataQualityIssue.builder()
                    .field("Required Fields")
                    .issue("Missing required data elements")
                    .count(missingRequired)
                    .severity(Severity.ERROR)
                    .recommendation("Ensure source data includes all required fields")
                    .build());
        }

        // Check for duplicates
        Long duplicates = errorsByCategory.getOrDefault(MigrationErrorCategory.DUPLICATE_RECORD, 0L);
        if (duplicates > 0) {
            issues.add(DataQualityIssue.builder()
                    .field("Identifiers")
                    .issue("Duplicate records detected")
                    .count(duplicates)
                    .severity(Severity.INFO)
                    .recommendation("Review source data for duplicate entries")
                    .build());
        }

        // Check overall failure rate
        if (job.getTotalRecords() > 0) {
            double failureRate = (double) job.getFailureCount() / job.getTotalRecords() * 100;
            if (failureRate > 10) {
                issues.add(DataQualityIssue.builder()
                        .field("Overall")
                        .issue("High overall failure rate: " + String.format("%.1f%%", failureRate))
                        .count(job.getFailureCount())
                        .severity(failureRate > 30 ? Severity.CRITICAL : Severity.ERROR)
                        .recommendation("Review error patterns and source data quality")
                        .build());
            }
        }

        // Sort by severity (critical first)
        issues.sort((a, b) -> {
            int severityOrder = getSeverityOrder(b.getSeverity()) - getSeverityOrder(a.getSeverity());
            return severityOrder != 0 ? severityOrder : Long.compare(b.getCount(), a.getCount());
        });

        return issues;
    }

    private int getSeverityOrder(Severity severity) {
        return switch (severity) {
            case CRITICAL -> 4;
            case ERROR -> 3;
            case WARNING -> 2;
            case INFO -> 1;
        };
    }

    private List<ErrorSample> getTopErrors(UUID jobId, int limit) {
        List<Object[]> results = errorRepository.findTopErrorsByJobId(
                jobId, PageRequest.of(0, limit));

        List<ErrorSample> topErrors = new ArrayList<>();

        for (Object[] row : results) {
            MigrationErrorCategory category = (MigrationErrorCategory) row[0];
            String message = (String) row[1];
            Long count = (Long) row[2];

            // Get sample record for this error type
            List<MigrationErrorEntity> samples = errorRepository.findByJobIdWithFilters(
                    jobId, category, message, PageRequest.of(0, 1)).getContent();

            ErrorSample sample = ErrorSample.builder()
                    .category(category)
                    .errorMessage(truncateMessage(message, 200))
                    .count(count)
                    .build();

            if (!samples.isEmpty()) {
                MigrationErrorEntity sampleError = samples.get(0);
                sample.setSampleRecordId(sampleError.getRecordIdentifier());
                sample.setSampleSourceFile(sampleError.getSourceFile());
            }

            topErrors.add(sample);
        }

        return topErrors;
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains comma or quote
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String truncateMessage(String message, int maxLength) {
        if (message == null) {
            return null;
        }
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength - 3) + "...";
    }
}
