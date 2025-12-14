package com.healthdata.migration.cli.command;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthdata.migration.cli.client.MigrationApiClient;
import com.healthdata.migration.dto.DataQualityReport;
import com.healthdata.migration.dto.MigrationErrorCategory;
import com.healthdata.migration.dto.MigrationSummary;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Generate reports for completed migrations
 */
@Component
@Command(name = "report", description = "Generate migration reports")
public class ReportCommand implements Callable<Integer> {

    @ParentCommand
    private com.healthdata.migration.cli.MigrationCommand parent;

    private final MigrationApiClient apiClient;
    private final ObjectMapper objectMapper;

    @Parameters(index = "0", description = "Job ID")
    UUID jobId;

    @Option(names = {"--format", "-f"}, defaultValue = "text", description = "Output format: text, json, csv")
    String format;

    @Option(names = {"--output", "-o"}, description = "Output file (prints to stdout if not specified)")
    String outputFile;

    @Option(names = {"--type"}, defaultValue = "summary", description = "Report type: summary, quality, errors")
    String reportType;

    public ReportCommand(MigrationApiClient apiClient) {
        this.apiClient = apiClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public Integer call() {
        try {
            String report = switch (reportType.toLowerCase()) {
                case "summary" -> generateSummaryReport();
                case "quality" -> generateQualityReport();
                case "errors" -> generateErrorsReport();
                default -> throw new IllegalArgumentException("Unknown report type: " + reportType);
            };

            if (outputFile != null) {
                try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                    writer.print(report);
                }
                System.out.println("Report written to: " + outputFile);
            } else {
                System.out.println(report);
            }

            return 0;
        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
            if (parent.verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    private String generateSummaryReport() throws Exception {
        MigrationSummary summary = apiClient.getSummary(parent.apiUrl, parent.tenantId, jobId);

        return switch (format.toLowerCase()) {
            case "json" -> objectMapper.writeValueAsString(summary);
            case "csv" -> formatSummaryAsCsv(summary);
            default -> formatSummaryAsText(summary);
        };
    }

    private String generateQualityReport() throws Exception {
        DataQualityReport report = apiClient.getQualityReport(parent.apiUrl, parent.tenantId, jobId);

        return switch (format.toLowerCase()) {
            case "json" -> objectMapper.writeValueAsString(report);
            default -> formatQualityAsText(report);
        };
    }

    private String generateErrorsReport() throws Exception {
        // Get errors from API
        var errors = apiClient.getErrors(parent.apiUrl, parent.tenantId, jobId, 100);

        StringBuilder sb = new StringBuilder();
        sb.append("Migration Errors Report\n");
        sb.append("=======================\n\n");
        sb.append("Job ID: ").append(jobId).append("\n");
        sb.append("Total Errors: ").append(errors.size()).append("\n\n");

        for (var error : errors) {
            sb.append("---\n");
            sb.append("Category: ").append(error.getErrorCategory()).append("\n");
            sb.append("Record: ").append(error.getRecordIdentifier()).append("\n");
            sb.append("Message: ").append(error.getErrorMessage()).append("\n");
            if (error.getSourceFile() != null) {
                sb.append("File: ").append(error.getSourceFile()).append("\n");
            }
        }

        return sb.toString();
    }

    private String formatSummaryAsText(MigrationSummary summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("Migration Summary Report\n");
        sb.append("========================\n\n");
        sb.append("Job: ").append(summary.getJobName()).append("\n");
        sb.append("ID: ").append(summary.getJobId()).append("\n");
        sb.append("Status: ").append(summary.getFinalStatus()).append("\n\n");

        sb.append("Record Statistics:\n");
        sb.append("  Total Records:  ").append(summary.getTotalRecords()).append("\n");
        sb.append("  Successful:     ").append(summary.getSuccessCount()).append("\n");
        sb.append("  Failed:         ").append(summary.getFailureCount()).append("\n");
        sb.append("  Skipped:        ").append(summary.getSkippedCount()).append("\n\n");

        sb.append("Performance:\n");
        sb.append("  Duration:       ").append(summary.getFormattedDuration()).append("\n");
        sb.append(String.format("  Records/sec:    %.1f%n", summary.getRecordsPerSecond()));
        sb.append(String.format("  Avg time/record: %d ms%n", summary.getAvgProcessingTimeMs())).append("\n");

        sb.append("Success Rate: ").append(String.format("%.1f%%", summary.getSuccessRate())).append("\n");

        if (summary.getFhirResourcesCreated() != null && !summary.getFhirResourcesCreated().isEmpty()) {
            sb.append("\nFHIR Resources Created:\n");
            for (var entry : summary.getFhirResourcesCreated().entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        if (summary.getErrorsByCategory() != null && !summary.getErrorsByCategory().isEmpty()) {
            sb.append("\nErrors by Category:\n");
            for (var entry : summary.getErrorsByCategory().entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        return sb.toString();
    }

    private String formatSummaryAsCsv(MigrationSummary summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("job_id,job_name,status,total,success,failed,skipped,success_rate,duration_ms\n");
        sb.append(summary.getJobId()).append(",");
        sb.append(summary.getJobName()).append(",");
        sb.append(summary.getFinalStatus()).append(",");
        sb.append(summary.getTotalRecords()).append(",");
        sb.append(summary.getSuccessCount()).append(",");
        sb.append(summary.getFailureCount()).append(",");
        sb.append(summary.getSkippedCount()).append(",");
        sb.append(String.format("%.2f", summary.getSuccessRate())).append(",");
        sb.append(summary.getTotalDurationMs()).append("\n");
        return sb.toString();
    }

    private String formatQualityAsText(DataQualityReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Data Quality Report\n");
        sb.append("===================\n\n");
        sb.append("Job: ").append(report.getJobName()).append("\n");
        sb.append("Generated: ").append(report.getGeneratedAt()).append("\n\n");

        sb.append("Quality Score: ").append(String.format("%.1f", report.getQualityScore()));
        sb.append(" (Grade: ").append(report.getQualityGrade()).append(")\n\n");

        if (report.getSummary() != null) {
            sb.append("Summary:\n");
            sb.append(String.format("  Success Rate: %.1f%%%n", report.getSummary().getSuccessRate()));
            sb.append(String.format("  Failure Rate: %.1f%%%n", report.getSummary().getFailureRate()));
        }

        if (report.getErrorsByCategory() != null && !report.getErrorsByCategory().isEmpty()) {
            sb.append("\nError Breakdown:\n");
            for (var entry : report.getErrorsByCategory().entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        return sb.toString();
    }
}
