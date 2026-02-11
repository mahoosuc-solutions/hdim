package com.healthdata.quality.integration;

import com.healthdata.quality.config.TestMessagingConfiguration;
import com.healthdata.quality.config.TestWebSocketConfiguration;
import com.healthdata.quality.persistence.SavedReportEntity;
import com.healthdata.quality.persistence.SavedReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD Integration Tests for Report Export API
 * Tests written BEFORE implementation
 */
@Tag("integration")
@SpringBootTest
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import({TestMessagingConfiguration.class, TestWebSocketConfiguration.class})
@DisplayName("Report Export API Integration Tests")
class ReportExportApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SavedReportRepository savedReportRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        savedReportRepository.deleteAll();
    }

    // ===== CSV EXPORT TESTS =====

    @Test
    @DisplayName("Should export report to CSV and return 200")
    void shouldExportReportToCsvAndReturn200() throws Exception {
        // Arrange
        SavedReportEntity report = createSavedReport("CSV Export Test");

        // Act & Assert
        mockMvc.perform(get("/quality-measure/reports/" + report.getId() + "/export/csv")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString(".csv")))
                .andExpect(content().string(containsString("Report Name")))
                .andExpect(content().string(containsString("CSV Export Test")));
    }

    @Test
    @DisplayName("Should enforce tenant isolation for CSV export")
    void shouldEnforceTenantIsolationForCsvExport() throws Exception {
        // Arrange
        SavedReportEntity otherTenantReport = createSavedReportForTenant("other-tenant", "Other Report");

        // Act & Assert - Should not find report from different tenant
        mockMvc.perform(get("/quality-measure/reports/" + otherTenantReport.getId() + "/export/csv")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when exporting non-existent report to CSV")
    void shouldReturn404WhenExportingNonExistentReportToCsv() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/quality-measure/reports/" + nonExistentId + "/export/csv")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 for invalid report ID format in CSV export")
    void shouldReturn400ForInvalidReportIdInCsvExport() throws Exception {
        mockMvc.perform(get("/quality-measure/reports/invalid-uuid/export/csv")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when tenant ID is missing for CSV export")
    void shouldReturn400WhenTenantIdMissingForCsvExport() throws Exception {
        SavedReportEntity report = createSavedReport("Test Report");

        mockMvc.perform(get("/quality-measure/reports/" + report.getId() + "/export/csv"))
                .andExpect(status().isBadRequest());
    }

    // ===== EXCEL EXPORT TESTS =====

    @Test
    @DisplayName("Should export report to Excel and return 200")
    void shouldExportReportToExcelAndReturn200() throws Exception {
        // Arrange
        SavedReportEntity report = createSavedReport("Excel Export Test");

        // Act & Assert
        mockMvc.perform(get("/quality-measure/reports/" + report.getId() + "/export/excel")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")));
    }

    @Test
    @DisplayName("Should enforce tenant isolation for Excel export")
    void shouldEnforceTenantIsolationForExcelExport() throws Exception {
        // Arrange
        SavedReportEntity otherTenantReport = createSavedReportForTenant("other-tenant", "Other Report");

        // Act & Assert
        mockMvc.perform(get("/quality-measure/reports/" + otherTenantReport.getId() + "/export/excel")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when exporting non-existent report to Excel")
    void shouldReturn404WhenExportingNonExistentReportToExcel() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/quality-measure/reports/" + nonExistentId + "/export/excel")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 for invalid report ID format in Excel export")
    void shouldReturn400ForInvalidReportIdInExcelExport() throws Exception {
        mockMvc.perform(get("/quality-measure/reports/invalid-uuid/export/excel")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when tenant ID is missing for Excel export")
    void shouldReturn400WhenTenantIdMissingForExcelExport() throws Exception {
        SavedReportEntity report = createSavedReport("Test Report");

        mockMvc.perform(get("/quality-measure/reports/" + report.getId() + "/export/excel"))
                .andExpect(status().isBadRequest());
    }

    // ===== HELPER METHODS =====

    private SavedReportEntity createSavedReport(String reportName) {
        return createSavedReportForTenant(TENANT_ID, reportName);
    }

    private SavedReportEntity createSavedReportForTenant(String tenantId, String reportName) {
        String reportData = """
                {
                    "patientId": "550e8400-e29b-41d4-a716-446655440000",
                    "totalMeasures": 5,
                    "compliantMeasures": 4,
                    "qualityScore": 80.0,
                    "measuresByCategory": {
                        "HEDIS": 3,
                        "CMS": 2
                    }
                }
                """;

        SavedReportEntity report = SavedReportEntity.builder()
                .tenantId(tenantId)
                .reportType("PATIENT")
                .reportName(reportName)
                .patientId(PATIENT_ID)
                .reportData(reportData)
                .createdBy("test-user")
                .status("COMPLETED")
                .build();

        return savedReportRepository.save(report);
    }
}
