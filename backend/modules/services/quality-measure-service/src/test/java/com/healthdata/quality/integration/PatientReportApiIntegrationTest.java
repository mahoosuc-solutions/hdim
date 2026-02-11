package com.healthdata.quality.integration;

import com.healthdata.quality.config.TestMessagingConfiguration;
import com.healthdata.quality.config.TestWebSocketConfiguration;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import com.healthdata.quality.client.CareGapServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Patient Quality Report API endpoints
 * Tests the /quality-measure/report/patient endpoint
 */
@Tag("integration")
@SpringBootTest
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import({TestMessagingConfiguration.class, TestWebSocketConfiguration.class})
@DisplayName("Patient Report API Integration Tests")
class PatientReportApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QualityMeasureResultRepository repository;

    @MockBean
    private CareGapServiceClient careGapServiceClient;

    @Autowired(required = false)
    private CacheManager cacheManager;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

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
    @DisplayName("Should generate comprehensive patient quality report")
    void shouldGenerateComprehensiveReport() throws Exception {
        // Setup test data
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC", "HEDIS", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CBP", "HEDIS", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "CMS_122", "CMS", false);

        // Mock care gap service
        String careGapResponse = "{\"totalGaps\": 3, \"highPriority\": 1}";
        when(careGapServiceClient.getCareGapSummary(anyString(), any(UUID.class)))
                .thenReturn(careGapResponse);

        mockMvc.perform(get("/quality-measure/report/patient")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.totalMeasures").value(3))
                .andExpect(jsonPath("$.compliantMeasures").value(2))
                .andExpect(jsonPath("$.qualityScore").value(closeTo(66.67, 0.01)))
                .andExpect(jsonPath("$.measuresByCategory").exists())
                .andExpect(jsonPath("$.measuresByCategory.HEDIS").value(2))
                .andExpect(jsonPath("$.measuresByCategory.CMS").value(1))
                .andExpect(jsonPath("$.careGapSummary").exists());

        // Verify care gap service was called
        verify(careGapServiceClient, times(1))
                .getCareGapSummary(eq(TENANT_ID), eq(PATIENT_ID));
    }

    @Test
    @DisplayName("Should generate report with all HEDIS measures")
    void shouldGenerateReportWithHedisMeasures() throws Exception {
        // Create only HEDIS measures
        for (int i = 0; i < 5; i++) {
            createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_" + i, "HEDIS", true);
        }

        mockMvc.perform(get("/quality-measure/report/patient")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(5))
                .andExpect(jsonPath("$.compliantMeasures").value(5))
                .andExpect(jsonPath("$.qualityScore").value(100.0))
                .andExpect(jsonPath("$.measuresByCategory.HEDIS").value(5))
                .andExpect(jsonPath("$.measuresByCategory.CMS").doesNotExist());
    }

    @Test
    @DisplayName("Should generate report with mixed measure categories")
    void shouldGenerateReportWithMixedCategories() throws Exception {
        // Create measures from different categories
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_1", "HEDIS", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_2", "HEDIS", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "CMS_1", "CMS", false);
        createMeasureResult(TENANT_ID, PATIENT_ID, "CUSTOM_1", "custom", true);

        mockMvc.perform(get("/quality-measure/report/patient")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(4))
                .andExpect(jsonPath("$.measuresByCategory.HEDIS").value(2))
                .andExpect(jsonPath("$.measuresByCategory.CMS").value(1))
                .andExpect(jsonPath("$.measuresByCategory.custom").value(1));
    }

    @Test
    @DisplayName("Should generate report when patient has no measures")
    void shouldGenerateReportWithNoMeasures() throws Exception {
        mockMvc.perform(get("/quality-measure/report/patient")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.totalMeasures").value(0))
                .andExpect(jsonPath("$.compliantMeasures").value(0))
                .andExpect(jsonPath("$.qualityScore").value(0.0))
                .andExpect(jsonPath("$.measuresByCategory").exists());
    }

    @Test
    @DisplayName("Should return 400 when X-Tenant-ID header is missing")
    void shouldReturnBadRequestWhenTenantIdMissing() throws Exception {
        mockMvc.perform(get("/quality-measure/report/patient")
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when patient parameter is missing")
    void shouldReturnBadRequestWhenPatientIdMissing() throws Exception {
        mockMvc.perform(get("/quality-measure/report/patient")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should isolate reports by tenant")
    void shouldIsolateReportsByTenant() throws Exception {
        // Create measures for test tenant
        createMeasureResult(TENANT_ID, PATIENT_ID, "MEASURE_1", "HEDIS", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "MEASURE_2", "HEDIS", true);

        // Create measures for other tenant
        createMeasureResult("other-tenant", PATIENT_ID, "MEASURE_3", "CMS", true);

        mockMvc.perform(get("/quality-measure/report/patient")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(2))
                .andExpect(jsonPath("$.measuresByCategory.HEDIS").value(2))
                .andExpect(jsonPath("$.measuresByCategory.CMS").doesNotExist());
    }

    @Test
    @DisplayName("Should handle care gap service unavailability gracefully")
    void shouldHandleCareGapServiceFailure() throws Exception {
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC", "HEDIS", true);

        // Mock care gap service failure
        when(careGapServiceClient.getCareGapSummary(anyString(), any(UUID.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        // Report should still be generated
        mockMvc.perform(get("/quality-measure/report/patient")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.totalMeasures").value(1))
                .andExpect(jsonPath("$.careGapSummary").doesNotExist());
    }

    @Test
    @DisplayName("Should calculate quality score correctly in report")
    void shouldCalculateQualityScoreCorrectly() throws Exception {
        // Create 7 compliant out of 10 measures
        for (int i = 0; i < 7; i++) {
            createMeasureResult(TENANT_ID, PATIENT_ID, "COMPLIANT_" + i, "HEDIS", true);
        }
        for (int i = 0; i < 3; i++) {
            createMeasureResult(TENANT_ID, PATIENT_ID, "NON_COMPLIANT_" + i, "HEDIS", false);
        }

        mockMvc.perform(get("/quality-measure/report/patient")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(10))
                .andExpect(jsonPath("$.compliantMeasures").value(7))
                .andExpect(jsonPath("$.qualityScore").value(70.0));
    }

    @Test
    @DisplayName("Should group measures by category correctly")
    void shouldGroupMeasuresByCategory() throws Exception {
        // Create 3 HEDIS, 2 CMS, 1 custom
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_1", "HEDIS", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_2", "HEDIS", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_3", "HEDIS", false);
        createMeasureResult(TENANT_ID, PATIENT_ID, "CMS_1", "CMS", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "CMS_2", "CMS", false);
        createMeasureResult(TENANT_ID, PATIENT_ID, "CUSTOM_1", "custom", true);

        mockMvc.perform(get("/quality-measure/report/patient")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measuresByCategory.HEDIS").value(3))
                .andExpect(jsonPath("$.measuresByCategory.CMS").value(2))
                .andExpect(jsonPath("$.measuresByCategory.custom").value(1));
    }

    @Test
    @DisplayName("Should handle null category as Unknown")
    void shouldHandleNullCategory() throws Exception {
        QualityMeasureResultEntity entity = createMeasureResult(TENANT_ID, PATIENT_ID, "NULL_CAT", null, true);
        entity.setMeasureCategory(null);
        repository.save(entity);

        mockMvc.perform(get("/quality-measure/report/patient")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(1))
                .andExpect(jsonPath("$.measuresByCategory.Unknown").value(1));
    }

    @Test
    @DisplayName("Should include care gap summary when available")
    void shouldIncludeCareGapSummary() throws Exception {
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC", "HEDIS", false);

        String careGapResponse = """
                {
                    "patientId": "%s",
                    "totalGaps": 5,
                    "highPriority": 2,
                    "mediumPriority": 2,
                    "lowPriority": 1
                }
                """.formatted(PATIENT_ID);

        when(careGapServiceClient.getCareGapSummary(anyString(), any(UUID.class)))
                .thenReturn(careGapResponse);

        mockMvc.perform(get("/quality-measure/report/patient")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careGapSummary").isString())
                .andExpect(jsonPath("$.careGapSummary").value(containsString("totalGaps")));
    }

    // Helper method to create test measure result
    private QualityMeasureResultEntity createMeasureResult(
            String tenantId,
            UUID patientId,
            String measureId,
            String category,
            boolean compliant
    ) {
        QualityMeasureResultEntity entity = QualityMeasureResultEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId(measureId)
                .measureName("Test Measure " + measureId)
                .measureCategory(category)
                .measureYear(LocalDate.now().getYear())
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
