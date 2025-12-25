package com.healthdata.quality.integration;

import com.healthdata.quality.config.TestMessagingConfiguration;
import com.healthdata.quality.config.TestWebSocketConfiguration;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Integration tests for Results Retrieval API endpoints
 * Tests the /quality-measure/results endpoint
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import({TestMessagingConfiguration.class, TestWebSocketConfiguration.class})
@DisplayName("Results API Integration Tests")
class ResultsApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QualityMeasureResultRepository repository;

    @Autowired
    private CacheManager cacheManager;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID PATIENT_ID_2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        // Clear all caches before each test to prevent data pollution
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    @DisplayName("Should retrieve all results for a patient")
    void shouldRetrieveAllPatientResults() throws Exception {
        // Setup test data - create 3 results for the patient
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC_A1C9", "Diabetes Care A1C", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CBP", "Controlling Blood Pressure", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_BCS", "Breast Cancer Screening", false);

        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].patientId", everyItem(is(PATIENT_ID.toString()))))
                .andExpect(jsonPath("$[*].tenantId", everyItem(is(TENANT_ID))))
                .andExpect(jsonPath("$[0].measureId").exists())
                .andExpect(jsonPath("$[0].measureName").exists())
                .andExpect(jsonPath("$[0].numeratorCompliant").exists())
                .andExpect(jsonPath("$[0].calculationDate").exists());
    }

    @Test
    @DisplayName("Should return empty array when patient has no results")
    void shouldReturnEmptyArrayWhenNoResults() throws Exception {
        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should return 400 when X-Tenant-ID header is missing")
    void shouldReturnBadRequestWhenTenantIdMissing() throws Exception {
        mockMvc.perform(get("/quality-measure/results")
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return all results when patient parameter is missing or empty")
    void shouldReturnAllResultsWhenPatientIdMissing() throws Exception {
        // Create results for multiple patients
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC_A1C9", "Patient 1 Measure 1", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CBP", "Patient 1 Measure 2", false);
        createMeasureResult(TENANT_ID, PATIENT_ID_2, "HEDIS_BCS", "Patient 2 Measure", true);

        // Query without patient parameter - should get all 3 results
        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].tenantId", everyItem(is(TENANT_ID))));
    }

    @Test
    @DisplayName("Should return all results when patient parameter is empty string")
    void shouldReturnAllResultsWhenPatientIdEmpty() throws Exception {
        // Create results for multiple patients
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC_A1C9", "Patient 1 Measure", true);
        createMeasureResult(TENANT_ID, PATIENT_ID_2, "HEDIS_CBP", "Patient 2 Measure", true);

        // Query with empty patient parameter - should get all 2 results
        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].tenantId", everyItem(is(TENANT_ID))));
    }

    @Test
    @DisplayName("Should isolate all results by tenant when patient not specified")
    void shouldIsolateAllResultsByTenantWhenNoPatient() throws Exception {
        // Create results for different tenants and patients
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC_A1C9", "Tenant 1 Patient 1", true);
        createMeasureResult(TENANT_ID, PATIENT_ID_2, "HEDIS_CBP", "Tenant 1 Patient 2", true);
        createMeasureResult("other-tenant", PATIENT_ID, "HEDIS_BCS", "Tenant 2 Patient 1", true);

        // Query without patient - should only get results for specified tenant
        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].tenantId", everyItem(is(TENANT_ID))));

        // Query for other tenant
        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", "other-tenant")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tenantId").value("other-tenant"));
    }

    @Test
    @DisplayName("Should support pagination with page and size parameters")
    void shouldSupportPaginationForAllResults() throws Exception {
        // Create 5 results
        for (int i = 0; i < 5; i++) {
            createMeasureResult(TENANT_ID, PATIENT_ID, "MEASURE_" + i, "Measure " + i, true);
        }

        // Get first page with size 2
        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("page", "0")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Get second page with size 2
        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("page", "1")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Get third page with size 2 (should have 1 result)
        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("page", "2")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Should isolate results by tenant")
    void shouldIsolateResultsByTenant() throws Exception {
        // Create results for different tenants
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC_A1C9", "Measure 1", true);
        createMeasureResult("other-tenant", PATIENT_ID, "HEDIS_CBP", "Measure 2", true);

        // Query with first tenant - should only see one result
        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tenantId").value(TENANT_ID))
                .andExpect(jsonPath("$[0].measureId").value("HEDIS_CDC_A1C9"));

        // Query with other tenant - should only see one result
        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", "other-tenant")
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tenantId").value("other-tenant"))
                .andExpect(jsonPath("$[0].measureId").value("HEDIS_CBP"));
    }

    @Test
    @DisplayName("Should not return results for other patients")
    void shouldNotReturnOtherPatientsResults() throws Exception {
        // Create results for different patients
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC_A1C9", "Patient 1 Measure", true);
        createMeasureResult(TENANT_ID, PATIENT_ID_2, "HEDIS_CBP", "Patient 2 Measure", true);

        // Query for patient 1 - should only see their result
        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$[0].measureName").value("Patient 1 Measure"));
    }

    @Test
    @DisplayName("Should include all measure result fields in response")
    void shouldIncludeAllFieldsInResponse() throws Exception {
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC_A1C9", "Complete Measure", true);

        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].tenantId").exists())
                .andExpect(jsonPath("$[0].patientId").exists())
                .andExpect(jsonPath("$[0].measureId").exists())
                .andExpect(jsonPath("$[0].measureName").exists())
                .andExpect(jsonPath("$[0].measureCategory").exists())
                .andExpect(jsonPath("$[0].measureYear").exists())
                .andExpect(jsonPath("$[0].numeratorCompliant").exists())
                .andExpect(jsonPath("$[0].denominatorElligible").exists())
                .andExpect(jsonPath("$[0].calculationDate").exists())
                .andExpect(jsonPath("$[0].cqlLibrary").exists())
                .andExpect(jsonPath("$[0].createdBy").exists())
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    @DisplayName("Should return results with different compliance statuses")
    void shouldReturnResultsWithDifferentComplianceStatuses() throws Exception {
        // Create both compliant and non-compliant results
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC_A1C9", "Compliant Measure", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CBP", "Non-Compliant Measure", false);

        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.numeratorCompliant == true)]").exists())
                .andExpect(jsonPath("$[?(@.numeratorCompliant == false)]").exists());
    }

    @Test
    @DisplayName("Should return results from different years")
    void shouldReturnResultsFromDifferentYears() throws Exception {
        // Create results with different years
        QualityMeasureResultEntity result2023 = createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC_A1C9", "2023 Measure", true);
        result2023.setMeasureYear(2023);
        repository.save(result2023);

        QualityMeasureResultEntity result2024 = createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CBP", "2024 Measure", true);
        result2024.setMeasureYear(2024);
        repository.save(result2024);

        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.measureYear == 2023)]").exists())
                .andExpect(jsonPath("$[?(@.measureYear == 2024)]").exists());
    }

    @Test
    @DisplayName("Should return results with different measure categories")
    void shouldReturnResultsWithDifferentCategories() throws Exception {
        // Create results with different categories
        QualityMeasureResultEntity hedis = createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC", "HEDIS Measure", true);
        hedis.setMeasureCategory("HEDIS");
        repository.save(hedis);

        QualityMeasureResultEntity cms = createMeasureResult(TENANT_ID, PATIENT_ID, "CMS_122", "CMS Measure", true);
        cms.setMeasureCategory("CMS");
        repository.save(cms);

        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.measureCategory == 'HEDIS')]").exists())
                .andExpect(jsonPath("$[?(@.measureCategory == 'CMS')]").exists());
    }

    @Test
    @DisplayName("Should handle invalid UUID format for patient ID")
    void shouldHandleInvalidPatientIdFormat() throws Exception {
        // Invalid UUID format should return 400 Bad Request (client error)
        // However, current implementation throws 500, which we'll improve later
        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "invalid-uuid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    // Helper method to create test measure result
    private QualityMeasureResultEntity createMeasureResult(
            String tenantId,
            UUID patientId,
            String measureId,
            String measureName,
            boolean compliant
    ) {
        QualityMeasureResultEntity entity = QualityMeasureResultEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId(measureId)
                .measureName(measureName)
                .measureCategory(measureId.startsWith("HEDIS_") ? "HEDIS" : measureId.startsWith("CMS_") ? "CMS" : "custom")
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
