package com.healthdata.quality.integration;

import com.healthdata.quality.config.TestMessagingConfiguration;
import com.healthdata.quality.config.TestWebSocketConfiguration;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
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
 * Integration tests for Population Quality Report API endpoints
 * Tests the /quality-measure/report/population endpoint
 */
@SpringBootTest
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import({TestMessagingConfiguration.class, TestWebSocketConfiguration.class})
@DisplayName("Population Report API Integration Tests")
class PopulationReportApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QualityMeasureResultRepository repository;

    @Autowired(required = false)
    private CacheManager cacheManager;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID_1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID PATIENT_ID_2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID PATIENT_ID_3 = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        // Clear caches if cache manager is available
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
    }

    @Test
    @DisplayName("Should generate population report for current year")
    void shouldGeneratePopulationReportForCurrentYear() throws Exception {
        int currentYear = LocalDate.now().getYear();

        // Create measures for 2 patients
        createMeasureResult(TENANT_ID, PATIENT_ID_1, "HEDIS_1", "HEDIS", currentYear, true);
        createMeasureResult(TENANT_ID, PATIENT_ID_1, "HEDIS_2", "HEDIS", currentYear, false);
        createMeasureResult(TENANT_ID, PATIENT_ID_2, "CMS_1", "CMS", currentYear, true);
        createMeasureResult(TENANT_ID, PATIENT_ID_2, "CMS_2", "CMS", currentYear, true);

        mockMvc.perform(get("/quality-measure/report/population")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.year").value(currentYear))
                .andExpect(jsonPath("$.uniquePatients").value(2))
                .andExpect(jsonPath("$.totalMeasures").value(4))
                .andExpect(jsonPath("$.compliantMeasures").value(3))
                .andExpect(jsonPath("$.overallScore").value(75.0))
                .andExpect(jsonPath("$.measuresByCategory.HEDIS").value(2))
                .andExpect(jsonPath("$.measuresByCategory.CMS").value(2));
    }

    @Test
    @DisplayName("Should generate population report for specific year")
    void shouldGeneratePopulationReportForSpecificYear() throws Exception {
        // Create measures for 2023
        createMeasureResult(TENANT_ID, PATIENT_ID_1, "HEDIS_1", "HEDIS", 2023, true);
        createMeasureResult(TENANT_ID, PATIENT_ID_2, "HEDIS_2", "HEDIS", 2023, true);

        // Create measures for 2024 (should not be included)
        createMeasureResult(TENANT_ID, PATIENT_ID_1, "HEDIS_3", "HEDIS", 2024, false);

        mockMvc.perform(get("/quality-measure/report/population")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("year", "2023")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2023))
                .andExpect(jsonPath("$.uniquePatients").value(2))
                .andExpect(jsonPath("$.totalMeasures").value(2))
                .andExpect(jsonPath("$.compliantMeasures").value(2))
                .andExpect(jsonPath("$.overallScore").value(100.0));
    }

    @Test
    @DisplayName("Should return 400 when X-Tenant-ID header is missing")
    void shouldReturnBadRequestWhenTenantIdMissing() throws Exception {
        mockMvc.perform(get("/quality-measure/report/population")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should calculate unique patient count correctly")
    void shouldCalculateUniquePatientCount() throws Exception {
        int currentYear = LocalDate.now().getYear();

        // Patient 1 has 3 measures
        createMeasureResult(TENANT_ID, PATIENT_ID_1, "HEDIS_1", "HEDIS", currentYear, true);
        createMeasureResult(TENANT_ID, PATIENT_ID_1, "HEDIS_2", "HEDIS", currentYear, true);
        createMeasureResult(TENANT_ID, PATIENT_ID_1, "HEDIS_3", "HEDIS", currentYear, false);

        // Patient 2 has 2 measures
        createMeasureResult(TENANT_ID, PATIENT_ID_2, "CMS_1", "CMS", currentYear, true);
        createMeasureResult(TENANT_ID, PATIENT_ID_2, "CMS_2", "CMS", currentYear, false);

        // Patient 3 has 1 measure
        createMeasureResult(TENANT_ID, PATIENT_ID_3, "CUSTOM_1", "custom", currentYear, true);

        mockMvc.perform(get("/quality-measure/report/population")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uniquePatients").value(3))
                .andExpect(jsonPath("$.totalMeasures").value(6));
    }

    @Test
    @DisplayName("Should calculate overall score correctly")
    void shouldCalculateOverallScoreCorrectly() throws Exception {
        int currentYear = LocalDate.now().getYear();

        // Create 8 compliant out of 10 total measures
        for (int i = 0; i < 8; i++) {
            createMeasureResult(TENANT_ID, PATIENT_ID_1, "COMPLIANT_" + i, "HEDIS", currentYear, true);
        }
        for (int i = 0; i < 2; i++) {
            createMeasureResult(TENANT_ID, PATIENT_ID_2, "NON_COMPLIANT_" + i, "HEDIS", currentYear, false);
        }

        mockMvc.perform(get("/quality-measure/report/population")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(10))
                .andExpect(jsonPath("$.compliantMeasures").value(8))
                .andExpect(jsonPath("$.overallScore").value(80.0));
    }

    @Test
    @DisplayName("Should group measures by category correctly")
    void shouldGroupMeasuresByCategory() throws Exception {
        int currentYear = LocalDate.now().getYear();

        // Create measures in different categories
        createMeasureResult(TENANT_ID, PATIENT_ID_1, "HEDIS_1", "HEDIS", currentYear, true);
        createMeasureResult(TENANT_ID, PATIENT_ID_1, "HEDIS_2", "HEDIS", currentYear, true);
        createMeasureResult(TENANT_ID, PATIENT_ID_1, "HEDIS_3", "HEDIS", currentYear, true);
        createMeasureResult(TENANT_ID, PATIENT_ID_2, "CMS_1", "CMS", currentYear, true);
        createMeasureResult(TENANT_ID, PATIENT_ID_2, "CMS_2", "CMS", currentYear, false);
        createMeasureResult(TENANT_ID, PATIENT_ID_3, "CUSTOM_1", "custom", currentYear, true);

        mockMvc.perform(get("/quality-measure/report/population")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measuresByCategory.HEDIS").value(3))
                .andExpect(jsonPath("$.measuresByCategory.CMS").value(2))
                .andExpect(jsonPath("$.measuresByCategory.custom").value(1));
    }

    @Test
    @DisplayName("Should isolate population reports by tenant")
    void shouldIsolateReportsByTenant() throws Exception {
        int currentYear = LocalDate.now().getYear();

        // Create measures for test tenant
        createMeasureResult(TENANT_ID, PATIENT_ID_1, "HEDIS_1", "HEDIS", currentYear, true);
        createMeasureResult(TENANT_ID, PATIENT_ID_2, "HEDIS_2", "HEDIS", currentYear, true);

        // Create measures for other tenant
        createMeasureResult("other-tenant", PATIENT_ID_1, "CMS_1", "CMS", currentYear, true);
        createMeasureResult("other-tenant", PATIENT_ID_2, "CMS_2", "CMS", currentYear, true);
        createMeasureResult("other-tenant", PATIENT_ID_3, "CMS_3", "CMS", currentYear, true);

        mockMvc.perform(get("/quality-measure/report/population")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uniquePatients").value(2))
                .andExpect(jsonPath("$.totalMeasures").value(2))
                .andExpect(jsonPath("$.measuresByCategory.HEDIS").value(2))
                .andExpect(jsonPath("$.measuresByCategory.CMS").doesNotExist());
    }

    @Test
    @DisplayName("Should return zero values when no data for year")
    void shouldReturnZeroValuesWhenNoData() throws Exception {
        mockMvc.perform(get("/quality-measure/report/population")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("year", "2020")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2020))
                .andExpect(jsonPath("$.uniquePatients").value(0))
                .andExpect(jsonPath("$.totalMeasures").value(0))
                .andExpect(jsonPath("$.compliantMeasures").value(0))
                .andExpect(jsonPath("$.overallScore").value(0.0));
    }

    @Test
    @DisplayName("Should handle large population efficiently")
    void shouldHandleLargePopulation() throws Exception {
        int currentYear = LocalDate.now().getYear();

        // Create measures for 20 patients
        for (int i = 0; i < 20; i++) {
            UUID patientId = UUID.randomUUID();
            // Each patient has 3 measures, 2 compliant
            createMeasureResult(TENANT_ID, patientId, "MEASURE_1_" + i, "HEDIS", currentYear, true);
            createMeasureResult(TENANT_ID, patientId, "MEASURE_2_" + i, "HEDIS", currentYear, true);
            createMeasureResult(TENANT_ID, patientId, "MEASURE_3_" + i, "CMS", currentYear, false);
        }

        mockMvc.perform(get("/quality-measure/report/population")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uniquePatients").value(20))
                .andExpect(jsonPath("$.totalMeasures").value(60))
                .andExpect(jsonPath("$.compliantMeasures").value(40))
                .andExpect(jsonPath("$.overallScore").value(closeTo(66.67, 0.01)));
    }

    @Test
    @DisplayName("Should not include measures from different years")
    void shouldNotIncludeDifferentYears() throws Exception {
        // Create measures for different years
        createMeasureResult(TENANT_ID, PATIENT_ID_1, "MEASURE_2023", "HEDIS", 2023, true);
        createMeasureResult(TENANT_ID, PATIENT_ID_1, "MEASURE_2024", "HEDIS", 2024, true);
        createMeasureResult(TENANT_ID, PATIENT_ID_1, "MEASURE_2025", "HEDIS", 2025, false);

        // Query for 2024
        mockMvc.perform(get("/quality-measure/report/population")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("year", "2024")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.totalMeasures").value(1))
                .andExpect(jsonPath("$.compliantMeasures").value(1));
    }

    @Test
    @DisplayName("Should calculate score with high precision")
    void shouldCalculateScoreWithPrecision() throws Exception {
        int currentYear = LocalDate.now().getYear();

        // Create 5 compliant out of 7 total measures (71.42857...)
        for (int i = 0; i < 5; i++) {
            createMeasureResult(TENANT_ID, PATIENT_ID_1, "COMPLIANT_" + i, "HEDIS", currentYear, true);
        }
        for (int i = 0; i < 2; i++) {
            createMeasureResult(TENANT_ID, PATIENT_ID_2, "NON_COMPLIANT_" + i, "HEDIS", currentYear, false);
        }

        mockMvc.perform(get("/quality-measure/report/population")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(7))
                .andExpect(jsonPath("$.compliantMeasures").value(5))
                .andExpect(jsonPath("$.overallScore").value(closeTo(71.428, 0.001)));
    }

    @Test
    @DisplayName("Should handle null category as Unknown")
    void shouldHandleNullCategory() throws Exception {
        int currentYear = LocalDate.now().getYear();

        QualityMeasureResultEntity entity = createMeasureResult(
                TENANT_ID, PATIENT_ID_1, "NULL_CAT", null, currentYear, true
        );
        entity.setMeasureCategory(null);
        repository.save(entity);

        mockMvc.perform(get("/quality-measure/report/population")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(1))
                .andExpect(jsonPath("$.measuresByCategory.Unknown").value(1));
    }

    @Test
    @DisplayName("Should support multiple years in database")
    void shouldSupportMultipleYears() throws Exception {
        // Create measures for 2023
        createMeasureResult(TENANT_ID, PATIENT_ID_1, "2023_M1", "HEDIS", 2023, true);
        createMeasureResult(TENANT_ID, PATIENT_ID_2, "2023_M2", "HEDIS", 2023, true);

        // Create measures for 2024
        createMeasureResult(TENANT_ID, PATIENT_ID_1, "2024_M1", "HEDIS", 2024, false);
        createMeasureResult(TENANT_ID, PATIENT_ID_2, "2024_M2", "HEDIS", 2024, false);
        createMeasureResult(TENANT_ID, PATIENT_ID_3, "2024_M3", "HEDIS", 2024, false);

        // Query 2023
        mockMvc.perform(get("/quality-measure/report/population")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("year", "2023")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2023))
                .andExpect(jsonPath("$.uniquePatients").value(2))
                .andExpect(jsonPath("$.totalMeasures").value(2))
                .andExpect(jsonPath("$.overallScore").value(100.0));

        // Query 2024
        mockMvc.perform(get("/quality-measure/report/population")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("year", "2024")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.uniquePatients").value(3))
                .andExpect(jsonPath("$.totalMeasures").value(3))
                .andExpect(jsonPath("$.overallScore").value(0.0));
    }

    // Helper method to create test measure result
    private QualityMeasureResultEntity createMeasureResult(
            String tenantId,
            UUID patientId,
            String measureId,
            String category,
            int year,
            boolean compliant
    ) {
        QualityMeasureResultEntity entity = QualityMeasureResultEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId(measureId)
                .measureName("Test Measure " + measureId)
                .measureCategory(category)
                .measureYear(year)
                .numeratorCompliant(compliant)
                .denominatorElligible(true)
                .complianceRate(compliant ? 100.0 : 0.0)
                .score(compliant ? 95.0 : 50.0)
                .calculationDate(LocalDate.now())
                .cqlLibrary(measureId)
                .cqlResult("{\"result\": \"test\"}")
                .createdBy("integration-test")
                .build();

        return repository.save(entity);
    }
}
