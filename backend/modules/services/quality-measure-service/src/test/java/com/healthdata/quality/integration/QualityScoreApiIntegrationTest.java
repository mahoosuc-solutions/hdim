package com.healthdata.quality.integration;

import com.healthdata.quality.config.TestMessagingConfiguration;
import com.healthdata.quality.config.TestWebSocketConfiguration;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Quality Score API endpoints
 * Tests the /quality-measure/score endpoint
 */
@Tag("integration")
@SpringBootTest
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import({TestMessagingConfiguration.class, TestWebSocketConfiguration.class})
@DisplayName("Quality Score API Integration Tests")
class QualityScoreApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QualityMeasureResultRepository repository;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Should calculate quality score with all compliant measures")
    void shouldCalculateScoreAllCompliant() throws Exception {
        // Create 5 compliant measures
        for (int i = 0; i < 5; i++) {
            createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_" + i, true);
        }

        mockMvc.perform(get("/quality-measure/score")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalMeasures").value(5))
                .andExpect(jsonPath("$.compliantMeasures").value(5))
                .andExpect(jsonPath("$.scorePercentage").value(100.0));
    }

    @Test
    @DisplayName("Should calculate quality score with no compliant measures")
    void shouldCalculateScoreNoneCompliant() throws Exception {
        // Create 3 non-compliant measures
        for (int i = 0; i < 3; i++) {
            createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_" + i, false);
        }

        mockMvc.perform(get("/quality-measure/score")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(3))
                .andExpect(jsonPath("$.compliantMeasures").value(0))
                .andExpect(jsonPath("$.scorePercentage").value(0.0));
    }

    @Test
    @DisplayName("Should calculate quality score with mixed compliance")
    void shouldCalculateScoreMixedCompliance() throws Exception {
        // Create 6 compliant and 4 non-compliant measures
        for (int i = 0; i < 6; i++) {
            createMeasureResult(TENANT_ID, PATIENT_ID, "COMPLIANT_" + i, true);
        }
        for (int i = 0; i < 4; i++) {
            createMeasureResult(TENANT_ID, PATIENT_ID, "NON_COMPLIANT_" + i, false);
        }

        mockMvc.perform(get("/quality-measure/score")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(10))
                .andExpect(jsonPath("$.compliantMeasures").value(6))
                .andExpect(jsonPath("$.scorePercentage").value(60.0));
    }

    @Test
    @DisplayName("Should return zero score when patient has no measures")
    void shouldReturnZeroScoreWhenNoMeasures() throws Exception {
        mockMvc.perform(get("/quality-measure/score")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(0))
                .andExpect(jsonPath("$.compliantMeasures").value(0))
                .andExpect(jsonPath("$.scorePercentage").value(0.0));
    }

    @Test
    @DisplayName("Should return 400 when X-Tenant-ID header is missing")
    void shouldReturnBadRequestWhenTenantIdMissing() throws Exception {
        mockMvc.perform(get("/quality-measure/score")
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when patient parameter is missing")
    void shouldReturnBadRequestWhenPatientIdMissing() throws Exception {
        mockMvc.perform(get("/quality-measure/score")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should calculate correct percentage with single measure")
    void shouldCalculateCorrectPercentageWithSingleMeasure() throws Exception {
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC", true);

        mockMvc.perform(get("/quality-measure/score")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(1))
                .andExpect(jsonPath("$.compliantMeasures").value(1))
                .andExpect(jsonPath("$.scorePercentage").value(100.0));
    }

    @Test
    @DisplayName("Should isolate quality scores by tenant")
    void shouldIsolateScoresByTenant() throws Exception {
        // Create measures for test tenant (3 compliant, 1 non-compliant)
        for (int i = 0; i < 3; i++) {
            createMeasureResult(TENANT_ID, PATIENT_ID, "MEASURE_" + i, true);
        }
        createMeasureResult(TENANT_ID, PATIENT_ID, "MEASURE_3", false);

        // Create measures for other tenant
        createMeasureResult("other-tenant", PATIENT_ID, "OTHER_MEASURE", true);

        mockMvc.perform(get("/quality-measure/score")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(4))
                .andExpect(jsonPath("$.compliantMeasures").value(3))
                .andExpect(jsonPath("$.scorePercentage").value(75.0));
    }

    @Test
    @DisplayName("Should calculate score for specific patient only")
    void shouldCalculateScoreForSpecificPatient() throws Exception {
        UUID otherPatientId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        // Create measures for target patient
        createMeasureResult(TENANT_ID, PATIENT_ID, "MEASURE_1", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "MEASURE_2", false);

        // Create measures for other patient
        createMeasureResult(TENANT_ID, otherPatientId, "MEASURE_3", true);

        mockMvc.perform(get("/quality-measure/score")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(2))
                .andExpect(jsonPath("$.compliantMeasures").value(1))
                .andExpect(jsonPath("$.scorePercentage").value(50.0));
    }

    @Test
    @DisplayName("Should calculate score with high precision")
    void shouldCalculateScoreWithPrecision() throws Exception {
        // Create 3 compliant out of 7 total measures (42.857...)
        for (int i = 0; i < 3; i++) {
            createMeasureResult(TENANT_ID, PATIENT_ID, "COMPLIANT_" + i, true);
        }
        for (int i = 0; i < 4; i++) {
            createMeasureResult(TENANT_ID, PATIENT_ID, "NON_COMPLIANT_" + i, false);
        }

        mockMvc.perform(get("/quality-measure/score")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(7))
                .andExpect(jsonPath("$.compliantMeasures").value(3))
                .andExpect(jsonPath("$.scorePercentage").value(closeTo(42.857, 0.001)));
    }

    @Test
    @DisplayName("Should handle large number of measures efficiently")
    void shouldHandleLargeNumberOfMeasures() throws Exception {
        // Create 50 measures (30 compliant, 20 non-compliant)
        for (int i = 0; i < 30; i++) {
            createMeasureResult(TENANT_ID, PATIENT_ID, "COMPLIANT_" + i, true);
        }
        for (int i = 0; i < 20; i++) {
            createMeasureResult(TENANT_ID, PATIENT_ID, "NON_COMPLIANT_" + i, false);
        }

        mockMvc.perform(get("/quality-measure/score")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(50))
                .andExpect(jsonPath("$.compliantMeasures").value(30))
                .andExpect(jsonPath("$.scorePercentage").value(60.0));
    }

    @Test
    @DisplayName("Should handle invalid UUID format for patient ID")
    void shouldHandleInvalidPatientIdFormat() throws Exception {
        mockMvc.perform(get("/quality-measure/score")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "invalid-uuid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should include all measures regardless of year")
    void shouldIncludeAllMeasuresRegardlessOfYear() throws Exception {
        // Create measures from different years
        QualityMeasureResultEntity result2023 = createMeasureResult(TENANT_ID, PATIENT_ID, "MEASURE_2023", true);
        result2023.setMeasureYear(2023);
        repository.save(result2023);

        QualityMeasureResultEntity result2024 = createMeasureResult(TENANT_ID, PATIENT_ID, "MEASURE_2024", true);
        result2024.setMeasureYear(2024);
        repository.save(result2024);

        mockMvc.perform(get("/quality-measure/score")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMeasures").value(2))
                .andExpect(jsonPath("$.compliantMeasures").value(2))
                .andExpect(jsonPath("$.scorePercentage").value(100.0));
    }

    // Helper method to create test measure result
    private QualityMeasureResultEntity createMeasureResult(
            String tenantId,
            UUID patientId,
            String measureId,
            boolean compliant
    ) {
        QualityMeasureResultEntity entity = QualityMeasureResultEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId(measureId)
                .measureName("Test Measure " + measureId)
                .measureCategory("HEDIS")
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
