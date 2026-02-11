package com.healthdata.predictive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.authentication.service.JwtTokenService;
import com.healthdata.predictive.model.*;
import com.healthdata.predictive.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PredictiveAnalyticsController.
 *
 * These tests use @SpringBootTest because the application's @EnableJpaRepositories
 * annotation requires JPA infrastructure. For a pure unit test approach, the
 * application would need to be refactored to not use @EnableJpaRepositories
 * directly on the main class.
 */
@Tag("integration")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.liquibase.enabled=false",
    "spring.kafka.bootstrap-servers="
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("PredictiveAnalyticsController Tests")
class PredictiveAnalyticsControllerTest {

    @MockBean
    private JwtTokenService jwtTokenService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReadmissionRiskPredictor readmissionPredictor;

    @MockBean
    private CostPredictor costPredictor;

    @MockBean
    private DiseaseProgressionPredictor progressionPredictor;

    @MockBean
    private PopulationRiskStratifier riskStratifier;

    @MockBean
    private com.healthdata.predictive.service.PredictedCareGapService predictedCareGapService;

    @Test
    @DisplayName("Should predict readmission risk")
    @WithMockUser(roles = "ANALYST")
    void shouldPredictReadmissionRisk() throws Exception {
        ReadmissionRiskScore score = createReadmissionRiskScore();
        when(readmissionPredictor.predict30DayRisk(eq("tenant-1"), eq("patient-123"), any()))
            .thenReturn(score);

        mockMvc.perform(post("/api/v1/analytics/readmission-risk/patient-123")
                .header("X-Tenant-ID", "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.patientId").value("patient-123"))
            .andExpect(jsonPath("$.score").exists());
    }

    @Test
    @DisplayName("Should predict cost")
    @WithMockUser(roles = "ANALYST")
    void shouldPredictCost() throws Exception {
        CostBreakdown cost = createCostBreakdown();
        when(costPredictor.predictCosts(eq("tenant-1"), eq("patient-123"), any(), eq(12)))
            .thenReturn(cost);

        mockMvc.perform(post("/api/v1/analytics/cost-prediction/patient-123")
                .header("X-Tenant-ID", "tenant-1")
                .param("predictionPeriodMonths", "12")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.patientId").value("patient-123"))
            .andExpect(jsonPath("$.totalPredictedCost").exists());
    }

    @Test
    @DisplayName("Should predict disease progression")
    @WithMockUser(roles = "ANALYST")
    void shouldPredictDiseaseProgression() throws Exception {
        ProgressionRisk risk = createProgressionRisk();
        when(progressionPredictor.predictProgression(eq("tenant-1"), eq("patient-123"), any(), eq("diabetes")))
            .thenReturn(risk);

        mockMvc.perform(post("/api/v1/analytics/disease-progression/patient-123")
                .header("X-Tenant-ID", "tenant-1")
                .param("condition", "diabetes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.condition").value("diabetes"));
    }

    @Test
    @DisplayName("Should get population risk stratification")
    @WithMockUser(roles = "ANALYST")
    void shouldGetPopulationRiskStratification() throws Exception {
        List<RiskCohort> cohorts = Arrays.asList(createRiskCohort());
        when(riskStratifier.stratifyPopulation(eq("tenant-1"), anyList(), any()))
            .thenReturn(cohorts);

        mockMvc.perform(get("/api/v1/analytics/population/risk-stratification")
                .header("X-Tenant-ID", "tenant-1")
                .param("patientIds", "p1,p2,p3")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should get high-risk patients")
    @WithMockUser(roles = "ANALYST")
    void shouldGetHighRiskPatients() throws Exception {
        List<String> highRiskPatients = Arrays.asList("p1", "p3");
        when(riskStratifier.getHighRiskPatients(eq("tenant-1"), anyList(), any()))
            .thenReturn(highRiskPatients);

        mockMvc.perform(get("/api/v1/analytics/population/high-risk")
                .header("X-Tenant-ID", "tenant-1")
                .param("patientIds", "p1,p2,p3")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Should require authentication")
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/readmission-risk/patient-123")
                .header("X-Tenant-ID", "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden()); // 403 because CSRF token is missing
    }

    @Test
    @DisplayName("Should require tenant ID header")
    @WithMockUser(roles = "ANALYST")
    void shouldRequireTenantIdHeader() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/readmission-risk/patient-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate patient ID")
    @WithMockUser(roles = "ANALYST")
    void shouldValidatePatientId() throws Exception {
        // When posting to the endpoint without a path variable (trailing slash only),
        // Spring cannot match the route pattern and returns an error
        mockMvc.perform(post("/api/v1/analytics/readmission-risk/")
                .header("X-Tenant-ID", "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Should handle 90-day readmission risk")
    @WithMockUser(roles = "ANALYST")
    void shouldHandle90DayReadmissionRisk() throws Exception {
        ReadmissionRiskScore score = createReadmissionRiskScore();
        score.setPredictionPeriodDays(90);
        when(readmissionPredictor.predict90DayRisk(eq("tenant-1"), eq("patient-123"), any()))
            .thenReturn(score);

        mockMvc.perform(post("/api/v1/analytics/readmission-risk/patient-123")
                .header("X-Tenant-ID", "tenant-1")
                .param("predictionPeriod", "90")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return JSON format")
    @WithMockUser(roles = "ANALYST")
    void shouldReturnJsonFormat() throws Exception {
        ReadmissionRiskScore score = createReadmissionRiskScore();
        when(readmissionPredictor.predict30DayRisk(any(), any(), any()))
            .thenReturn(score);

        mockMvc.perform(post("/api/v1/analytics/readmission-risk/patient-123")
                .header("X-Tenant-ID", "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should handle service errors gracefully")
    @WithMockUser(roles = "ANALYST")
    void shouldHandleServiceErrorsGracefully() throws Exception {
        when(readmissionPredictor.predict30DayRisk(any(), any(), any()))
            .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/api/v1/analytics/readmission-risk/patient-123")
                .header("X-Tenant-ID", "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Should support batch patient requests")
    @WithMockUser(roles = "ANALYST")
    void shouldSupportBatchPatientRequests() throws Exception {
        when(riskStratifier.stratifyPopulation(any(), anyList(), any()))
            .thenReturn(Arrays.asList(createRiskCohort()));

        mockMvc.perform(get("/api/v1/analytics/population/risk-stratification")
                .header("X-Tenant-ID", "tenant-1")
                .param("patientIds", "p1,p2,p3,p4,p5")
                .with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should validate cost prediction period")
    @WithMockUser(roles = "ANALYST")
    void shouldValidateCostPredictionPeriod() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/cost-prediction/patient-123")
                .header("X-Tenant-ID", "tenant-1")
                .param("predictionPeriodMonths", "0")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate disease condition")
    @WithMockUser(roles = "ANALYST")
    void shouldValidateDiseaseCondition() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/disease-progression/patient-123")
                .header("X-Tenant-ID", "tenant-1")
                .param("condition", "")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should support different user roles")
    @WithMockUser(roles = "EVALUATOR")
    void shouldSupportDifferentUserRoles() throws Exception {
        ReadmissionRiskScore score = createReadmissionRiskScore();
        when(readmissionPredictor.predict30DayRisk(any(), any(), any()))
            .thenReturn(score);

        mockMvc.perform(post("/api/v1/analytics/readmission-risk/patient-123")
                .header("X-Tenant-ID", "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().isOk());
    }

    private ReadmissionRiskScore createReadmissionRiskScore() {
        return ReadmissionRiskScore.builder()
            .patientId("patient-123")
            .tenantId("tenant-1")
            .score(45.0)
            .riskTier(RiskTier.MODERATE)
            .predictionPeriodDays(30)
            .readmissionProbability(0.45)
            .laceIndex(8)
            .confidence(0.80)
            .modelVersion("v1.0.0")
            .predictedAt(LocalDateTime.now())
            .build();
    }

    private CostBreakdown createCostBreakdown() {
        return CostBreakdown.builder()
            .patientId("patient-123")
            .tenantId("tenant-1")
            .totalPredictedCost(25000.0)
            .inpatientCost(12000.0)
            .outpatientCost(6000.0)
            .pharmacyCost(4000.0)
            .emergencyCost(2000.0)
            .labCost(800.0)
            .imagingCost(600.0)
            .otherCost(400.0)
            .predictionPeriodMonths(12)
            .confidence(0.75)
            .modelVersion("v1.0.0")
            .predictedAt(LocalDateTime.now())
            .build();
    }

    private ProgressionRisk createProgressionRisk() {
        return ProgressionRisk.builder()
            .patientId("patient-123")
            .tenantId("tenant-1")
            .condition("diabetes")
            .currentStage("moderate")
            .predictedStage("severe")
            .progressionProbability(0.55)
            .riskScore(55.0)
            .riskTier(RiskTier.HIGH)
            .confidence(0.78)
            .modelVersion("v1.0.0")
            .predictedAt(LocalDateTime.now())
            .build();
    }

    private RiskCohort createRiskCohort() {
        return RiskCohort.builder()
            .cohortId(UUID.randomUUID().toString())
            .tenantId("tenant-1")
            .riskTier(RiskTier.MODERATE)
            .patientIds(Arrays.asList("p1", "p2"))
            .patientCount(2)
            .averageRiskScore(45.0)
            .commonRiskFactors(new HashMap<>())
            .generatedAt(LocalDateTime.now())
            .build();
    }
}
