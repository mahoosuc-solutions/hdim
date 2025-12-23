package com.healthdata.quality.integration;

import com.healthdata.quality.dto.QualityMeasureResultDTO;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import com.healthdata.quality.client.CqlEngineServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Measure Calculation API endpoints
 * Tests the /quality-measure/calculate endpoint
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Measure Calculation API Integration Tests")
class MeasureCalculationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QualityMeasureResultRepository repository;

    @MockBean
    private CqlEngineServiceClient cqlEngineServiceClient;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String MEASURE_ID = "HEDIS_CDC_A1C9";

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Should calculate measure successfully with valid inputs")
    void shouldCalculateMeasureSuccessfully() throws Exception {
        // Mock CQL Engine response
        String cqlResponse = """
                {
                    "libraryName": "HEDIS_CDC_2024",
                    "measureResult": {
                        "measureName": "Comprehensive Diabetes Care: HbA1c Control (<9.0%)",
                        "inNumerator": true,
                        "inDenominator": true,
                        "complianceRate": 85.5,
                        "score": 92.3
                    }
                }
                """;

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(UUID.class), anyString()))
                .thenReturn(cqlResponse);

        // Execute request
        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", MEASURE_ID)
                        .param("createdBy", "integration-test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID))
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.measureId").value(MEASURE_ID))
                .andExpect(jsonPath("$.measureName").value("Comprehensive Diabetes Care: HbA1c Control (<9.0%)"))
                .andExpect(jsonPath("$.measureCategory").value("HEDIS"))
                .andExpect(jsonPath("$.numeratorCompliant").value(true))
                .andExpect(jsonPath("$.denominatorElligible").value(true))
                .andExpect(jsonPath("$.complianceRate").value(85.5))
                .andExpect(jsonPath("$.score").value(92.3))
                .andExpect(jsonPath("$.cqlLibrary").value(MEASURE_ID))
                .andExpect(jsonPath("$.createdBy").value("integration-test"))
                .andExpect(jsonPath("$.calculationDate").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        // Verify CQL Engine was called
        verify(cqlEngineServiceClient, times(1))
                .evaluateCql(eq(TENANT_ID), eq(MEASURE_ID), eq(PATIENT_ID), eq("{}"));

        // Verify Kafka event was published
        verify(kafkaTemplate, times(1))
                .send(eq("measure-calculated"), anyString());
    }

    @Test
    @DisplayName("Should calculate measure with default createdBy when not provided")
    void shouldUseDefaultCreatedBy() throws Exception {
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

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", MEASURE_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value("system"));
    }

    @Test
    @DisplayName("Should return 400 when X-Tenant-ID header is missing")
    void shouldReturnBadRequestWhenTenantIdMissing() throws Exception {
        mockMvc.perform(post("/quality-measure/calculate")
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", MEASURE_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Verify CQL Engine was not called
        verify(cqlEngineServiceClient, never())
                .evaluateCql(anyString(), anyString(), any(UUID.class), anyString());
    }

    @Test
    @DisplayName("Should return 400 when patient parameter is missing")
    void shouldReturnBadRequestWhenPatientIdMissing() throws Exception {
        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("measure", MEASURE_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when measure parameter is missing")
    void shouldReturnBadRequestWhenMeasureIdMissing() throws Exception {
        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle CQL Engine failures gracefully")
    void shouldHandleCqlEngineFailure() throws Exception {
        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(UUID.class), anyString()))
                .thenThrow(new RuntimeException("CQL Engine unavailable"));

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", MEASURE_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Should calculate CMS measure with correct category")
    void shouldCalculateCmsMeasure() throws Exception {
        String cqlResponse = """
                {
                    "measureResult": {
                        "measureName": "CMS Quality Measure",
                        "inNumerator": true,
                        "inDenominator": true
                    }
                }
                """;

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(UUID.class), anyString()))
                .thenReturn(cqlResponse);

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "CMS_122")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.measureCategory").value("CMS"));
    }

    @Test
    @DisplayName("Should calculate custom measure with correct category")
    void shouldCalculateCustomMeasure() throws Exception {
        String cqlResponse = """
                {
                    "measureResult": {
                        "measureName": "Custom Quality Measure",
                        "inNumerator": false,
                        "inDenominator": true
                    }
                }
                """;

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(UUID.class), anyString()))
                .thenReturn(cqlResponse);

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "CUSTOM_001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.measureCategory").value("custom"));
    }

    @Test
    @DisplayName("Should persist calculated measure to database")
    void shouldPersistCalculatedMeasure() throws Exception {
        String cqlResponse = """
                {
                    "measureResult": {
                        "measureName": "Test Measure",
                        "inNumerator": true,
                        "inDenominator": true,
                        "score": 95.0
                    }
                }
                """;

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(UUID.class), anyString()))
                .thenReturn(cqlResponse);

        // Before calculation
        long countBefore = repository.count();

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", MEASURE_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        // After calculation
        long countAfter = repository.count();
        assert countAfter == countBefore + 1;

        // Verify the persisted entity
        var results = repository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_ID);
        assert results.size() == 1;
        QualityMeasureResultEntity entity = results.get(0);
        assert entity.getMeasureId().equals(MEASURE_ID);
        assert entity.getNumeratorCompliant();
        assert entity.getDenominatorElligible();
    }

    @Test
    @DisplayName("Should handle non-compliant measure results")
    void shouldHandleNonCompliantResults() throws Exception {
        String cqlResponse = """
                {
                    "measureResult": {
                        "measureName": "Failed Measure",
                        "inNumerator": false,
                        "inDenominator": true,
                        "complianceRate": 0.0,
                        "score": 0.0
                    }
                }
                """;

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(UUID.class), anyString()))
                .thenReturn(cqlResponse);

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", MEASURE_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeratorCompliant").value(false))
                .andExpect(jsonPath("$.denominatorElligible").value(true))
                .andExpect(jsonPath("$.complianceRate").value(0.0));
    }
}
