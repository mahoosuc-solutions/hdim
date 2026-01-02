package com.healthdata.migration.cli.output;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.springframework.stereotype.Component;

import com.healthdata.migration.dto.JobStatus;
import com.healthdata.migration.dto.MigrationJobResponse;

/**
 * ASCII table renderer for CLI output
 */
@Component
public class TableRenderer {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public void renderJobList(List<MigrationJobResponse> jobs) {
        if (jobs.isEmpty()) {
            System.out.println("No jobs found");
            return;
        }

        // Column widths
        int idWidth = 8;
        int nameWidth = 25;
        int statusWidth = 12;
        int progressWidth = 12;
        int recordsWidth = 15;
        int dateWidth = 16;

        // Header
        String headerFormat = "%-" + idWidth + "s | %-" + nameWidth + "s | %-" + statusWidth + "s | %-"
                + progressWidth + "s | %-" + recordsWidth + "s | %-" + dateWidth + "s%n";
        String rowFormat = headerFormat;

        System.out.println();
        System.out.printf(headerFormat, "ID", "Name", "Status", "Progress", "Records", "Created");
        System.out.println("-".repeat(idWidth + nameWidth + statusWidth + progressWidth + recordsWidth + dateWidth + 15));

        for (MigrationJobResponse job : jobs) {
            String shortId = job.getId().toString().substring(0, 8);
            String name = truncate(job.getJobName(), nameWidth);
            String status = colorizeStatus(job.getStatus());
            String progress = String.format("%.1f%%", job.getCompletionPercentage());
            String records = String.format("%d/%d", job.getProcessedCount(), job.getTotalRecords());
            String date = formatDate(job.getCreatedAt());

            System.out.printf(rowFormat, shortId, name, status, progress, records, date);
        }

        System.out.println();
        System.out.println("Total: " + jobs.size() + " jobs");
    }

    public void renderJobDetails(MigrationJobResponse job) {
        System.out.println();
        System.out.println("Job Details");
        System.out.println("===========");
        System.out.println();

        printRow("ID", job.getId().toString());
        printRow("Name", job.getJobName());
        printRow("Status", colorizeStatus(job.getStatus()));
        printRow("Source", job.getSourceType().toString());
        printRow("Data Type", job.getDataType().toString());

        System.out.println();
        System.out.println("Progress");
        System.out.println("--------");

        printRow("Total Records", String.valueOf(job.getTotalRecords()));
        printRow("Processed", String.valueOf(job.getProcessedCount()));
        printRow("Successful", colorize(String.valueOf(job.getSuccessCount()), Color.GREEN));
        printRow("Failed", job.getFailureCount() > 0
                ? colorize(String.valueOf(job.getFailureCount()), Color.RED)
                : "0");
        printRow("Skipped", String.valueOf(job.getSkippedCount()));
        printRow("Completion", String.format("%.1f%%", job.getCompletionPercentage()));
        printRow("Success Rate", String.format("%.1f%%", job.getSuccessRate()));

        System.out.println();
        System.out.println("Timestamps");
        System.out.println("----------");

        printRow("Created", formatDate(job.getCreatedAt()));
        if (job.getStartedAt() != null) {
            printRow("Started", formatDate(job.getStartedAt()));
        }
        if (job.getCompletedAt() != null) {
            printRow("Completed", formatDate(job.getCompletedAt()));
        }

        System.out.println();
    }

    private void printRow(String label, String value) {
        System.out.printf("  %-15s: %s%n", label, value);
    }

    private String colorizeStatus(JobStatus status) {
        return switch (status) {
            case COMPLETED -> colorize(status.name(), Color.GREEN);
            case RUNNING -> colorize(status.name(), Color.CYAN);
            case PAUSED -> colorize(status.name(), Color.YELLOW);
            case FAILED -> colorize(status.name(), Color.RED);
            case CANCELLED -> colorize(status.name(), Color.MAGENTA);
            case RETRYING -> colorize(status.name(), Color.YELLOW);
            default -> status.name();
        };
    }

    private String colorize(String text, Color color) {
        return Ansi.ansi().fg(color).a(text).reset().toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private String formatDate(Instant instant) {
        if (instant == null) return "-";
        return DATE_FORMATTER.format(instant);
    }
}
