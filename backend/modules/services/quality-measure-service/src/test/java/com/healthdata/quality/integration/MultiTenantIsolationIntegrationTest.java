package com.healthdata.quality.integration;

import com.healthdata.quality.config.TestMessagingConfiguration;
import com.healthdata.quality.config.TestWebSocketConfiguration;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import com.healthdata.quality.client.CqlEngineServiceClient;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.kafka.core.KafkaTemplate;
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
 * Integration tests for multi-tenant isolation
 * Ensures data is properly isolated between tenants
 */
@SpringBootTest
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import({TestMessagingConfiguration.class, TestWebSocketConfiguration.class})
@DisplayName("Multi-Tenant Isolation Integration Tests")
class MultiTenantIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QualityMeasureResultRepository repository;

    @MockBean
    private CqlEngineServiceClient cqlEngineServiceClient;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired(required = false)
    private CacheManager cacheManager;

    private static final String TENANT_1 = "tenant-1";
    private static final String TENANT_2 = "tenant-2";
    private static final UUID PATIENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID PATIENT_ID_2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

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
    @DisplayName("Should isolate measure calculations by tenant")
    void shouldIsolateMeasureCalculationsByTenant() throws Exception {
        String cqlResponse = """
                {
                    "measureResult": {
                        "measureName": "Test Measure",
                        "inNumerator": true,
                        "inDenominator": true
                    }
                }
                """;

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(UUID.class), anyString()))
                .thenReturn(cqlResponse);

        // Calculate measure for tenant 1
        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_1)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "HEDIS_TEST")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(TENANT_1));

        // Calculate measure for tenant 2
        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_2)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "HEDIS_TEST")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(TENANT_2));

        // Verify both tenants have their own data
        var tenant1Results = repository.findByTenantIdAndPatientId(TENANT_1, PATIENT_ID);
        var tenant2Results = repository.findByTenantIdAndPatientId(TENANT_2, PATIENT_ID);

        assert tenant1Results.size() == 1;
        assert tenant2Results.size() == 1;
        assert tenant1Results.get(0).getTenantId().equals(TENANT_1);
        assert tenant2Results.get(0).getTenantId().equals(TENANT_2);
    }

    @Test
    @DisplayName("Should isolate patient results by tenant")
    void shouldIsolatePatientResultsByTenant() throws Exception {
        // Create data for tenant 1
        createMeasureResult(TENANT_1, PATIENT_ID, "HEDIS_1", true);
        createMeasureResult(TENANT_1, PATIENT_ID, "HEDIS_2", false);

        // Create data for tenant 2
        createMeasureResult(TENANT_2, PATIENT_ID, "HEDIS_3", true);

        // Query tenant 1 - should only see tenant 1 data
        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_1)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].tenantId", everyItem(is(TENANT_1))))
                .andExpect(jsonPath("$[*].measureId", hasItems("HEDIS_1", "HEDIS_2")));

        // Query tenant 2 - should only see tenant 2 data
        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_2)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tenantId").value(TENANT_2))
                .andExpect(jsonPath("$[0].measureId").value("HEDIS_3"));
    }

    @Test
    @DisplayName("Should isolate quality scores by tenant")
    void shouldIsolateQualityScoresByTenant() throws Exception {
        // Tenant 1: 3 compliant out of 4 measures (75%)
        createMeasureResult(TENANT_1, PATIENT_ID, "T1_M1", true);
        createMeasureResult(TENANT_1, PATIENT_ID, "T1_M2", true);
        createMeasureResult(TENANT_1, PATIENT_ID, "T1_M3", true);
        createMeasureResult(TENANT_1, PATIENT_ID, "T1_M4", false);

        // Tenant 2: 1 compliant out of 2 measures (50%)
        createMeasureResult(TENANT_2, PATIENT_ID, "T2_M1", true);
        createMeasureResult(TENANT_2, PATIENT_ID, "T2_M2", false);

        // Query tenant 1 score
        mockMvc.perform(get("/quality-measure/score")
                        .header("X-Tenant-ID", TENANT_1)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(4))
                .andExpect(jsonPath("$.compliantMeasures").value(3))
                .andExpect(jsonPath("$.scorePercentage").value(75.0));

        // Query tenant 2 score
        mockMvc.perform(get("/quality-measure/score")
                        .header("X-Tenant-ID", TENANT_2)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(2))
                .andExpect(jsonPath("$.compliantMeasures").value(1))
                .andExpect(jsonPath("$.scorePercentage").value(50.0));
    }

    @Test
    @DisplayName("Should isolate patient reports by tenant")
    void shouldIsolatePatientReportsByTenant() throws Exception {
        // Tenant 1: 2 HEDIS measures
        createMeasureResult(TENANT_1, PATIENT_ID, "HEDIS_1", "HEDIS", true);
        createMeasureResult(TENANT_1, PATIENT_ID, "HEDIS_2", "HEDIS", false);

        // Tenant 2: 1 CMS measure
        createMeasureResult(TENANT_2, PATIENT_ID, "CMS_1", "CMS", true);

        // Query tenant 1 report
        mockMvc.perform(get("/quality-measure/report/patient")
                        .header("X-Tenant-ID", TENANT_1)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(2))
                .andExpect(jsonPath("$.measuresByCategory.HEDIS").value(2))
                .andExpect(jsonPath("$.measuresByCategory.CMS").doesNotExist());

        // Query tenant 2 report
        mockMvc.perform(get("/quality-measure/report/patient")
                        .header("X-Tenant-ID", TENANT_2)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(1))
                .andExpect(jsonPath("$.measuresByCategory.CMS").value(1))
                .andExpect(jsonPath("$.measuresByCategory.HEDIS").doesNotExist());
    }

    @Test
    @DisplayName("Should isolate population reports by tenant")
    void shouldIsolatePopulationReportsByTenant() throws Exception {
        int currentYear = LocalDate.now().getYear();

        // Tenant 1: 2 patients, 4 measures
        createMeasureResultWithYear(TENANT_1, PATIENT_ID, "T1_M1", currentYear, true);
        createMeasureResultWithYear(TENANT_1, PATIENT_ID, "T1_M2", currentYear, true);
        createMeasureResultWithYear(TENANT_1, PATIENT_ID_2, "T1_M3", currentYear, false);
        createMeasureResultWithYear(TENANT_1, PATIENT_ID_2, "T1_M4", currentYear, true);

        // Tenant 2: 1 patient, 2 measures
        createMeasureResultWithYear(TENANT_2, PATIENT_ID, "T2_M1", currentYear, true);
        createMeasureResultWithYear(TENANT_2, PATIENT_ID, "T2_M2", currentYear, true);

        // Query tenant 1 population report
        mockMvc.perform(get("/quality-measure/report/population")
                        .header("X-Tenant-ID", TENANT_1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uniquePatients").value(2))
                .andExpect(jsonPath("$.totalMeasures").value(4))
                .andExpect(jsonPath("$.compliantMeasures").value(3))
                .andExpect(jsonPath("$.overallScore").value(75.0));

        // Query tenant 2 population report
        mockMvc.perform(get("/quality-measure/report/population")
                        .header("X-Tenant-ID", TENANT_2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uniquePatients").value(1))
                .andExpect(jsonPath("$.totalMeasures").value(2))
                .andExpect(jsonPath("$.compliantMeasures").value(2))
                .andExpect(jsonPath("$.overallScore").value(100.0));
    }

    @Test
    @DisplayName("Should not allow tenant to access another tenant's data")
    void shouldNotAllowCrossTenantAccess() throws Exception {
        // Create data for tenant 1
        createMeasureResult(TENANT_1, PATIENT_ID, "HEDIS_1", true);

        // Try to access tenant 1 data using tenant 2 header
        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_2)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0))); // Should see no data
    }

    @Test
    @DisplayName("Should isolate same patient across different tenants")
    void shouldIsolateSamePatientAcrossTenants() throws Exception {
        // Same patient ID used by different tenants
        createMeasureResult(TENANT_1, PATIENT_ID, "HEDIS_1", true);
        createMeasureResult(TENANT_2, PATIENT_ID, "HEDIS_2", false);

        // Verify tenant 1 only sees their data
        var tenant1Results = repository.findByTenantIdAndPatientId(TENANT_1, PATIENT_ID);
        assert tenant1Results.size() == 1;
        assert tenant1Results.get(0).getMeasureId().equals("HEDIS_1");

        // Verify tenant 2 only sees their data
        var tenant2Results = repository.findByTenantIdAndPatientId(TENANT_2, PATIENT_ID);
        assert tenant2Results.size() == 1;
        assert tenant2Results.get(0).getMeasureId().equals("HEDIS_2");
    }

    @Test
    @DisplayName("Should maintain tenant isolation in complex scenarios")
    void shouldMaintainTenantIsolationInComplexScenarios() throws Exception {
        int currentYear = LocalDate.now().getYear();

        // Create overlapping data structure for both tenants
        // Same patients, same measures, different tenants
        createMeasureResultWithYear(TENANT_1, PATIENT_ID, "HEDIS_CDC", currentYear, true);
        createMeasureResultWithYear(TENANT_1, PATIENT_ID_2, "HEDIS_CDC", currentYear, false);

        createMeasureResultWithYear(TENANT_2, PATIENT_ID, "HEDIS_CDC", currentYear, false);
        createMeasureResultWithYear(TENANT_2, PATIENT_ID_2, "HEDIS_CDC", currentYear, true);

        // Verify tenant 1 patient results
        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_1)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numeratorCompliant").value(true));

        // Verify tenant 2 patient results (opposite compliance)
        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", TENANT_2)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numeratorCompliant").value(false));

        // Verify population reports are different
        mockMvc.perform(get("/quality-measure/report/population")
                        .header("X-Tenant-ID", TENANT_1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compliantMeasures").value(1));

        mockMvc.perform(get("/quality-measure/report/population")
                        .header("X-Tenant-ID", TENANT_2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compliantMeasures").value(1));
    }

    @Test
    @DisplayName("Should propagate tenant ID to external services")
    void shouldPropagateTenantIdToExternalServices() throws Exception {
        String cqlResponse = """
                {
                    "measureResult": {
                        "inNumerator": true,
                        "inDenominator": true
                    }
                }
                """;

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(UUID.class), anyString()))
                .thenReturn(cqlResponse);

        // Calculate measure for tenant 1
        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_1)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "HEDIS_TEST")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        // Verify CQL Engine was called with tenant 1 ID
        verify(cqlEngineServiceClient, times(1))
                .evaluateCql(eq(TENANT_1), anyString(), any(UUID.class), anyString());

        // Calculate measure for tenant 2
        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_2)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "HEDIS_TEST")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        // Verify CQL Engine was called with tenant 2 ID
        verify(cqlEngineServiceClient, times(1))
                .evaluateCql(eq(TENANT_2), anyString(), any(UUID.class), anyString());
    }

    @Test
    @DisplayName("Should enforce tenant ID presence in all requests")
    void shouldEnforceTenantIdPresence() throws Exception {
        // All endpoints should require X-Tenant-ID header
        mockMvc.perform(get("/quality-measure/results")
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/quality-measure/score")
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/quality-measure/report/patient")
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/quality-measure/report/population")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle tenant with special characters")
    void shouldHandleTenantWithSpecialCharacters() throws Exception {
        String specialTenant = "tenant-with-dashes_and_underscores.123";

        createMeasureResult(specialTenant, PATIENT_ID, "HEDIS_1", true);

        mockMvc.perform(get("/quality-measure/results")
                        .header("X-Tenant-ID", specialTenant)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tenantId").value(specialTenant));
    }

    // Helper methods
    private QualityMeasureResultEntity createMeasureResult(
            String tenantId,
            UUID patientId,
            String measureId,
            boolean compliant
    ) {
        return createMeasureResultWithYear(
                tenantId, patientId, measureId, "HEDIS", LocalDate.now().getYear(), compliant
        );
    }

    private QualityMeasureResultEntity createMeasureResult(
            String tenantId,
            UUID patientId,
            String measureId,
            String category,
            boolean compliant
    ) {
        return createMeasureResultWithYear(
                tenantId, patientId, measureId, category, LocalDate.now().getYear(), compliant
        );
    }

    private QualityMeasureResultEntity createMeasureResultWithYear(
            String tenantId,
            UUID patientId,
            String measureId,
            int year,
            boolean compliant
    ) {
        return createMeasureResultWithYear(tenantId, patientId, measureId, "HEDIS", year, compliant);
    }

    private QualityMeasureResultEntity createMeasureResultWithYear(
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
