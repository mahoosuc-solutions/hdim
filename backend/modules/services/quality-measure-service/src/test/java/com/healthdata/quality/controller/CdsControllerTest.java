package com.healthdata.quality.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.dto.*;
import com.healthdata.quality.service.CdsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.Instant;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for CdsController
 *
 * Tests all CDS endpoints including:
 * - Rule retrieval (GET /cds/rules, /cds/rules/category/{category}, /cds/rules/{ruleCode})
 * - Recommendation retrieval (GET /cds/recommendations/{patientId}, /cds/recommendations/{patientId}/count, /cds/recommendations/{patientId}/overdue)
 * - Rule evaluation (POST /cds/evaluate)
 * - Recommendation acknowledgment (POST /cds/acknowledge)
 *
 * Uses standalone MockMvc setup for focused controller testing with mocked service layer.
 * Includes security testing with @WithMockUser annotations.
 */
@SpringBootTest(classes = CdsControllerTest.TestConfig.class)
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@DisplayName("CDS Controller Tests")
class CdsControllerTest {

    /**
     * Test configuration that only loads the controller and security without JPA
     */
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(prePostEnabled = true)
    @Import({CdsController.class, CdsControllerTest.TestExceptionHandler.class})
    static class TestConfig {

        @org.springframework.context.annotation.Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                    .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(401);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\": \"Unauthorized\"}");
                    })
                );
            return http.build();
        }

        @org.springframework.context.annotation.Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            return mapper;
        }
    }

    /**
     * Exception handler for test context
     */
    @org.springframework.web.bind.annotation.ControllerAdvice
    static class TestExceptionHandler {

        @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
        @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
        @org.springframework.web.bind.annotation.ResponseBody
        public Map<String, String> handleIllegalArgumentException(IllegalArgumentException e) {
            return Map.of("error", e.getMessage());
        }

        @org.springframework.web.bind.annotation.ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
        @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
        @org.springframework.web.bind.annotation.ResponseBody
        public Map<String, String> handleEntityNotFoundException(jakarta.persistence.EntityNotFoundException e) {
            return Map.of("error", e.getMessage());
        }

        @org.springframework.web.bind.annotation.ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
        @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN)
        @org.springframework.web.bind.annotation.ResponseBody
        public Map<String, String> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException e) {
            return Map.of("error", "Access denied");
        }

        @org.springframework.web.bind.annotation.ExceptionHandler(SecurityException.class)
        @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN)
        @org.springframework.web.bind.annotation.ResponseBody
        public Map<String, String> handleSecurityException(SecurityException e) {
            return Map.of("error", e.getMessage());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CdsService cdsService;

    private static final String TENANT_ID = "test-tenant-001";
    private static final String PATIENT_ID = "patient-12345";
    private static final String RULE_CODE = "DIABETES_HBA1C_CHECK";
    private static final String CATEGORY = "PREVENTIVE_CARE";

    private CdsRuleDTO testRule;
    private CdsRecommendationDTO testRecommendation;
    private CdsEvaluateRequest evaluateRequest;
    private CdsEvaluateResponse evaluateResponse;
    private CdsAcknowledgeRequest acknowledgeRequest;

    @BeforeEach
    void setUp() {
        initializeTestData();
    }

    private void initializeTestData() {
        // Test Rule
        testRule = CdsRuleDTO.builder()
                .id(UUID.randomUUID())
                .ruleName("HbA1c Testing Overdue")
                .ruleCode(RULE_CODE)
                .description("Recommends HbA1c testing for diabetic patients")
                .category(CATEGORY)
                .priority(1)
                .cqlLibraryName("DiabetesCare")
                .cqlExpression("HbA1cOverdue")
                .recommendationTemplate("Schedule HbA1c test for diabetic patient")
                .evidenceSource("ADA Guidelines 2023")
                .clinicalGuideline("Diabetes Management Protocol")
                .actionItems(Arrays.asList("Order HbA1c test", "Schedule follow-up"))
                .defaultUrgency("URGENT")
                .active(true)
                .requiresAcknowledgment(true)
                .version("1.0")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Test Recommendation
        testRecommendation = CdsRecommendationDTO.builder()
                .id(UUID.randomUUID())
                .patientId(PATIENT_ID)
                .ruleId(testRule.getId())
                .title("HbA1c Test Overdue")
                .description("Patient with diabetes requires HbA1c testing")
                .category(CATEGORY)
                .urgency("URGENT")
                .status("ACTIVE")
                .priority(1)
                .actionItems(Arrays.asList("Order HbA1c test", "Schedule follow-up"))
                .evidenceSource("ADA Guidelines 2023")
                .clinicalGuideline("Diabetes Management Protocol")
                .dueDate(Instant.now().plusSeconds(48 * 60 * 60)) // 48 hours
                .evaluatedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Evaluate Request
        evaluateRequest = CdsEvaluateRequest.builder()
                .patientId(PATIENT_ID)
                .forceReEvaluation(false)
                .includeRuleDetails(false)
                .build();

        // Evaluate Response
        evaluateResponse = CdsEvaluateResponse.builder()
                .patientId(PATIENT_ID)
                .evaluatedAt(Instant.now())
                .rulesEvaluated(5)
                .recommendationsGenerated(2)
                .existingRecommendationsSkipped(1)
                .newRecommendations(Arrays.asList(testRecommendation))
                .existingRecommendations(Arrays.asList())
                .recommendationsByCategory(Map.of(CATEGORY, 2))
                .recommendationsByUrgency(Map.of("URGENT", 2))
                .evaluationDetails(Arrays.asList(
                        CdsEvaluateResponse.EvaluationDetail.builder()
                                .ruleCode(RULE_CODE)
                                .ruleName("HbA1c Testing Overdue")
                                .triggered(true)
                                .result("TRIGGERED - Recommendation created")
                                .evaluationTimeMs(150L)
                                .build()
                ))
                .build();

        // Acknowledge Request
        acknowledgeRequest = CdsAcknowledgeRequest.builder()
                .recommendationId(testRecommendation.getId())
                .action("ACKNOWLEDGED")
                .userId("provider-001")
                .userName("Dr. Smith")
                .userRole("PROVIDER")
                .notes("Will schedule test for next visit")
                .build();
    }

    // ==================== GET /cds/rules Tests ====================

    @Nested
    @DisplayName("GET /cds/rules - Get All Rules Tests")
    class GetRulesTests {

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should successfully retrieve all active rules")
        void shouldGetActiveRules() throws Exception {
            // Given
            List<CdsRuleDTO> rules = Arrays.asList(testRule);
            when(cdsService.getActiveRules(TENANT_ID)).thenReturn(rules);

            // When & Then
            mockMvc.perform(get("/cds/rules")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].ruleCode", is(RULE_CODE)))
                    .andExpect(jsonPath("$[0].ruleName", is("HbA1c Testing Overdue")))
                    .andExpect(jsonPath("$[0].category", is(CATEGORY)))
                    .andExpect(jsonPath("$[0].active", is(true)));

            verify(cdsService, times(1)).getActiveRules(TENANT_ID);
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should retrieve all rules including inactive when activeOnly=false")
        void shouldGetAllRulesIncludingInactive() throws Exception {
            // Given
            CdsRuleDTO inactiveRule = CdsRuleDTO.builder()
                    .id(UUID.randomUUID())
                    .ruleCode("INACTIVE_RULE")
                    .ruleName("Inactive Rule")
                    .active(false)
                    .build();
            List<CdsRuleDTO> rules = Arrays.asList(testRule, inactiveRule);
            when(cdsService.getAllRules(TENANT_ID)).thenReturn(rules);

            // When & Then
            mockMvc.perform(get("/cds/rules")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("activeOnly", "false")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].active", is(true)))
                    .andExpect(jsonPath("$[1].active", is(false)));

            verify(cdsService, times(1)).getAllRules(TENANT_ID);
            verify(cdsService, never()).getActiveRules(anyString());
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should return empty list when no rules exist")
        void shouldReturnEmptyListWhenNoRules() throws Exception {
            // Given
            when(cdsService.getActiveRules(TENANT_ID)).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/cds/rules")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(cdsService, times(1)).getActiveRules(TENANT_ID);
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should return 400 when X-Tenant-ID header is missing")
        void shouldReturn400WhenTenantIdMissing() throws Exception {
            // When & Then
            mockMvc.perform(get("/cds/rules")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(cdsService, never()).getActiveRules(anyString());
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // When & Then
            mockMvc.perform(get("/cds/rules")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());

            verify(cdsService, never()).getActiveRules(anyString());
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("Should return 403 when user lacks required role")
        void shouldReturn403WhenInsufficientRole() throws Exception {
            // When & Then
            mockMvc.perform(get("/cds/rules")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());

            verify(cdsService, never()).getActiveRules(anyString());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should allow ADMIN role to access rules")
        void shouldAllowAdminAccess() throws Exception {
            // Given
            when(cdsService.getActiveRules(TENANT_ID)).thenReturn(Arrays.asList(testRule));

            // When & Then
            mockMvc.perform(get("/cds/rules")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(cdsService, times(1)).getActiveRules(TENANT_ID);
        }
    }

    // ==================== GET /cds/rules/category/{category} Tests ====================

    @Nested
    @DisplayName("GET /cds/rules/category/{category} - Get Rules By Category Tests")
    class GetRulesByCategoryTests {

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should successfully retrieve rules by category")
        void shouldGetRulesByCategory() throws Exception {
            // Given
            List<CdsRuleDTO> rules = Arrays.asList(testRule);
            when(cdsService.getRulesByCategory(TENANT_ID, CATEGORY)).thenReturn(rules);

            // When & Then
            mockMvc.perform(get("/cds/rules/category/{category}", CATEGORY)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].category", is(CATEGORY)));

            verify(cdsService, times(1)).getRulesByCategory(TENANT_ID, CATEGORY);
        }

        @Test
        @WithMockUser(roles = "NURSE")
        @DisplayName("Should return empty list for category with no rules")
        void shouldReturnEmptyListForEmptyCategory() throws Exception {
            // Given
            when(cdsService.getRulesByCategory(TENANT_ID, "MENTAL_HEALTH")).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/cds/rules/category/{category}", "MENTAL_HEALTH")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(cdsService, times(1)).getRulesByCategory(TENANT_ID, "MENTAL_HEALTH");
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should handle service exceptions gracefully")
        void shouldHandleServiceExceptions() throws Exception {
            // Given
            when(cdsService.getRulesByCategory(TENANT_ID, "INVALID_CATEGORY"))
                    .thenThrow(new IllegalArgumentException("Invalid category"));

            // When & Then
            mockMvc.perform(get("/cds/rules/category/{category}", "INVALID_CATEGORY")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is4xxClientError());

            verify(cdsService, times(1)).getRulesByCategory(TENANT_ID, "INVALID_CATEGORY");
        }
    }

    // ==================== GET /cds/rules/{ruleCode} Tests ====================

    @Nested
    @DisplayName("GET /cds/rules/{ruleCode} - Get Rule By Code Tests")
    class GetRuleByCodeTests {

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should successfully retrieve rule by code")
        void shouldGetRuleByCode() throws Exception {
            // Given
            when(cdsService.getRuleByCode(TENANT_ID, RULE_CODE)).thenReturn(Optional.of(testRule));

            // When & Then
            mockMvc.perform(get("/cds/rules/{ruleCode}", RULE_CODE)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.ruleCode", is(RULE_CODE)))
                    .andExpect(jsonPath("$.ruleName", is("HbA1c Testing Overdue")))
                    .andExpect(jsonPath("$.priority", is(1)))
                    .andExpect(jsonPath("$.defaultUrgency", is("URGENT")));

            verify(cdsService, times(1)).getRuleByCode(TENANT_ID, RULE_CODE);
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should return 404 when rule not found")
        void shouldReturn404WhenRuleNotFound() throws Exception {
            // Given
            when(cdsService.getRuleByCode(TENANT_ID, "NONEXISTENT_RULE")).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/cds/rules/{ruleCode}", "NONEXISTENT_RULE")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(cdsService, times(1)).getRuleByCode(TENANT_ID, "NONEXISTENT_RULE");
        }

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Should allow ANALYST role to access rule details")
        void shouldAllowAnalystAccess() throws Exception {
            // Given
            when(cdsService.getRuleByCode(TENANT_ID, RULE_CODE)).thenReturn(Optional.of(testRule));

            // When & Then
            mockMvc.perform(get("/cds/rules/{ruleCode}", RULE_CODE)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(cdsService, times(1)).getRuleByCode(TENANT_ID, RULE_CODE);
        }
    }

    // ==================== GET /cds/recommendations/{patientId} Tests ====================

    @Nested
    @DisplayName("GET /cds/recommendations/{patientId} - Get Recommendations Tests")
    class GetRecommendationsTests {

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should successfully retrieve patient recommendations")
        void shouldGetRecommendations() throws Exception {
            // Given
            List<CdsRecommendationDTO> recommendations = Arrays.asList(testRecommendation);
            when(cdsService.getActiveRecommendations(TENANT_ID, PATIENT_ID)).thenReturn(recommendations);

            // When & Then
            mockMvc.perform(get("/cds/recommendations/{patientId}", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].patientId", is(PATIENT_ID)))
                    .andExpect(jsonPath("$[0].title", is("HbA1c Test Overdue")))
                    .andExpect(jsonPath("$[0].urgency", is("URGENT")))
                    .andExpect(jsonPath("$[0].status", is("ACTIVE")));

            verify(cdsService, times(1)).getActiveRecommendations(TENANT_ID, PATIENT_ID);
        }

        @Test
        @WithMockUser(roles = "CARE_COORDINATOR")
        @DisplayName("Should allow CARE_COORDINATOR role to access recommendations")
        void shouldAllowCareCoordinatorAccess() throws Exception {
            // Given
            when(cdsService.getActiveRecommendations(TENANT_ID, PATIENT_ID))
                    .thenReturn(Arrays.asList(testRecommendation));

            // When & Then
            mockMvc.perform(get("/cds/recommendations/{patientId}", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(cdsService, times(1)).getActiveRecommendations(TENANT_ID, PATIENT_ID);
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should return empty list when patient has no recommendations")
        void shouldReturnEmptyListWhenNoRecommendations() throws Exception {
            // Given
            when(cdsService.getActiveRecommendations(TENANT_ID, PATIENT_ID)).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/cds/recommendations/{patientId}", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(cdsService, times(1)).getActiveRecommendations(TENANT_ID, PATIENT_ID);
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should handle patient IDs with special characters")
        void shouldHandlePatientIdWithSpecialCharacters() throws Exception {
            // Given
            String specialPatientId = "patient-123.456@test";
            when(cdsService.getActiveRecommendations(TENANT_ID, specialPatientId))
                    .thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/cds/recommendations/{patientId}", specialPatientId)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(cdsService, times(1)).getActiveRecommendations(TENANT_ID, specialPatientId);
        }
    }

    // ==================== GET /cds/recommendations/{patientId}/count Tests ====================

    @Nested
    @DisplayName("GET /cds/recommendations/{patientId}/count - Get Recommendation Counts Tests")
    class GetRecommendationCountTests {

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should successfully retrieve recommendation counts by urgency")
        void shouldGetRecommendationCounts() throws Exception {
            // Given
            Map<String, Long> countsByUrgency = Map.of(
                    "EMERGENT", 1L,
                    "URGENT", 3L,
                    "SOON", 2L,
                    "ROUTINE", 5L
            );
            when(cdsService.getRecommendationCountsByUrgency(TENANT_ID, PATIENT_ID)).thenReturn(countsByUrgency);
            when(cdsService.getActiveRecommendationCount(TENANT_ID, PATIENT_ID)).thenReturn(11L);

            // When & Then
            mockMvc.perform(get("/cds/recommendations/{patientId}/count", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.patientId", is(PATIENT_ID)))
                    .andExpect(jsonPath("$.totalActive", is(11)))
                    .andExpect(jsonPath("$.emergent", is(1)))
                    .andExpect(jsonPath("$.urgent", is(3)))
                    .andExpect(jsonPath("$.soon", is(2)))
                    .andExpect(jsonPath("$.routine", is(5)));

            verify(cdsService, times(1)).getRecommendationCountsByUrgency(TENANT_ID, PATIENT_ID);
            verify(cdsService, times(1)).getActiveRecommendationCount(TENANT_ID, PATIENT_ID);
        }

        @Test
        @WithMockUser(roles = "NURSE")
        @DisplayName("Should handle patient with zero recommendations")
        void shouldHandleZeroRecommendations() throws Exception {
            // Given
            Map<String, Long> countsByUrgency = Map.of();
            when(cdsService.getRecommendationCountsByUrgency(TENANT_ID, PATIENT_ID)).thenReturn(countsByUrgency);
            when(cdsService.getActiveRecommendationCount(TENANT_ID, PATIENT_ID)).thenReturn(0L);

            // When & Then
            mockMvc.perform(get("/cds/recommendations/{patientId}/count", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalActive", is(0)))
                    .andExpect(jsonPath("$.emergent", is(0)))
                    .andExpect(jsonPath("$.urgent", is(0)))
                    .andExpect(jsonPath("$.soon", is(0)))
                    .andExpect(jsonPath("$.routine", is(0)));

            verify(cdsService, times(1)).getRecommendationCountsByUrgency(TENANT_ID, PATIENT_ID);
            verify(cdsService, times(1)).getActiveRecommendationCount(TENANT_ID, PATIENT_ID);
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should handle missing urgency categories")
        void shouldHandleMissingUrgencyCategories() throws Exception {
            // Given - Only URGENT recommendations exist
            Map<String, Long> countsByUrgency = Map.of("URGENT", 2L);
            when(cdsService.getRecommendationCountsByUrgency(TENANT_ID, PATIENT_ID)).thenReturn(countsByUrgency);
            when(cdsService.getActiveRecommendationCount(TENANT_ID, PATIENT_ID)).thenReturn(2L);

            // When & Then
            mockMvc.perform(get("/cds/recommendations/{patientId}/count", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalActive", is(2)))
                    .andExpect(jsonPath("$.urgent", is(2)))
                    .andExpect(jsonPath("$.emergent", is(0)))
                    .andExpect(jsonPath("$.soon", is(0)))
                    .andExpect(jsonPath("$.routine", is(0)));
        }
    }

    // ==================== GET /cds/recommendations/{patientId}/overdue Tests ====================

    @Nested
    @DisplayName("GET /cds/recommendations/{patientId}/overdue - Get Overdue Recommendations Tests")
    class GetOverdueRecommendationsTests {

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should successfully retrieve overdue recommendations")
        void shouldGetOverdueRecommendations() throws Exception {
            // Given
            CdsRecommendationDTO overdueRecommendation = CdsRecommendationDTO.builder()
                    .id(UUID.randomUUID())
                    .patientId(PATIENT_ID)
                    .title("Overdue Lab Test")
                    .status("ACTIVE")
                    .urgency("URGENT")
                    .dueDate(Instant.now().minusSeconds(24 * 60 * 60)) // 1 day overdue
                    .daysOverdue(1L)
                    .build();

            when(cdsService.getOverdueRecommendations(TENANT_ID, PATIENT_ID))
                    .thenReturn(Arrays.asList(overdueRecommendation));

            // When & Then
            mockMvc.perform(get("/cds/recommendations/{patientId}/overdue", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].patientId", is(PATIENT_ID)))
                    .andExpect(jsonPath("$[0].daysOverdue", is(1)));

            verify(cdsService, times(1)).getOverdueRecommendations(TENANT_ID, PATIENT_ID);
        }

        @Test
        @WithMockUser(roles = "CARE_COORDINATOR")
        @DisplayName("Should return empty list when no overdue recommendations")
        void shouldReturnEmptyListWhenNoOverdueRecommendations() throws Exception {
            // Given
            when(cdsService.getOverdueRecommendations(TENANT_ID, PATIENT_ID)).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/cds/recommendations/{patientId}/overdue", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(cdsService, times(1)).getOverdueRecommendations(TENANT_ID, PATIENT_ID);
        }
    }

    // ==================== POST /cds/evaluate Tests ====================

    @Nested
    @DisplayName("POST /cds/evaluate - Evaluate CDS Rules Tests")
    class EvaluateRulesTests {

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should successfully evaluate CDS rules for patient")
        void shouldEvaluateRules() throws Exception {
            // Given
            when(cdsService.evaluateRules(eq(TENANT_ID), any(CdsEvaluateRequest.class)))
                    .thenReturn(evaluateResponse);

            // When & Then
            mockMvc.perform(post("/cds/evaluate")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(evaluateRequest))
                            .with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.patientId", is(PATIENT_ID)))
                    .andExpect(jsonPath("$.rulesEvaluated", is(5)))
                    .andExpect(jsonPath("$.recommendationsGenerated", is(2)))
                    .andExpect(jsonPath("$.existingRecommendationsSkipped", is(1)))
                    .andExpect(jsonPath("$.newRecommendations", hasSize(1)))
                    .andExpect(jsonPath("$.evaluationDetails", hasSize(1)))
                    .andExpect(jsonPath("$.evaluationDetails[0].ruleCode", is(RULE_CODE)))
                    .andExpect(jsonPath("$.evaluationDetails[0].triggered", is(true)));

            verify(cdsService, times(1)).evaluateRules(eq(TENANT_ID), any(CdsEvaluateRequest.class));
        }

        @Test
        @WithMockUser(roles = "NURSE")
        @DisplayName("Should evaluate rules with specific rule IDs")
        void shouldEvaluateSpecificRules() throws Exception {
            // Given
            CdsEvaluateRequest requestWithRuleIds = CdsEvaluateRequest.builder()
                    .patientId(PATIENT_ID)
                    .ruleIds(Arrays.asList(testRule.getId()))
                    .build();

            when(cdsService.evaluateRules(eq(TENANT_ID), any(CdsEvaluateRequest.class)))
                    .thenReturn(evaluateResponse);

            // When & Then
            mockMvc.perform(post("/cds/evaluate")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithRuleIds))
                            .with(csrf()))
                    .andExpect(status().isCreated());

            verify(cdsService, times(1)).evaluateRules(eq(TENANT_ID), any(CdsEvaluateRequest.class));
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should evaluate rules filtered by categories")
        void shouldEvaluateRulesByCategories() throws Exception {
            // Given
            CdsEvaluateRequest requestWithCategories = CdsEvaluateRequest.builder()
                    .patientId(PATIENT_ID)
                    .categories(Arrays.asList("PREVENTIVE_CARE", "CHRONIC_DISEASE"))
                    .build();

            when(cdsService.evaluateRules(eq(TENANT_ID), any(CdsEvaluateRequest.class)))
                    .thenReturn(evaluateResponse);

            // When & Then
            mockMvc.perform(post("/cds/evaluate")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithCategories))
                            .with(csrf()))
                    .andExpect(status().isCreated());

            verify(cdsService, times(1)).evaluateRules(eq(TENANT_ID), any(CdsEvaluateRequest.class));
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should handle force re-evaluation flag")
        void shouldHandleForceReEvaluation() throws Exception {
            // Given
            CdsEvaluateRequest forceRequest = CdsEvaluateRequest.builder()
                    .patientId(PATIENT_ID)
                    .forceReEvaluation(true)
                    .build();

            when(cdsService.evaluateRules(eq(TENANT_ID), any(CdsEvaluateRequest.class)))
                    .thenReturn(evaluateResponse);

            // When & Then
            mockMvc.perform(post("/cds/evaluate")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(forceRequest))
                            .with(csrf()))
                    .andExpect(status().isCreated());

            verify(cdsService, times(1)).evaluateRules(eq(TENANT_ID), any(CdsEvaluateRequest.class));
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should return 400 when patientId is missing")
        void shouldReturn400WhenPatientIdMissing() throws Exception {
            // Given
            CdsEvaluateRequest invalidRequest = CdsEvaluateRequest.builder().build();

            // When & Then
            mockMvc.perform(post("/cds/evaluate")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());

            verify(cdsService, never()).evaluateRules(anyString(), any(CdsEvaluateRequest.class));
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should return 400 when request body is invalid JSON")
        void shouldReturn400WhenInvalidJson() throws Exception {
            // When & Then
            mockMvc.perform(post("/cds/evaluate")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}")
                            .with(csrf()))
                    .andExpect(status().isBadRequest());

            verify(cdsService, never()).evaluateRules(anyString(), any(CdsEvaluateRequest.class));
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should return 400 when X-Tenant-ID is missing")
        void shouldReturn400WhenTenantIdMissingForEvaluate() throws Exception {
            // When & Then
            mockMvc.perform(post("/cds/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(evaluateRequest))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());

            verify(cdsService, never()).evaluateRules(anyString(), any(CdsEvaluateRequest.class));
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void shouldReturn401WhenNotAuthenticatedForEvaluate() throws Exception {
            // When & Then
            mockMvc.perform(post("/cds/evaluate")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(evaluateRequest))
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());

            verify(cdsService, never()).evaluateRules(anyString(), any(CdsEvaluateRequest.class));
        }

        @Test
        @WithMockUser(roles = "CARE_COORDINATOR")
        @DisplayName("Should return 403 when user lacks PROVIDER/NURSE/ADMIN role")
        void shouldReturn403WhenInsufficientRoleForEvaluate() throws Exception {
            // When & Then
            mockMvc.perform(post("/cds/evaluate")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(evaluateRequest))
                            .with(csrf()))
                    .andExpect(status().isForbidden());

            verify(cdsService, never()).evaluateRules(anyString(), any(CdsEvaluateRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should allow ADMIN role to evaluate rules")
        void shouldAllowAdminToEvaluate() throws Exception {
            // Given
            when(cdsService.evaluateRules(eq(TENANT_ID), any(CdsEvaluateRequest.class)))
                    .thenReturn(evaluateResponse);

            // When & Then
            mockMvc.perform(post("/cds/evaluate")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(evaluateRequest))
                            .with(csrf()))
                    .andExpect(status().isCreated());

            verify(cdsService, times(1)).evaluateRules(eq(TENANT_ID), any(CdsEvaluateRequest.class));
        }
    }

    // ==================== POST /cds/acknowledge Tests ====================

    @Nested
    @DisplayName("POST /cds/acknowledge - Acknowledge Recommendation Tests")
    class AcknowledgeRecommendationTests {

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should successfully acknowledge recommendation")
        void shouldAcknowledgeRecommendation() throws Exception {
            // Given
            CdsRecommendationDTO acknowledgedRecommendation = CdsRecommendationDTO.builder()
                    .id(testRecommendation.getId())
                    .patientId(PATIENT_ID)
                    .title("HbA1c Test Overdue")
                    .status("ACKNOWLEDGED")
                    .acknowledgedAt(Instant.now())
                    .acknowledgedBy("provider-001")
                    .build();

            when(cdsService.acknowledgeRecommendation(eq(TENANT_ID), any(CdsAcknowledgeRequest.class)))
                    .thenReturn(acknowledgedRecommendation);

            // When & Then
            mockMvc.perform(post("/cds/acknowledge")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(acknowledgeRequest))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.status", is("ACKNOWLEDGED")))
                    .andExpect(jsonPath("$.acknowledgedBy", is("provider-001")))
                    .andExpect(jsonPath("$.acknowledgedAt", notNullValue()));

            verify(cdsService, times(1)).acknowledgeRecommendation(eq(TENANT_ID), any(CdsAcknowledgeRequest.class));
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should handle ACCEPTED action")
        void shouldHandleAcceptedAction() throws Exception {
            // Given
            CdsAcknowledgeRequest acceptRequest = CdsAcknowledgeRequest.builder()
                    .recommendationId(testRecommendation.getId())
                    .action("ACCEPTED")
                    .userId("provider-001")
                    .userName("Dr. Smith")
                    .userRole("PROVIDER")
                    .build();

            CdsRecommendationDTO acceptedRecommendation = CdsRecommendationDTO.builder()
                    .id(testRecommendation.getId())
                    .status("IN_PROGRESS")
                    .build();

            when(cdsService.acknowledgeRecommendation(eq(TENANT_ID), any(CdsAcknowledgeRequest.class)))
                    .thenReturn(acceptedRecommendation);

            // When & Then
            mockMvc.perform(post("/cds/acknowledge")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(acceptRequest))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

            verify(cdsService, times(1)).acknowledgeRecommendation(eq(TENANT_ID), any(CdsAcknowledgeRequest.class));
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should handle COMPLETED action")
        void shouldHandleCompletedAction() throws Exception {
            // Given
            CdsAcknowledgeRequest completeRequest = CdsAcknowledgeRequest.builder()
                    .recommendationId(testRecommendation.getId())
                    .action("COMPLETED")
                    .userId("provider-001")
                    .userName("Dr. Smith")
                    .userRole("PROVIDER")
                    .outcome("HbA1c test completed - results normal")
                    .build();

            CdsRecommendationDTO completedRecommendation = CdsRecommendationDTO.builder()
                    .id(testRecommendation.getId())
                    .status("COMPLETED")
                    .completedAt(Instant.now())
                    .completedBy("provider-001")
                    .completionOutcome("HbA1c test completed - results normal")
                    .build();

            when(cdsService.acknowledgeRecommendation(eq(TENANT_ID), any(CdsAcknowledgeRequest.class)))
                    .thenReturn(completedRecommendation);

            // When & Then
            mockMvc.perform(post("/cds/acknowledge")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(completeRequest))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("COMPLETED")))
                    .andExpect(jsonPath("$.completionOutcome", is("HbA1c test completed - results normal")));

            verify(cdsService, times(1)).acknowledgeRecommendation(eq(TENANT_ID), any(CdsAcknowledgeRequest.class));
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should handle DECLINED action with reason")
        void shouldHandleDeclinedAction() throws Exception {
            // Given
            CdsAcknowledgeRequest declineRequest = CdsAcknowledgeRequest.builder()
                    .recommendationId(testRecommendation.getId())
                    .action("DECLINED")
                    .userId("provider-001")
                    .userName("Dr. Smith")
                    .userRole("PROVIDER")
                    .reason("Patient recently had test at another facility")
                    .build();

            CdsRecommendationDTO declinedRecommendation = CdsRecommendationDTO.builder()
                    .id(testRecommendation.getId())
                    .status("DECLINED")
                    .declinedAt(Instant.now())
                    .declinedBy("provider-001")
                    .declineReason("Patient recently had test at another facility")
                    .build();

            when(cdsService.acknowledgeRecommendation(eq(TENANT_ID), any(CdsAcknowledgeRequest.class)))
                    .thenReturn(declinedRecommendation);

            // When & Then
            mockMvc.perform(post("/cds/acknowledge")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(declineRequest))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("DECLINED")))
                    .andExpect(jsonPath("$.declineReason", is("Patient recently had test at another facility")));

            verify(cdsService, times(1)).acknowledgeRecommendation(eq(TENANT_ID), any(CdsAcknowledgeRequest.class));
        }

        @Test
        @WithMockUser(roles = "NURSE")
        @DisplayName("Should allow NURSE role to acknowledge recommendations")
        void shouldAllowNurseToAcknowledge() throws Exception {
            // Given
            when(cdsService.acknowledgeRecommendation(eq(TENANT_ID), any(CdsAcknowledgeRequest.class)))
                    .thenReturn(testRecommendation);

            // When & Then
            mockMvc.perform(post("/cds/acknowledge")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(acknowledgeRequest))
                            .with(csrf()))
                    .andExpect(status().isOk());

            verify(cdsService, times(1)).acknowledgeRecommendation(eq(TENANT_ID), any(CdsAcknowledgeRequest.class));
        }

        @Test
        @WithMockUser(roles = "CARE_COORDINATOR")
        @DisplayName("Should allow CARE_COORDINATOR role to acknowledge recommendations")
        void shouldAllowCareCoordinatorToAcknowledge() throws Exception {
            // Given
            when(cdsService.acknowledgeRecommendation(eq(TENANT_ID), any(CdsAcknowledgeRequest.class)))
                    .thenReturn(testRecommendation);

            // When & Then
            mockMvc.perform(post("/cds/acknowledge")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(acknowledgeRequest))
                            .with(csrf()))
                    .andExpect(status().isOk());

            verify(cdsService, times(1)).acknowledgeRecommendation(eq(TENANT_ID), any(CdsAcknowledgeRequest.class));
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should return 400 when recommendationId is missing")
        void shouldReturn400WhenRecommendationIdMissing() throws Exception {
            // Given
            CdsAcknowledgeRequest invalidRequest = CdsAcknowledgeRequest.builder()
                    .action("ACKNOWLEDGED")
                    .build();

            // When & Then
            mockMvc.perform(post("/cds/acknowledge")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());

            verify(cdsService, never()).acknowledgeRecommendation(anyString(), any(CdsAcknowledgeRequest.class));
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should return 400 when action is missing")
        void shouldReturn400WhenActionMissing() throws Exception {
            // Given
            CdsAcknowledgeRequest invalidRequest = CdsAcknowledgeRequest.builder()
                    .recommendationId(testRecommendation.getId())
                    .build();

            // When & Then
            mockMvc.perform(post("/cds/acknowledge")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());

            verify(cdsService, never()).acknowledgeRecommendation(anyString(), any(CdsAcknowledgeRequest.class));
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should handle service exception when recommendation not found")
        void shouldHandleRecommendationNotFoundException() throws Exception {
            // Given
            when(cdsService.acknowledgeRecommendation(eq(TENANT_ID), any(CdsAcknowledgeRequest.class)))
                    .thenThrow(new IllegalArgumentException("Recommendation not found"));

            // When & Then
            mockMvc.perform(post("/cds/acknowledge")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(acknowledgeRequest))
                            .with(csrf()))
                    .andExpect(status().is4xxClientError());

            verify(cdsService, times(1)).acknowledgeRecommendation(eq(TENANT_ID), any(CdsAcknowledgeRequest.class));
        }

        @Test
        @WithMockUser(roles = "PROVIDER")
        @DisplayName("Should handle security exception for unauthorized access")
        void shouldHandleSecurityException() throws Exception {
            // Given
            when(cdsService.acknowledgeRecommendation(eq(TENANT_ID), any(CdsAcknowledgeRequest.class)))
                    .thenThrow(new SecurityException("Access denied to recommendation"));

            // When & Then
            mockMvc.perform(post("/cds/acknowledge")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(acknowledgeRequest))
                            .with(csrf()))
                    .andExpect(status().is4xxClientError());

            verify(cdsService, times(1)).acknowledgeRecommendation(eq(TENANT_ID), any(CdsAcknowledgeRequest.class));
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void shouldReturn401WhenNotAuthenticatedForAcknowledge() throws Exception {
            // When & Then
            mockMvc.perform(post("/cds/acknowledge")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(acknowledgeRequest))
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());

            verify(cdsService, never()).acknowledgeRecommendation(anyString(), any(CdsAcknowledgeRequest.class));
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("Should return 403 when user lacks required role")
        void shouldReturn403WhenInsufficientRoleForAcknowledge() throws Exception {
            // When & Then
            mockMvc.perform(post("/cds/acknowledge")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(acknowledgeRequest))
                            .with(csrf()))
                    .andExpect(status().isForbidden());

            verify(cdsService, never()).acknowledgeRecommendation(anyString(), any(CdsAcknowledgeRequest.class));
        }
    }
}
