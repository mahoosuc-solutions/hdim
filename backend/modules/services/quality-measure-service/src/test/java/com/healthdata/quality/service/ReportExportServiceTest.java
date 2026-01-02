package com.healthdata.quality.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.persistence.SavedReportEntity;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD Tests for ReportExportService
 * Written BEFORE implementation - focusing on CSV and Excel export
 */
@DisplayName("ReportExportService Tests")
class ReportExportServiceTest {

    private ReportExportService exportService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        exportService = new ReportExportService(objectMapper);
    }

    // ===== CSV EXPORT TESTS =====

    @Test
    @DisplayName("Should export patient report to CSV format")
    void shouldExportPatientReportToCsv() throws Exception {
        // Arrange
        SavedReportEntity report = createPatientReport();

        // Act
        byte[] csvData = exportService.exportToCsv(report);

        // Assert
        assertThat(csvData).isNotNull();
        assertThat(csvData.length).isGreaterThan(0);

        String csvContent = new String(csvData);
        assertThat(csvContent).contains("Report Name");
        assertThat(csvContent).contains("Test Patient Report");
        assertThat(csvContent).contains("Patient ID");
        assertThat(csvContent).contains("PATIENT");
    }

    @Test
    @DisplayName("Should export population report to CSV format")
    void shouldExportPopulationReportToCsv() throws Exception {
        // Arrange
        SavedReportEntity report = createPopulationReport();

        // Act
        byte[] csvData = exportService.exportToCsv(report);

        // Assert
        assertThat(csvData).isNotNull();
        assertThat(csvData.length).isGreaterThan(0);

        String csvContent = new String(csvData);
        assertThat(csvContent).contains("Report Name");
        assertThat(csvContent).contains("2024 Population Report");
        assertThat(csvContent).contains("Year");
        assertThat(csvContent).contains("2024");
        assertThat(csvContent).contains("POPULATION");
    }

    @Test
    @DisplayName("Should include all metadata in CSV export")
    void shouldIncludeAllMetadataInCsvExport() throws Exception {
        // Arrange
        SavedReportEntity report = createPatientReport();

        // Act
        byte[] csvData = exportService.exportToCsv(report);
        String csvContent = new String(csvData);

        // Assert
        assertThat(csvContent).contains("Report Name");
        assertThat(csvContent).contains("Report Type");
        assertThat(csvContent).contains("Created By");
        assertThat(csvContent).contains("Created At");
        assertThat(csvContent).contains("Status");
        assertThat(csvContent).contains("test-user");
        assertThat(csvContent).contains("COMPLETED");
    }

    @Test
    @DisplayName("Should handle CSV export with special characters")
    void shouldHandleCsvExportWithSpecialCharacters() throws Exception {
        // Arrange
        SavedReportEntity report = createPatientReport();
        report.setReportName("Report with \"quotes\" and, commas");
        report.setDescription("Description with\nnewlines and special chars: ñ, é");

        // Act
        byte[] csvData = exportService.exportToCsv(report);
        String csvContent = new String(csvData);

        // Assert
        assertThat(csvData).isNotNull();
        assertThat(csvContent).contains("Report with");
        assertThat(csvContent).contains("Description with");
    }

    @Test
    @DisplayName("Should export array fields to CSV format")
    void shouldExportArrayFieldsToCsv() throws Exception {
        SavedReportEntity report = createPatientReport();
        report.setReportData("""
            {
              "metrics": ["A", "B", "C"],
              "counts": [1, 2]
            }
            """);

        byte[] csvData = exportService.exportToCsv(report);
        String csvContent = new String(csvData);

        assertThat(csvContent).contains("metrics[0]");
        assertThat(csvContent).contains("counts[1]");
    }

    @Test
    @DisplayName("Should write raw data when CSV JSON parsing fails")
    void shouldWriteRawDataWhenCsvParsingFails() throws Exception {
        SavedReportEntity report = createPatientReport();
        report.setReportData("not-json");

        byte[] csvData = exportService.exportToCsv(report);
        String csvContent = new String(csvData);

        assertThat(csvContent).contains("Raw Data");
        assertThat(csvContent).contains("not-json");
    }

    @Test
    @DisplayName("Should throw exception when report is null for CSV export")
    void shouldThrowExceptionWhenReportNullForCsv() {
        // Act & Assert
        assertThatThrownBy(() -> exportService.exportToCsv(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Report cannot be null");
    }

    // ===== EXCEL EXPORT TESTS =====

    @Test
    @DisplayName("Should export patient report to Excel format")
    void shouldExportPatientReportToExcel() throws Exception {
        // Arrange
        SavedReportEntity report = createPatientReport();

        // Act
        byte[] excelData = exportService.exportToExcel(report);

        // Assert
        assertThat(excelData).isNotNull();
        assertThat(excelData.length).isGreaterThan(0);

        // Excel files start with specific bytes (PK for zip format)
        assertThat(excelData[0]).isEqualTo((byte) 0x50); // 'P'
        assertThat(excelData[1]).isEqualTo((byte) 0x4B); // 'K'
    }

    @Test
    @DisplayName("Should export population report to Excel format")
    void shouldExportPopulationReportToExcel() throws Exception {
        // Arrange
        SavedReportEntity report = createPopulationReport();

        // Act
        byte[] excelData = exportService.exportToExcel(report);

        // Assert
        assertThat(excelData).isNotNull();
        assertThat(excelData.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should throw exception when report is null for Excel export")
    void shouldThrowExceptionWhenReportNullForExcel() {
        // Act & Assert
        assertThatThrownBy(() -> exportService.exportToExcel(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Report cannot be null");
    }

    @Test
    @DisplayName("Should create Excel with proper structure")
    void shouldCreateExcelWithProperStructure() throws Exception {
        // Arrange
        SavedReportEntity report = createPatientReport();

        // Act
        byte[] excelData = exportService.exportToExcel(report);

        // Assert - verify it's a valid Excel file format
        assertThat(excelData).isNotNull();
        assertThat(excelData.length).isGreaterThan(100); // Should have meaningful content
    }

    @Test
    @DisplayName("Should create Excel error row when report data is invalid")
    void shouldCreateExcelErrorRowWhenInvalidData() throws Exception {
        SavedReportEntity report = createPatientReport();
        report.setReportData("invalid-json");

        byte[] excelData = exportService.exportToExcel(report);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelData))) {
            Sheet sheet = workbook.getSheet("Report Data");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Error");
        }
    }

    @Test
    @DisplayName("Should export array fields to Excel format")
    void shouldExportArrayFieldsToExcel() throws Exception {
        SavedReportEntity report = createPatientReport();
        report.setReportData("""
            {
              "metrics": ["A", "B"],
              "counts": [1, 2]
            }
            """);

        byte[] excelData = exportService.exportToExcel(report);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelData))) {
            Sheet sheet = workbook.getSheet("Report Data");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Field");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).contains("metrics[0]");
        }
    }

    // ===== HELPER METHODS =====

    private SavedReportEntity createPatientReport() {
        String reportData = """
                {
                    "patientId": "550e8400-e29b-41d4-a716-446655440000",
                    "totalMeasures": 5,
                    "compliantMeasures": 4,
                    "qualityScore": 80.0,
                    "measuresByCategory": {
                        "HEDIS": 3,
                        "CMS": 2
                    },
                    "careGapSummary": "{\\"gaps\\": 1}"
                }
                """;

        return SavedReportEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("test-tenant")
                .reportType("PATIENT")
                .reportName("Test Patient Report")
                .description("A comprehensive patient quality report")
                .patientId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .reportData(reportData)
                .createdBy("test-user")
                .createdAt(LocalDateTime.now())
                .generatedAt(LocalDateTime.now())
                .status("COMPLETED")
                .build();
    }

    private SavedReportEntity createPopulationReport() {
        String reportData = """
                {
                    "year": 2024,
                    "uniquePatients": 1500,
                    "totalMeasures": 7500,
                    "compliantMeasures": 6000,
                    "overallScore": 80.0,
                    "measuresByCategory": {
                        "HEDIS": 4500,
                        "CMS": 3000
                    }
                }
                """;

        return SavedReportEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("test-tenant")
                .reportType("POPULATION")
                .reportName("2024 Population Report")
                .description("Annual population quality metrics")
                .year(2024)
                .reportData(reportData)
                .createdBy("test-user")
                .createdAt(LocalDateTime.now())
                .generatedAt(LocalDateTime.now())
                .status("COMPLETED")
                .build();
    }
}
