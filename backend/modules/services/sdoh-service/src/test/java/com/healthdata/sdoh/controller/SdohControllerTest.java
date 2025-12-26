package com.healthdata.sdoh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import com.healthdata.sdoh.config.TestCacheConfiguration;
import com.healthdata.sdoh.config.TestSecurityConfiguration;
import com.healthdata.sdoh.model.*;
import com.healthdata.sdoh.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD Tests for SdohController
 *
 * Testing REST API endpoints for SDOH service
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestSecurityConfiguration.class, TestCacheConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@DisplayName("SDOH Controller Tests")
class SdohControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GravityScreeningService screeningService;

    @MockBean
    private ZCodeMapper zCodeMapper;

    @MockBean
    private CommunityResourceService resourceService;

    @MockBean
    private HealthEquityAnalyzer equityAnalyzer;

    @MockBean
    private SdohRiskCalculator riskCalculator;

    private String tenantId;
    private String patientId;
    private SdohAssessment mockAssessment;

    @BeforeEach
    void setUp() {
        tenantId = "tenant-001";
        patientId = "patient-001";

        mockAssessment = SdohAssessment.builder()
                .assessmentId("assessment-001")
                .patientId(patientId)
                .tenantId(tenantId)
                .assessmentDate(LocalDateTime.now())
                .screeningTool("AHC-HRSN")
                .status(SdohAssessment.AssessmentStatus.COMPLETED)
                .build();
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("POST /api/v1/sdoh/screening/{patientId} - Submit screening")
    void testSubmitScreening() throws Exception {
        // Given
        List<SdohScreeningResponse> responses = Arrays.asList(
                SdohScreeningResponse.builder()
                        .questionId("q1")
                        .answer("Yes")
                        .build()
        );

        when(screeningService.submitScreening(anyString(), anyString(), anyString(), anyList()))
                .thenReturn(mockAssessment);

        // When & Then
        mockMvc.perform(post("/api/v1/sdoh/screening/{patientId}", patientId)
                        .header("X-Tenant-ID", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "screeningTool", "AHC-HRSN",
                                "responses", responses
                        )))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentId").value("assessment-001"))
                .andExpect(jsonPath("$.patientId").value(patientId));
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("GET /api/v1/sdoh/assessment/{patientId} - Get patient assessment")
    void testGetPatientAssessment() throws Exception {
        // Given
        when(screeningService.getMostRecentAssessment(tenantId, patientId))
                .thenReturn(Optional.of(mockAssessment));

        // When & Then
        mockMvc.perform(get("/api/v1/sdoh/assessment/{patientId}", patientId)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentId").value("assessment-001"))
                .andExpect(jsonPath("$.screeningTool").value("AHC-HRSN"));
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("GET /api/v1/sdoh/assessment/{patientId} - Not found")
    void testGetPatientAssessmentNotFound() throws Exception {
        // Given
        when(screeningService.getMostRecentAssessment(tenantId, patientId))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/sdoh/assessment/{patientId}", patientId)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("GET /api/v1/sdoh/z-codes/{patientId} - Get patient Z-codes")
    void testGetPatientZCodes() throws Exception {
        // Given
        List<SdohDiagnosis> diagnoses = Arrays.asList(
                SdohDiagnosis.builder()
                        .diagnosisId("d1")
                        .patientId(patientId)
                        .zCode("Z59.4")
                        .zCodeDescription("Food insecurity")
                        .category(SdohCategory.FOOD_INSECURITY)
                        .build()
        );

        // Use anyString() matchers to ensure mock matches regardless of exact parameter values
        when(zCodeMapper.getActiveDiagnoses(anyString(), anyString()))
                .thenReturn(diagnoses);

        // When & Then - print response for debugging
        String response = mockMvc.perform(get("/api/v1/sdoh/z-codes/{patientId}", patientId)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // If response is empty array, it means mock isn't being called or returned correctly
        assertFalse(response.equals("[]"), "Response should not be empty - got: " + response);
        assertTrue(response.contains("Z59.4"), "Response should contain zCode Z59.4 - got: " + response);
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("GET /api/v1/sdoh/resources - Search community resources")
    void testSearchCommunityResources() throws Exception {
        // Given
        List<CommunityResource> resources = Arrays.asList(
                CommunityResource.builder()
                        .resourceId("r1")
                        .organizationName("Local Food Bank")
                        .category(ResourceCategory.FOOD)
                        .city("Boston")
                        .state("MA")
                        .build()
        );

        when(resourceService.searchByLocation("Boston", "MA"))
                .thenReturn(resources);

        // When & Then
        mockMvc.perform(get("/api/v1/sdoh/resources")
                        .param("city", "Boston")
                        .param("state", "MA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].organizationName").value("Local Food Bank"))
                .andExpect(jsonPath("$[0].category").value("FOOD"));
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("GET /api/v1/sdoh/resources - Search by category")
    void testSearchResourcesByCategory() throws Exception {
        // Given
        List<CommunityResource> resources = Arrays.asList(
                CommunityResource.builder()
                        .resourceId("r1")
                        .category(ResourceCategory.HOUSING)
                        .build()
        );

        when(resourceService.searchByCategory(ResourceCategory.HOUSING))
                .thenReturn(resources);

        // When & Then
        mockMvc.perform(get("/api/v1/sdoh/resources")
                        .param("category", "HOUSING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("HOUSING"));
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("POST /api/v1/sdoh/referral - Create resource referral")
    void testCreateResourceReferral() throws Exception {
        // Given
        ResourceReferral mockReferral = ResourceReferral.builder()
                .referralId("ref-001")
                .patientId(patientId)
                .resourceId("r1")
                .category(SdohCategory.FOOD_INSECURITY)
                .status(ResourceReferral.ReferralStatus.PENDING)
                .build();

        when(resourceService.createReferral(anyString(), anyString(), anyString(),
                any(SdohCategory.class), anyString(), anyString()))
                .thenReturn(mockReferral);

        // When & Then
        mockMvc.perform(post("/api/v1/sdoh/referral")
                        .header("X-Tenant-ID", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", patientId,
                                "resourceId", "r1",
                                "category", "FOOD_INSECURITY",
                                "reason", "Patient needs food assistance",
                                "referredBy", "Provider-001"
                        )))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referralId").value("ref-001"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("GET /api/v1/sdoh/equity/report - Get health equity report")
    void testGetHealthEquityReport() throws Exception {
        // Given
        EquityReport mockReport = EquityReport.builder()
                .reportId("report-001")
                .tenantId(tenantId)
                .reportDate(LocalDateTime.now())
                .disparityMetrics(new ArrayList<>())
                .keyFindings(Arrays.asList("Finding 1", "Finding 2"))
                .build();

        when(equityAnalyzer.generateEquityReport(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockReport);

        // When & Then
        mockMvc.perform(get("/api/v1/sdoh/equity/report")
                        .header("X-Tenant-ID", tenantId)
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value("report-001"))
                .andExpect(jsonPath("$.keyFindings").isArray());
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("GET /api/v1/sdoh/risk/{patientId} - Get patient risk score")
    void testGetPatientRiskScore() throws Exception {
        // Given
        SdohRiskScore mockScore = SdohRiskScore.builder()
                .scoreId("score-001")
                .patientId(patientId)
                .totalScore(65.0)
                .riskLevel(SdohRiskScore.RiskLevel.HIGH)
                .build();

        when(riskCalculator.getRiskScoreHistory(tenantId, patientId))
                .thenReturn(Arrays.asList(mockScore));

        // When & Then
        mockMvc.perform(get("/api/v1/sdoh/risk/{patientId}", patientId)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalScore").value(65.0))
                .andExpect(jsonPath("$[0].riskLevel").value("HIGH"));
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("GET /api/v1/sdoh/referrals/{patientId} - Get patient referrals")
    void testGetPatientReferrals() throws Exception {
        // Given
        List<ResourceReferral> referrals = Arrays.asList(
                ResourceReferral.builder()
                        .referralId("ref-001")
                        .patientId(patientId)
                        .status(ResourceReferral.ReferralStatus.PENDING)
                        .build()
        );

        when(resourceService.getPatientReferrals(tenantId, patientId))
                .thenReturn(referrals);

        // When & Then
        mockMvc.perform(get("/api/v1/sdoh/referrals/{patientId}", patientId)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].referralId").value("ref-001"));
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("PUT /api/v1/sdoh/referral/{referralId}/status - Update referral status")
    void testUpdateReferralStatus() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/v1/sdoh/referral/{referralId}/status", "ref-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "COMPLETED",
                                "outcome", "Patient successfully connected to resource"
                        )))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("GET /api/v1/sdoh/screening/questions - Get screening questions")
    void testGetScreeningQuestions() throws Exception {
        // Given
        List<SdohScreeningQuestion> questions = Arrays.asList(
                SdohScreeningQuestion.builder()
                        .questionId("q1")
                        .questionText("Do you have food security concerns?")
                        .category(SdohCategory.FOOD_INSECURITY)
                        .loincCode("88122-7")
                        .build()
        );

        when(screeningService.createAhcHrsnQuestionnaire())
                .thenReturn(questions);

        // When & Then
        mockMvc.perform(get("/api/v1/sdoh/screening/questions")
                        .param("tool", "AHC-HRSN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questionId").value("q1"))
                .andExpect(jsonPath("$[0].category").value("FOOD_INSECURITY"));
    }

    @Test
    @DisplayName("Unauthorized access should be rejected by method security")
    void testUnauthorizedAccess() {
        // With @AutoConfigureMockMvc(addFilters = false), HTTP security filters are disabled
        // but method-level security (@PreAuthorize) is still enforced.
        // Without authentication, the method security check throws AuthenticationCredentialsNotFoundException
        // which is wrapped in a ServletException
        try {
            mockMvc.perform(get("/api/v1/sdoh/assessment/{patientId}", patientId)
                    .header("X-Tenant-ID", tenantId));
            // If we get here without exception, fail the test
            fail("Expected method security to reject unauthenticated request");
        } catch (Exception e) {
            // Expected - method security rejected the request
            assertTrue(e.getCause() instanceof org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
                    || e.getMessage().contains("Authentication"),
                    "Expected authentication exception, got: " + e.getMessage());
        }
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("Missing tenant header should return 400")
    void testMissingTenantHeader() throws Exception {
        // When X-Tenant-ID header is missing and it's required, Spring returns 400
        mockMvc.perform(get("/api/v1/sdoh/assessment/{patientId}", patientId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("GET /api/v1/sdoh/_health - Health check")
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/v1/sdoh/_health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("sdoh-service"));
    }
}
