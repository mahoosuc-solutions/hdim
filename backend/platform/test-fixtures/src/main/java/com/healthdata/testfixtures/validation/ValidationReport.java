package com.healthdata.testfixtures.validation;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.*;

/**
 * Structured report of entity-migration validation findings.
 *
 * Categorizes issues by severity (ERROR, WARNING, INFO) and provides
 * detailed information about schema mismatches between JPA entities
 * and actual database schema.
 *
 * @author HDIM Platform Team
 */
@Getter
@ToString
public class ValidationReport {

    private final Instant timestamp = Instant.now();
    private final List<ValidationIssue> errors = new ArrayList<>();
    private final List<ValidationIssue> warnings = new ArrayList<>();
    private final List<ValidationIssue> infos = new ArrayList<>();
    private int totalTablesChecked = 0;
    private int totalColumnsChecked = 0;
    private int totalIndexesChecked = 0;

    /**
     * Add an error-level issue.
     *
     * @param table the table name
     * @param column the column name (or null for table-level issues)
     * @param message the issue description
     */
    public void addError(String table, String column, String message) {
        errors.add(new ValidationIssue(table, column, message));
    }

    /**
     * Add an error-level issue (shorthand for table-level).
     *
     * @param table the table name
     * @param message the issue description
     */
    public void addError(String table, String message) {
        addError(table, null, message);
    }

    /**
     * Add a warning-level issue.
     *
     * @param table the table name
     * @param column the column name (or null for table-level issues)
     * @param message the issue description
     */
    public void addWarning(String table, String column, String message) {
        warnings.add(new ValidationIssue(table, column, message));
    }

    /**
     * Add a warning-level issue (shorthand for table-level).
     *
     * @param table the table name
     * @param message the issue description
     */
    public void addWarning(String table, String message) {
        addWarning(table, null, message);
    }

    /**
     * Add an info-level issue.
     *
     * @param table the table name
     * @param column the column name (or null for table-level issues)
     * @param message the issue description
     */
    public void addInfo(String table, String column, String message) {
        infos.add(new ValidationIssue(table, column, message));
    }

    /**
     * Add an info-level issue (shorthand for table-level).
     *
     * @param table the table name
     * @param message the issue description
     */
    public void addInfo(String table, String message) {
        addInfo(table, null, message);
    }

    /**
     * Check if validation passed (no errors).
     *
     * @return true if no errors, false otherwise
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Get total number of issues.
     *
     * @return sum of errors, warnings, and infos
     */
    public int getTotalIssues() {
        return errors.size() + warnings.size() + infos.size();
    }

    /**
     * Increment table check counter.
     */
    public void incrementTablesChecked() {
        totalTablesChecked++;
    }

    /**
     * Increment table check counter by amount.
     *
     * @param count number to increment by
     */
    public void incrementTablesChecked(int count) {
        totalTablesChecked += count;
    }

    /**
     * Increment column check counter.
     */
    public void incrementColumnsChecked() {
        totalColumnsChecked++;
    }

    /**
     * Increment column check counter by amount.
     *
     * @param count number to increment by
     */
    public void incrementColumnsChecked(int count) {
        totalColumnsChecked += count;
    }

    /**
     * Increment index check counter.
     */
    public void incrementIndexesChecked() {
        totalIndexesChecked++;
    }

    /**
     * Increment index check counter by amount.
     *
     * @param count number to increment by
     */
    public void incrementIndexesChecked(int count) {
        totalIndexesChecked += count;
    }

    /**
     * Get detailed message for test assertions.
     *
     * @return formatted error message with all issues
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Entity-Migration Validation Report ===\n");
        sb.append(String.format("Timestamp: %s\n", timestamp));
        sb.append(String.format("Tables Checked: %d | Columns Checked: %d | Indexes Checked: %d\n",
                totalTablesChecked, totalColumnsChecked, totalIndexesChecked));
        sb.append(String.format("Issues: %d errors, %d warnings, %d infos\n",
                errors.size(), warnings.size(), infos.size()));
        sb.append("\n");

        if (!errors.isEmpty()) {
            sb.append("ERRORS:\n");
            for (ValidationIssue issue : errors) {
                sb.append(String.format("  - [%s] %s: %s\n", issue.getTable(), issue.getColumn(), issue.getMessage()));
            }
            sb.append("\n");
        }

        if (!warnings.isEmpty()) {
            sb.append("WARNINGS:\n");
            for (ValidationIssue issue : warnings) {
                sb.append(String.format("  - [%s] %s: %s\n", issue.getTable(), issue.getColumn(), issue.getMessage()));
            }
            sb.append("\n");
        }

        if (!infos.isEmpty()) {
            sb.append("INFOS:\n");
            for (ValidationIssue issue : infos) {
                sb.append(String.format("  - [%s] %s: %s\n", issue.getTable(), issue.getColumn(), issue.getMessage()));
            }
            sb.append("\n");
        }

        sb.append("===========================================\n");
        return sb.toString();
    }

    /**
     * Represents a single validation issue.
     */
    @Getter
    @ToString
    public static class ValidationIssue {
        private final String table;
        private final String column;
        private final String message;

        public ValidationIssue(String table, String column, String message) {
            this.table = table;
            this.column = column != null ? column : "(table-level)";
            this.message = message;
        }
    }
}
