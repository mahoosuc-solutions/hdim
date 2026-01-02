package com.healthdata.quality.integration;

import com.healthdata.quality.config.TestMessagingConfiguration;
import com.healthdata.quality.config.TestWebSocketConfiguration;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import com.healthdata.quality.persistence.SavedReportEntity;
import com.healthdata.quality.persistence.SavedReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD Integration Tests for Saved Reports API
 * Tests written BEFORE implementation
 */
@SpringBootTest
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import({TestMessagingConfiguration.class, TestWebSocketConfiguration.class})
@DisplayName("Saved Reports API Integration Tests")
class SavedReportsApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SavedReportRepository savedReportRepository;

    @Autowired
    private QualityMeasureResultRepository resultRepository;

    @Autowired
    private CacheManager cacheManager;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID PATIENT_ID_2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @BeforeEach
    void setUp() {
        savedReportRepository.deleteAll();
        resultRepository.deleteAll();

        // Clear all caches
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    // ===== SAVE PATIENT REPORT TESTS =====

    @Test
    @DisplayName("Should save patient report and return 201 Created")
    void shouldSavePatientReportAndReturn201() throws Exception {
        // Setup test data
        createMeasureResults(TENANT_ID, PATIENT_ID);

        mockMvc.perform(post("/quality-measure/report/patient/save")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("name", "Patient Quality Report - John Doe")
                        .param("createdBy", "test-user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID))
                .andExpect(jsonPath("$.reportType").value("PATIENT"))
                .andExpect(jsonPath("$.reportName").value("Patient Quality Report - John Doe"))
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.createdBy").value("test-user"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.reportData").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("Should return 400 when tenant ID is missing for patient report")
    void shouldReturn400WhenTenantIdMissingForPatientReport() throws Exception {
        mockMvc.perform(post("/quality-measure/report/patient/save")
                        .param("patient", PATIENT_ID.toString())
                        .param("name", "Test Report")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when patient ID is missing")
    void shouldReturn400WhenPatientIdMissing() throws Exception {
        mockMvc.perform(post("/quality-measure/report/patient/save")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("name", "Test Report")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when report name is missing")
    void shouldReturn400WhenReportNameMissing() throws Exception {
        mockMvc.perform(post("/quality-measure/report/patient/save")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should use default createdBy when not provided")
    void shouldUseDefaultCreatedByWhenNotProvided() throws Exception {
        createMeasureResults(TENANT_ID, PATIENT_ID);

        mockMvc.perform(post("/quality-measure/report/patient/save")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("name", "Test Report")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value("system"));
    }

    // ===== SAVE POPULATION REPORT TESTS =====

    @Test
    @DisplayName("Should save population report and return 201 Created")
    void shouldSavePopulationReportAndReturn201() throws Exception {
        // Setup test data for 2024
        createMeasureResults(TENANT_ID, PATIENT_ID);

        mockMvc.perform(post("/quality-measure/report/population/save")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("year", "2024")
                        .param("name", "2024 Population Quality Report")
                        .param("createdBy", "test-user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID))
                .andExpect(jsonPath("$.reportType").value("POPULATION"))
                .andExpect(jsonPath("$.reportName").value("2024 Population Quality Report"))
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.createdBy").value("test-user"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Should return 400 when year is missing for population report")
    void shouldReturn400WhenYearMissing() throws Exception {
        mockMvc.perform(post("/quality-measure/report/population/save")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("name", "Test Report")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ===== GET ALL REPORTS TESTS =====

    @Test
    @DisplayName("Should retrieve all saved reports for tenant")
    void shouldRetrieveAllSavedReportsForTenant() throws Exception {
        // Create test reports
        createSavedReport(TENANT_ID, "Report 1", "PATIENT", PATIENT_ID, null);
        createSavedReport(TENANT_ID, "Report 2", "PATIENT", PATIENT_ID_2, null);
        createSavedReport(TENANT_ID, "Report 3", "POPULATION", null, 2024);

        mockMvc.perform(get("/quality-measure/reports")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].tenantId", everyItem(is(TENANT_ID))));
    }

    @Test
    @DisplayName("Should return empty array when no saved reports exist")
    void shouldReturnEmptyArrayWhenNoReports() throws Exception {
        mockMvc.perform(get("/quality-measure/reports")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should isolate reports by tenant")
    void shouldIsolateReportsByTenant() throws Exception {
        createSavedReport(TENANT_ID, "Tenant 1 Report", "PATIENT", PATIENT_ID, null);
        createSavedReport("other-tenant", "Tenant 2 Report", "PATIENT", PATIENT_ID, null);

        mockMvc.perform(get("/quality-measure/reports")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tenantId").value(TENANT_ID));
    }

    @Test
    @DisplayName("Should filter reports by type")
    void shouldFilterReportsByType() throws Exception {
        createSavedReport(TENANT_ID, "Patient Report 1", "PATIENT", PATIENT_ID, null);
        createSavedReport(TENANT_ID, "Patient Report 2", "PATIENT", PATIENT_ID_2, null);
        createSavedReport(TENANT_ID, "Population Report", "POPULATION", null, 2024);

        mockMvc.perform(get("/quality-measure/reports")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("type", "PATIENT")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].reportType", everyItem(is("PATIENT"))));
    }

    @Test
    @DisplayName("Should return 400 when tenant ID is missing for get reports")
    void shouldReturn400WhenTenantIdMissingForGetReports() throws Exception {
        mockMvc.perform(get("/quality-measure/reports")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ===== GET REPORT BY ID TESTS =====

    @Test
    @DisplayName("Should retrieve saved report by ID")
    void shouldRetrieveSavedReportById() throws Exception {
        SavedReportEntity saved = createSavedReport(TENANT_ID, "Test Report", "PATIENT", PATIENT_ID, null);

        mockMvc.perform(get("/quality-measure/reports/" + saved.getId())
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.reportName").value("Test Report"))
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID));
    }

    @Test
    @DisplayName("Should return 404 when report not found")
    void shouldReturn404WhenReportNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/quality-measure/reports/" + nonExistentId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should enforce tenant isolation when getting report by ID")
    void shouldEnforceTenantIsolationWhenGettingById() throws Exception {
        SavedReportEntity otherTenantReport = createSavedReport("other-tenant", "Other Tenant Report", "PATIENT", PATIENT_ID, null);

        mockMvc.perform(get("/quality-measure/reports/" + otherTenantReport.getId())
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // Should not find report from different tenant
    }

    @Test
    @DisplayName("Should return 400 when report ID is invalid UUID")
    void shouldReturn400WhenReportIdInvalid() throws Exception {
        mockMvc.perform(get("/quality-measure/reports/invalid-uuid")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ===== DELETE REPORT TESTS =====

    @Test
    @DisplayName("Should delete saved report and return 204 No Content")
    void shouldDeleteSavedReportAndReturn204() throws Exception {
        SavedReportEntity saved = createSavedReport(TENANT_ID, "Report to Delete", "PATIENT", PATIENT_ID, null);

        mockMvc.perform(delete("/quality-measure/reports/" + saved.getId())
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify report is deleted
        mockMvc.perform(get("/quality-measure/reports/" + saved.getId())
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent report")
    void shouldReturn404WhenDeletingNonExistentReport() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(delete("/quality-measure/reports/" + nonExistentId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should enforce tenant isolation when deleting")
    void shouldEnforceTenantIsolationWhenDeleting() throws Exception {
        SavedReportEntity otherTenantReport = createSavedReport("other-tenant", "Other Tenant Report", "PATIENT", PATIENT_ID, null);

        mockMvc.perform(delete("/quality-measure/reports/" + otherTenantReport.getId())
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // Should not delete report from different tenant
    }

    // ===== FIELD VALIDATION TESTS =====

    @Test
    @DisplayName("Should include all expected fields in saved report response")
    void shouldIncludeAllFieldsInResponse() throws Exception {
        SavedReportEntity saved = createSavedReport(TENANT_ID, "Complete Report", "PATIENT", PATIENT_ID, null);

        mockMvc.perform(get("/quality-measure/reports/" + saved.getId())
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tenantId").exists())
                .andExpect(jsonPath("$.reportType").exists())
                .andExpect(jsonPath("$.reportName").exists())
                .andExpect(jsonPath("$.reportData").exists())
                .andExpect(jsonPath("$.createdBy").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.generatedAt").exists())
                .andExpect(jsonPath("$.status").exists());
    }

    // Helper methods
    private void createMeasureResults(String tenantId, UUID patientId) {
        QualityMeasureResultEntity result1 = QualityMeasureResultEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId("HEDIS_CDC_A1C9")
                .measureName("Diabetes Care A1C")
                .measureCategory("HEDIS")
                .measureYear(LocalDate.now().getYear())
                .numeratorCompliant(true)
                .denominatorElligible(true)
                .calculationDate(LocalDate.now())
                .cqlLibrary("HEDIS_CDC")
                .cqlResult("{\"result\": \"test\"}")
                .createdBy("test-user")
                .build();

        QualityMeasureResultEntity result2 = QualityMeasureResultEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId("HEDIS_CBP")
                .measureName("Blood Pressure Control")
                .measureCategory("HEDIS")
                .measureYear(LocalDate.now().getYear())
                .numeratorCompliant(false)
                .denominatorElligible(true)
                .calculationDate(LocalDate.now())
                .cqlLibrary("HEDIS_CBP")
                .cqlResult("{\"result\": \"test\"}")
                .createdBy("test-user")
                .build();

        resultRepository.save(result1);
        resultRepository.save(result2);
    }

    private SavedReportEntity createSavedReport(String tenantId, String reportName, String reportType, UUID patientId, Integer year) {
        SavedReportEntity.SavedReportEntityBuilder builder = SavedReportEntity.builder()
                .tenantId(tenantId)
                .reportType(reportType)
                .reportName(reportName)
                .reportData("{\"test\": \"data\"}")
                .createdBy("test-user")
                .status("COMPLETED");

        if (patientId != null) {
            builder.patientId(patientId);
        }
        if (year != null) {
            builder.year(year);
        }

        return savedReportRepository.save(builder.build());
    }
}
