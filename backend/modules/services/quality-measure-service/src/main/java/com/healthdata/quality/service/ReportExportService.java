package com.healthdata.quality.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.persistence.SavedReportEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Report Export Service
 * Handles exporting saved reports to various formats (CSV, Excel, PDF)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExportService {

    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Export report to CSV format
     */
    public byte[] exportToCsv(SavedReportEntity report) throws IOException {
        if (report == null) {
            throw new IllegalArgumentException("Report cannot be null");
        }

        log.info("Exporting report {} to CSV format", report.getId());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CSVPrinter csvPrinter = new CSVPrinter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder()
                        .setHeader("Field", "Value")
                        .build()
        )) {
            // Report metadata
            csvPrinter.printRecord("Report ID", report.getId());
            csvPrinter.printRecord("Report Name", report.getReportName());
            csvPrinter.printRecord("Report Type", report.getReportType());
            csvPrinter.printRecord("Description", report.getDescription() != null ? report.getDescription() : "");
            csvPrinter.printRecord("Tenant ID", report.getTenantId());

            // Type-specific fields
            if (report.getPatientId() != null) {
                csvPrinter.printRecord("Patient ID", report.getPatientId());
            }
            if (report.getYear() != null) {
                csvPrinter.printRecord("Year", report.getYear());
            }

            // Audit fields
            csvPrinter.printRecord("Created By", report.getCreatedBy());
            csvPrinter.printRecord("Created At", formatDateTime(report.getCreatedAt()));
            csvPrinter.printRecord("Generated At", formatDateTime(report.getGeneratedAt()));
            csvPrinter.printRecord("Status", report.getStatus());

            // Empty row
            csvPrinter.println();

            // Report data
            csvPrinter.printRecord("Report Data", "");
            if (report.getReportData() != null) {
                try {
                    JsonNode reportDataNode = objectMapper.readTree(report.getReportData());
                    exportJsonToCSV(csvPrinter, reportDataNode, "");
                } catch (Exception e) {
                    log.warn("Error parsing report data for CSV export: {}", e.getMessage());
                    csvPrinter.printRecord("Raw Data", report.getReportData());
                }
            }

            csvPrinter.flush();
        }

        return outputStream.toByteArray();
    }

    /**
     * Export report to Excel format
     */
    public byte[] exportToExcel(SavedReportEntity report) throws IOException {
        if (report == null) {
            throw new IllegalArgumentException("Report cannot be null");
        }

        log.info("Exporting report {} to Excel format", report.getId());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Create metadata sheet
            Sheet metadataSheet = workbook.createSheet("Report Metadata");
            createMetadataSheet(workbook, metadataSheet, report);

            // Create data sheet
            Sheet dataSheet = workbook.createSheet("Report Data");
            createDataSheet(workbook, dataSheet, report);

            // Auto-size columns
            for (int i = 0; i < 2; i++) {
                metadataSheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    // Helper methods

    private void createMetadataSheet(Workbook workbook, Sheet sheet, SavedReportEntity report) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        int rowNum = 0;

        // Header
        Row headerRow = sheet.createRow(rowNum++);
        createCell(headerRow, 0, "Field", headerStyle);
        createCell(headerRow, 1, "Value", headerStyle);

        // Data rows
        createMetadataRow(sheet, rowNum++, "Report ID", report.getId().toString());
        createMetadataRow(sheet, rowNum++, "Report Name", report.getReportName());
        createMetadataRow(sheet, rowNum++, "Report Type", report.getReportType());
        createMetadataRow(sheet, rowNum++, "Description", report.getDescription() != null ? report.getDescription() : "");
        createMetadataRow(sheet, rowNum++, "Tenant ID", report.getTenantId());

        if (report.getPatientId() != null) {
            createMetadataRow(sheet, rowNum++, "Patient ID", report.getPatientId().toString());
        }
        if (report.getYear() != null) {
            createMetadataRow(sheet, rowNum++, "Year", report.getYear().toString());
        }

        createMetadataRow(sheet, rowNum++, "Created By", report.getCreatedBy());
        createMetadataRow(sheet, rowNum++, "Created At", formatDateTime(report.getCreatedAt()));
        createMetadataRow(sheet, rowNum++, "Generated At", formatDateTime(report.getGeneratedAt()));
        createMetadataRow(sheet, rowNum++, "Status", report.getStatus());
    }

    private void createDataSheet(Workbook workbook, Sheet sheet, SavedReportEntity report) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        int rowNum = 0;

        // Parse and display report data
        if (report.getReportData() != null) {
            try {
                JsonNode reportDataNode = objectMapper.readTree(report.getReportData());

                // Create header
                Row headerRow = sheet.createRow(rowNum++);
                createCell(headerRow, 0, "Field", headerStyle);
                createCell(headerRow, 1, "Value", headerStyle);

                // Add data
                rowNum = exportJsonToExcel(sheet, reportDataNode, "", rowNum);

            } catch (Exception e) {
                log.warn("Error parsing report data for Excel export: {}", e.getMessage());
                Row errorRow = sheet.createRow(rowNum);
                createCell(errorRow, 0, "Error", headerStyle);
                createCell(errorRow, 1, "Unable to parse report data", null);
            }
        }

        // Auto-size columns
        for (int i = 0; i < 2; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createMetadataRow(Sheet sheet, int rowNum, String field, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(field);
        row.createCell(1).setCellValue(value != null ? value : "");
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void exportJsonToCSV(CSVPrinter csvPrinter, JsonNode node, String prefix) throws IOException {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                try {
                    if (entry.getValue().isValueNode()) {
                        csvPrinter.printRecord(key, entry.getValue().asText());
                    } else {
                        exportJsonToCSV(csvPrinter, entry.getValue(), key);
                    }
                } catch (IOException e) {
                    log.warn("Error exporting field {} to CSV: {}", key, e.getMessage());
                }
            });
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String key = prefix + "[" + i + "]";
                JsonNode arrayElement = node.get(i);
                if (arrayElement.isValueNode()) {
                    csvPrinter.printRecord(key, arrayElement.asText());
                } else {
                    exportJsonToCSV(csvPrinter, arrayElement, key);
                }
            }
        } else {
            csvPrinter.printRecord(prefix, node.asText());
        }
    }

    private int exportJsonToExcel(Sheet sheet, JsonNode node, String prefix, int rowNum) {
        if (node.isObject()) {
            for (var entry : node.properties()) {
                String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                if (entry.getValue().isValueNode()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(key);
                    row.createCell(1).setCellValue(entry.getValue().asText());
                } else {
                    rowNum = exportJsonToExcel(sheet, entry.getValue(), key, rowNum);
                }
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String key = prefix + "[" + i + "]";
                JsonNode arrayElement = node.get(i);
                if (arrayElement.isValueNode()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(key);
                    row.createCell(1).setCellValue(arrayElement.asText());
                } else {
                    rowNum = exportJsonToExcel(sheet, arrayElement, key, rowNum);
                }
            }
        } else {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(prefix);
            row.createCell(1).setCellValue(node.asText());
        }
        return rowNum;
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : "";
    }
}
