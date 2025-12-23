package com.healthdata.quality.integration;

import com.healthdata.quality.client.CqlEngineServiceClient;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import com.healthdata.quality.service.MeasureCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CQL Engine Service integration
 * Tests the interaction between Quality Measure Service and CQL Engine Service
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CQL Engine Service Integration Tests")
class CqlEngineIntegrationTest {

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

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        reset(cqlEngineServiceClient);
    }

    @Test
    @DisplayName("Should call CQL Engine with correct parameters")
    void shouldCallCqlEngineWithCorrectParameters() throws Exception {
        String cqlResponse = """
                {
                    "measureResult": {
                        "measureName": "Test Measure",
                        "inNumerator": true,
                        "inDenominator": true
                    }
                }
                """;

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), ArgumentMatchers.any(UUID.class), anyString()))
                .thenReturn(cqlResponse);

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "HEDIS_CDC_A1C9")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        // Verify CQL Engine was called with correct parameters
        verify(cqlEngineServiceClient, times(1))
                .evaluateCql(
                        eq(TENANT_ID),
                        eq("HEDIS_CDC_A1C9"),
                        eq(PATIENT_ID),
                        eq("{}")
                );
    }

    @Test
    @DisplayName("Should parse CQL Engine response with all fields")
    void shouldParseCqlResponseWithAllFields() throws Exception {
        String cqlResponse = """
                {
                    "libraryName": "HEDIS_CDC_2024",
                    "measureResult": {
                        "measureName": "Comprehensive Diabetes Care",
                        "inNumerator": true,
                        "inDenominator": true,
                        "complianceRate": 92.5,
                        "score": 88.3
                    }
                }
                """;

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), ArgumentMatchers.any(UUID.class), anyString()))
                .thenReturn(cqlResponse);

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "HEDIS_CDC_A1C9")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.measureName").value("Comprehensive Diabetes Care"))
                .andExpect(jsonPath("$.numeratorCompliant").value(true))
                .andExpect(jsonPath("$.denominatorElligible").value(true))
                .andExpect(jsonPath("$.complianceRate").value(92.5))
                .andExpect(jsonPath("$.score").value(88.3));
    }

    @Test
    @DisplayName("Should parse minimal CQL Engine response")
    void shouldParseMinimalCqlResponse() throws Exception {
        String cqlResponse = """
                {
                    "measureResult": {
                        "inNumerator": false,
                        "inDenominator": true
                    }
                }
                """;

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), ArgumentMatchers.any(UUID.class), anyString()))
                .thenReturn(cqlResponse);

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "HEDIS_TEST")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeratorCompliant").value(false))
                .andExpect(jsonPath("$.denominatorElligible").value(true));
    }

    @Test
    @DisplayName("Should handle CQL Engine timeout")
    void shouldHandleCqlEngineTimeout() throws Exception {
        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), ArgumentMatchers.any(UUID.class), anyString()))
                .thenThrow(new RuntimeException("Connection timeout"));

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "HEDIS_CDC_A1C9")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Should handle CQL Engine service unavailable")
    void shouldHandleCqlEngineUnavailable() throws Exception {
        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), ArgumentMatchers.any(UUID.class), anyString()))
                .thenThrow(new RuntimeException("Service unavailable"));

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "HEDIS_CDC_A1C9")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());

        // Verify no data was persisted
        assert repository.count() == 0;
    }

    @Test
    @DisplayName("Should handle malformed CQL Engine response")
    void shouldHandleMalformedCqlResponse() throws Exception {
        String malformedResponse = "{ invalid json";

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), ArgumentMatchers.any(UUID.class), anyString()))
                .thenReturn(malformedResponse);

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "HEDIS_CDC_A1C9")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Should extract measure name from library name when not in result")
    void shouldExtractMeasureNameFromLibrary() throws Exception {
        String cqlResponse = """
                {
                    "libraryName": "HEDIS Diabetes Care 2024",
                    "measureResult": {
                        "inNumerator": true,
                        "inDenominator": true
                    }
                }
                """;

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), ArgumentMatchers.any(UUID.class), anyString()))
                .thenReturn(cqlResponse);

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "HEDIS_CDC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.measureName").value("HEDIS Diabetes Care 2024"));
    }

    @Test
    @DisplayName("Should use measure ID as fallback for measure name")
    void shouldUseMeasureIdAsFallbackName() throws Exception {
        String cqlResponse = """
                {
                    "measureResult": {
                        "inNumerator": true,
                        "inDenominator": true
                    }
                }
                """;

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), ArgumentMatchers.any(UUID.class), anyString()))
                .thenReturn(cqlResponse);

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "CUSTOM_MEASURE_001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.measureName").value("CUSTOM MEASURE 001"));
    }

    @Test
    @DisplayName("Should handle CQL response with score but no compliance rate")
    void shouldHandleScoreWithoutComplianceRate() throws Exception {
        String cqlResponse = """
                {
                    "measureResult": {
                        "inNumerator": true,
                        "inDenominator": true,
                        "score": 95.0
                    }
                }
                """;

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), ArgumentMatchers.any(UUID.class), anyString()))
                .thenReturn(cqlResponse);

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "HEDIS_TEST")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(95.0))
                .andExpect(jsonPath("$.complianceRate").doesNotExist());
    }

    @Test
    @DisplayName("Should handle CQL response with top-level score")
    void shouldHandleTopLevelScore() throws Exception {
        String cqlResponse = """
                {
                    "score": 78.5,
                    "measureResult": {
                        "inNumerator": true,
                        "inDenominator": true
                    }
                }
                """;

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), ArgumentMatchers.any(UUID.class), anyString()))
                .thenReturn(cqlResponse);

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "HEDIS_TEST")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(78.5));
    }

    @Test
    @DisplayName("Should store raw CQL result for auditing")
    void shouldStoreRawCqlResult() throws Exception {
        String cqlResponse = """
                {
                    "measureResult": {
                        "inNumerator": true,
                        "inDenominator": true
                    }
                }
                """;

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), ArgumentMatchers.any(UUID.class), anyString()))
                .thenReturn(cqlResponse);

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "HEDIS_TEST")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        // Verify raw result was stored
        var results = repository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_ID);
        assert results.size() == 1;
        assert results.get(0).getCqlResult() != null;
        assert results.get(0).getCqlResult().contains("measureResult");
    }

    @Test
    @DisplayName("Should call CQL Engine for different measure types")
    void shouldCallCqlEngineForDifferentMeasures() throws Exception {
        String cqlResponse = """
                {
                    "measureResult": {
                        "inNumerator": true,
                        "inDenominator": true
                    }
                }
                """;

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), ArgumentMatchers.any(UUID.class), anyString()))
                .thenReturn(cqlResponse);

        // Test HEDIS measure
        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "HEDIS_CDC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        // Test CMS measure
        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "CMS_122")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        // Verify both calls were made
        verify(cqlEngineServiceClient, times(1))
                .evaluateCql(eq(TENANT_ID), eq("HEDIS_CDC"), eq(PATIENT_ID), eq("{}"));
        verify(cqlEngineServiceClient, times(1))
                .evaluateCql(eq(TENANT_ID), eq("CMS_122"), eq(PATIENT_ID), eq("{}"));
    }

    @Test
    @DisplayName("Should handle empty CQL response")
    void shouldHandleEmptyCqlResponse() throws Exception {
        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), ArgumentMatchers.any(UUID.class), anyString()))
                .thenReturn("{}");

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "HEDIS_TEST")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeratorCompliant").value(false))
                .andExpect(jsonPath("$.denominatorElligible").value(true));
    }

    @Test
    @DisplayName("Should propagate tenant ID to CQL Engine")
    void shouldPropagateTenantId() throws Exception {
        String cqlResponse = """
                {
                    "measureResult": {
                        "inNumerator": true,
                        "inDenominator": true
                    }
                }
                """;

        when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), ArgumentMatchers.any(UUID.class), anyString()))
                .thenReturn(cqlResponse);

        String customTenantId = "custom-tenant-123";

        mockMvc.perform(post("/quality-measure/calculate")
                        .header("X-Tenant-ID", customTenantId)
                        .param("patient", PATIENT_ID.toString())
                        .param("measure", "HEDIS_TEST")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        // Verify tenant ID was passed to CQL Engine
        verify(cqlEngineServiceClient, times(1))
                .evaluateCql(eq(customTenantId), anyString(), ArgumentMatchers.any(UUID.class), anyString());
    }
}
