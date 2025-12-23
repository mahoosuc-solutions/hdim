package com.healthdata.quality.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.client.CqlEngineServiceClient;
import com.healthdata.quality.dto.*;
import com.healthdata.quality.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for CdsService
 * Tests CDS rule management, recommendation retrieval, rule evaluation, and acknowledgments
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CDS Service Unit Tests")
class CdsServiceTest {

    @Mock
    private CdsRuleRepository ruleRepository;

    @Mock
    private CdsRecommendationRepository recommendationRepository;

    @Mock
    private CdsAcknowledgmentRepository acknowledgmentRepository;

    @Mock
    private CqlEngineServiceClient cqlEngineClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CdsService service;

    private String tenantId;
    private UUID patientId;
    private UUID ruleId;
    private UUID recommendationId;
    private CdsRuleEntity testRule;
    private CdsRecommendationEntity testRecommendation;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant-123";
        patientId = UUID.fromString("cdcdcdcd-1111-2222-3333-444444444444");
        ruleId = UUID.randomUUID();
        recommendationId = UUID.randomUUID();

        // Setup test rule
        testRule = CdsRuleEntity.builder()
            .id(ruleId)
            .tenantId(tenantId)
            .ruleCode("RULE-001")
            .ruleName("Diabetes HbA1c Screening")
            .description("Screen for HbA1c levels in diabetic patients")
            .category(CdsRuleEntity.CdsCategory.CHRONIC_DISEASE)
            .priority(1)
            .cqlLibraryName("DiabetesManagement")
            .cqlExpression("NeedsHbA1cTest")
            .recommendationTemplate("Patient needs HbA1c test")
            .actionItems("Order HbA1c lab test")
            .evidenceSource("ADA Guidelines 2024")
            .clinicalGuideline("ADA-2024-HbA1c")
            .defaultUrgency(CdsRuleEntity.CdsUrgency.ROUTINE)
            .active(true)
            .requiresAcknowledgment(true)
            .version("1.0")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Setup test recommendation
        testRecommendation = CdsRecommendationEntity.builder()
            .id(recommendationId)
            .tenantId(tenantId)
            .patientId(patientId)
            .ruleId(ruleId)
            .title("Diabetes HbA1c Screening")
            .description("Patient needs HbA1c test")
            .category(CdsRuleEntity.CdsCategory.CHRONIC_DISEASE)
            .urgency(CdsRuleEntity.CdsUrgency.ROUTINE)
            .status(CdsRecommendationEntity.CdsStatus.ACTIVE)
            .priority(1)
            .actionItems("Order HbA1c lab test")
            .evidenceSource("ADA Guidelines 2024")
            .clinicalGuideline("ADA-2024-HbA1c")
            .evaluatedAt(Instant.now())
            .dueDate(Instant.now().plus(30, ChronoUnit.DAYS))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    // ============================================
    // Tests for getActiveRules()
    // ============================================

    @Test
    @DisplayName("getActiveRules() - Should return all active rules for tenant")
    void getActiveRules_ShouldReturnActiveRulesForTenant() {
        // Given
        List<CdsRuleEntity> activeRules = Arrays.asList(
            testRule,
            CdsRuleEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .ruleCode("RULE-002")
                .ruleName("Blood Pressure Check")
                .category(CdsRuleEntity.CdsCategory.PREVENTIVE)
                .priority(2)
                .active(true)
                .defaultUrgency(CdsRuleEntity.CdsUrgency.ROUTINE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build()
        );

        when(ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId))
            .thenReturn(activeRules);

        // When
        List<CdsRuleDTO> result = service.getActiveRules(tenantId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRuleCode()).isEqualTo("RULE-001");
        assertThat(result.get(1).getRuleCode()).isEqualTo("RULE-002");
        verify(ruleRepository, times(1)).findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId);
    }

    @Test
    @DisplayName("getActiveRules() - Should return empty list when no active rules exist")
    void getActiveRules_ShouldReturnEmptyListWhenNoActiveRules() {
        // Given
        when(ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId))
            .thenReturn(Collections.emptyList());

        // When
        List<CdsRuleDTO> result = service.getActiveRules(tenantId);

        // Then
        assertThat(result).isEmpty();
        verify(ruleRepository, times(1)).findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId);
    }

    @Test
    @DisplayName("getActiveRules() - Should return rules in priority order")
    void getActiveRules_ShouldReturnRulesInPriorityOrder() {
        // Given
        CdsRuleEntity rule1 = CdsRuleEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .ruleCode("RULE-LOW-PRIORITY")
            .ruleName("Low Priority Rule")
            .category(CdsRuleEntity.CdsCategory.PREVENTIVE)
            .priority(10)
            .active(true)
            .defaultUrgency(CdsRuleEntity.CdsUrgency.ROUTINE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        CdsRuleEntity rule2 = CdsRuleEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .ruleCode("RULE-HIGH-PRIORITY")
            .ruleName("High Priority Rule")
            .category(CdsRuleEntity.CdsCategory.ALERT)
            .priority(1)
            .active(true)
            .defaultUrgency(CdsRuleEntity.CdsUrgency.EMERGENT)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId))
            .thenReturn(Arrays.asList(rule2, rule1)); // Repository returns in priority order

        // When
        List<CdsRuleDTO> result = service.getActiveRules(tenantId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPriority()).isEqualTo(1);
        assertThat(result.get(1).getPriority()).isEqualTo(10);
    }

    // ============================================
    // Tests for getAllRules()
    // ============================================

    @Test
    @DisplayName("getAllRules() - Should return all rules including inactive")
    void getAllRules_ShouldReturnAllRulesIncludingInactive() {
        // Given
        CdsRuleEntity inactiveRule = CdsRuleEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .ruleCode("RULE-INACTIVE")
            .ruleName("Inactive Rule")
            .category(CdsRuleEntity.CdsCategory.PREVENTIVE)
            .priority(5)
            .active(false)
            .defaultUrgency(CdsRuleEntity.CdsUrgency.ROUTINE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        List<CdsRuleEntity> allRules = Arrays.asList(testRule, inactiveRule);

        when(ruleRepository.findByTenantIdOrderByPriorityAsc(tenantId))
            .thenReturn(allRules);

        // When
        List<CdsRuleDTO> result = service.getAllRules(tenantId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.stream().anyMatch(r -> !r.getActive())).isTrue();
        verify(ruleRepository, times(1)).findByTenantIdOrderByPriorityAsc(tenantId);
    }

    @Test
    @DisplayName("getAllRules() - Should return empty list when no rules exist")
    void getAllRules_ShouldReturnEmptyListWhenNoRulesExist() {
        // Given
        when(ruleRepository.findByTenantIdOrderByPriorityAsc(tenantId))
            .thenReturn(Collections.emptyList());

        // When
        List<CdsRuleDTO> result = service.getAllRules(tenantId);

        // Then
        assertThat(result).isEmpty();
        verify(ruleRepository, times(1)).findByTenantIdOrderByPriorityAsc(tenantId);
    }

    // ============================================
    // Tests for getRulesByCategory()
    // ============================================

    @Test
    @DisplayName("getRulesByCategory() - Should return rules filtered by category")
    void getRulesByCategory_ShouldReturnRulesFilteredByCategory() {
        // Given
        String category = "CHRONIC_DISEASE";
        List<CdsRuleEntity> categoryRules = Collections.singletonList(testRule);

        when(ruleRepository.findByTenantIdAndCategoryAndActiveTrueOrderByPriorityAsc(
            eq(tenantId), eq(CdsRuleEntity.CdsCategory.CHRONIC_DISEASE)))
            .thenReturn(categoryRules);

        // When
        List<CdsRuleDTO> result = service.getRulesByCategory(tenantId, category);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("CHRONIC_DISEASE");
        verify(ruleRepository, times(1))
            .findByTenantIdAndCategoryAndActiveTrueOrderByPriorityAsc(
                tenantId, CdsRuleEntity.CdsCategory.CHRONIC_DISEASE);
    }

    @Test
    @DisplayName("getRulesByCategory() - Should handle case-insensitive category names")
    void getRulesByCategory_ShouldHandleCaseInsensitiveCategoryNames() {
        // Given
        String category = "chronic_disease"; // lowercase
        List<CdsRuleEntity> categoryRules = Collections.singletonList(testRule);

        when(ruleRepository.findByTenantIdAndCategoryAndActiveTrueOrderByPriorityAsc(
            eq(tenantId), eq(CdsRuleEntity.CdsCategory.CHRONIC_DISEASE)))
            .thenReturn(categoryRules);

        // When
        List<CdsRuleDTO> result = service.getRulesByCategory(tenantId, category);

        // Then
        assertThat(result).hasSize(1);
        verify(ruleRepository, times(1))
            .findByTenantIdAndCategoryAndActiveTrueOrderByPriorityAsc(
                tenantId, CdsRuleEntity.CdsCategory.CHRONIC_DISEASE);
    }

    @Test
    @DisplayName("getRulesByCategory() - Should return empty list for category with no rules")
    void getRulesByCategory_ShouldReturnEmptyListForCategoryWithNoRules() {
        // Given
        String category = "SDOH";
        when(ruleRepository.findByTenantIdAndCategoryAndActiveTrueOrderByPriorityAsc(
            eq(tenantId), eq(CdsRuleEntity.CdsCategory.SDOH)))
            .thenReturn(Collections.emptyList());

        // When
        List<CdsRuleDTO> result = service.getRulesByCategory(tenantId, category);

        // Then
        assertThat(result).isEmpty();
    }

    // ============================================
    // Tests for getActiveRecommendations()
    // ============================================

    @Test
    @DisplayName("getActiveRecommendations() - Should return active recommendations for patient")
    void getActiveRecommendations_ShouldReturnActiveRecommendationsForPatient() {
        // Given
        List<CdsRecommendationEntity> activeRecommendations = Arrays.asList(
            testRecommendation,
            CdsRecommendationEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .patientId(patientId)
                .ruleId(UUID.randomUUID())
                .title("Flu Vaccine Reminder")
                .category(CdsRuleEntity.CdsCategory.PREVENTIVE)
                .urgency(CdsRuleEntity.CdsUrgency.ROUTINE)
                .status(CdsRecommendationEntity.CdsStatus.ACTIVE)
                .priority(3)
                .evaluatedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build()
        );

        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(activeRecommendations);

        // When
        List<CdsRecommendationDTO> result = service.getActiveRecommendations(tenantId, patientId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Diabetes HbA1c Screening");
        assertThat(result.get(1).getTitle()).isEqualTo("Flu Vaccine Reminder");
        verify(recommendationRepository, times(1)).findActiveRecommendations(tenantId, patientId);
    }

    @Test
    @DisplayName("getActiveRecommendations() - Should return empty list when no active recommendations")
    void getActiveRecommendations_ShouldReturnEmptyListWhenNoActiveRecommendations() {
        // Given
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        // When
        List<CdsRecommendationDTO> result = service.getActiveRecommendations(tenantId, patientId);

        // Then
        assertThat(result).isEmpty();
        verify(recommendationRepository, times(1)).findActiveRecommendations(tenantId, patientId);
    }

    // ============================================
    // Tests for getUrgentRecommendations()
    // ============================================

    @Test
    @DisplayName("getUrgentRecommendations() - Should return urgent and emergent recommendations")
    void getUrgentRecommendations_ShouldReturnUrgentAndEmergentRecommendations() {
        // Given
        CdsRecommendationEntity urgentRec = CdsRecommendationEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .ruleId(UUID.randomUUID())
            .title("Critical BP Reading")
            .category(CdsRuleEntity.CdsCategory.ALERT)
            .urgency(CdsRuleEntity.CdsUrgency.URGENT)
            .status(CdsRecommendationEntity.CdsStatus.ACTIVE)
            .priority(1)
            .evaluatedAt(Instant.now())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        CdsRecommendationEntity emergentRec = CdsRecommendationEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .ruleId(UUID.randomUUID())
            .title("Immediate Intervention Required")
            .category(CdsRuleEntity.CdsCategory.ALERT)
            .urgency(CdsRuleEntity.CdsUrgency.EMERGENT)
            .status(CdsRecommendationEntity.CdsStatus.ACTIVE)
            .priority(1)
            .evaluatedAt(Instant.now())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        List<CdsRecommendationEntity> urgentRecommendations = Arrays.asList(emergentRec, urgentRec);

        when(recommendationRepository.findUrgentRecommendations(tenantId, patientId))
            .thenReturn(urgentRecommendations);

        // When
        List<CdsRecommendationDTO> result = service.getUrgentRecommendations(tenantId, patientId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.stream().anyMatch(r -> r.getUrgency().equals("EMERGENT"))).isTrue();
        assertThat(result.stream().anyMatch(r -> r.getUrgency().equals("URGENT"))).isTrue();
        verify(recommendationRepository, times(1)).findUrgentRecommendations(tenantId, patientId);
    }

    @Test
    @DisplayName("getUrgentRecommendations() - Should return empty list when no urgent recommendations")
    void getUrgentRecommendations_ShouldReturnEmptyListWhenNoUrgentRecommendations() {
        // Given
        when(recommendationRepository.findUrgentRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        // When
        List<CdsRecommendationDTO> result = service.getUrgentRecommendations(tenantId, patientId);

        // Then
        assertThat(result).isEmpty();
        verify(recommendationRepository, times(1)).findUrgentRecommendations(tenantId, patientId);
    }

    // ============================================
    // Tests for evaluateRules()
    // ============================================

    @Test
    @DisplayName("evaluateRules() - Should evaluate all active rules when no filters specified")
    void evaluateRules_ShouldEvaluateAllActiveRulesWhenNoFilters() {
        // Given
        CdsEvaluateRequest request = CdsEvaluateRequest.builder()
            .patientId(patientId)
            .forceReEvaluation(false)
            .build();

        List<CdsRuleEntity> activeRules = Collections.singletonList(testRule);

        when(ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId))
            .thenReturn(activeRules);
        when(recommendationRepository.existsActiveRecommendation(eq(tenantId), eq(patientId), any()))
            .thenReturn(false);
        when(cqlEngineClient.evaluateCql(eq(tenantId), eq("DiabetesManagement"), eq(patientId), isNull()))
            .thenReturn("{\"triggered\":true}");
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        // When
        CdsEvaluateResponse response = service.evaluateRules(tenantId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPatientId()).isEqualTo(patientId);
        assertThat(response.getRulesEvaluated()).isEqualTo(1);
        assertThat(response.getRecommendationsGenerated()).isEqualTo(1);
        assertThat(response.getNewRecommendations()).hasSize(1);
        verify(ruleRepository, times(1)).findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId);
        verify(cqlEngineClient, times(1)).evaluateCql(eq(tenantId), eq("DiabetesManagement"), eq(patientId), isNull());
        verify(recommendationRepository, times(1)).save(any(CdsRecommendationEntity.class));
    }

    @Test
    @DisplayName("evaluateRules() - Should skip rules with existing active recommendations")
    void evaluateRules_ShouldSkipRulesWithExistingActiveRecommendations() {
        // Given
        CdsEvaluateRequest request = CdsEvaluateRequest.builder()
            .patientId(patientId)
            .forceReEvaluation(false)
            .build();

        List<CdsRuleEntity> activeRules = Collections.singletonList(testRule);

        when(ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId))
            .thenReturn(activeRules);
        when(recommendationRepository.existsActiveRecommendation(tenantId, patientId, ruleId))
            .thenReturn(true); // Active recommendation exists
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        // When
        CdsEvaluateResponse response = service.evaluateRules(tenantId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRulesEvaluated()).isEqualTo(1);
        assertThat(response.getRecommendationsGenerated()).isEqualTo(0);
        assertThat(response.getExistingRecommendationsSkipped()).isEqualTo(1);
        verify(cqlEngineClient, never()).evaluateCql(anyString(), anyString(), any(UUID.class), any());
        verify(recommendationRepository, never()).save(any(CdsRecommendationEntity.class));
    }

    @Test
    @DisplayName("evaluateRules() - Should force re-evaluation when requested")
    void evaluateRules_ShouldForceReEvaluationWhenRequested() {
        // Given
        CdsEvaluateRequest request = CdsEvaluateRequest.builder()
            .patientId(patientId)
            .forceReEvaluation(true) // Force re-evaluation
            .build();

        List<CdsRuleEntity> activeRules = Collections.singletonList(testRule);

        when(ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId))
            .thenReturn(activeRules);
        when(cqlEngineClient.evaluateCql(eq(tenantId), eq("DiabetesManagement"), eq(patientId), isNull()))
            .thenReturn("{\"triggered\":true}");
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        // When
        CdsEvaluateResponse response = service.evaluateRules(tenantId, request);

        // Then
        assertThat(response.getRecommendationsGenerated()).isEqualTo(1);
        verify(cqlEngineClient, times(1)).evaluateCql(eq(tenantId), eq("DiabetesManagement"), eq(patientId), isNull());
        verify(recommendationRepository, times(1)).save(any(CdsRecommendationEntity.class));
    }

    @Test
    @DisplayName("evaluateRules() - Should not create recommendation when rule does not trigger")
    void evaluateRules_ShouldNotCreateRecommendationWhenRuleDoesNotTrigger() {
        // Given
        CdsEvaluateRequest request = CdsEvaluateRequest.builder()
            .patientId(patientId)
            .build();

        List<CdsRuleEntity> activeRules = Collections.singletonList(testRule);

        when(ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId))
            .thenReturn(activeRules);
        when(recommendationRepository.existsActiveRecommendation(eq(tenantId), eq(patientId), any()))
            .thenReturn(false);
        when(cqlEngineClient.evaluateCql(eq(tenantId), eq("DiabetesManagement"), eq(patientId), isNull()))
            .thenReturn("{\"triggered\":false}"); // Rule does not trigger
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        // When
        CdsEvaluateResponse response = service.evaluateRules(tenantId, request);

        // Then
        assertThat(response.getRulesEvaluated()).isEqualTo(1);
        assertThat(response.getRecommendationsGenerated()).isEqualTo(0);
        verify(recommendationRepository, never()).save(any(CdsRecommendationEntity.class));
    }

    @Test
    @DisplayName("evaluateRules() - Should treat blank CQL results as not triggered")
    void evaluateRules_ShouldTreatBlankCqlResultsAsNotTriggered() throws Exception {
        // Given
        CdsEvaluateRequest request = CdsEvaluateRequest.builder()
            .patientId(patientId)
            .build();

        when(ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId))
            .thenReturn(Collections.singletonList(testRule));
        when(recommendationRepository.existsActiveRecommendation(eq(tenantId), eq(patientId), any()))
            .thenReturn(false);
        when(cqlEngineClient.evaluateCql(eq(tenantId), eq("DiabetesManagement"), eq(patientId), isNull()))
            .thenReturn("   ");
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        // When
        CdsEvaluateResponse response = service.evaluateRules(tenantId, request);

        // Then
        assertThat(response.getRecommendationsGenerated()).isEqualTo(0);
        verify(recommendationRepository, never()).save(any(CdsRecommendationEntity.class));
        assertThat(response.getEvaluationDetails()).hasSize(1);
        assertThat(response.getEvaluationDetails().get(0).getResult()).isEqualTo("NOT_TRIGGERED");
    }

    @Test
    @DisplayName("evaluateRules() - Should evaluate only specified rule IDs")
    void evaluateRules_ShouldEvaluateOnlySpecifiedRuleIds() {
        // Given
        CdsEvaluateRequest request = CdsEvaluateRequest.builder()
            .patientId(patientId)
            .ruleIds(Collections.singletonList(ruleId))
            .build();

        when(ruleRepository.findAllById(Collections.singletonList(ruleId)))
            .thenReturn(Collections.singletonList(testRule));
        when(recommendationRepository.existsActiveRecommendation(eq(tenantId), eq(patientId), any()))
            .thenReturn(false);
        when(cqlEngineClient.evaluateCql(eq(tenantId), eq("DiabetesManagement"), eq(patientId), isNull()))
            .thenReturn("{\"triggered\":true}");
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        // When
        CdsEvaluateResponse response = service.evaluateRules(tenantId, request);

        // Then
        assertThat(response.getRulesEvaluated()).isEqualTo(1);
        verify(ruleRepository, times(1)).findAllById(Collections.singletonList(ruleId));
        verify(ruleRepository, never()).findByTenantIdAndActiveTrueOrderByPriorityAsc(anyString());
    }

    @Test
    @DisplayName("evaluateRules() - Should evaluate only rules in specified categories")
    void evaluateRules_ShouldEvaluateOnlyRulesInSpecifiedCategories() {
        // Given
        CdsEvaluateRequest request = CdsEvaluateRequest.builder()
            .patientId(patientId)
            .categories(Collections.singletonList("CHRONIC_DISEASE"))
            .build();

        when(ruleRepository.findByTenantIdAndCategoryAndActiveTrueOrderByPriorityAsc(
            eq(tenantId), eq(CdsRuleEntity.CdsCategory.CHRONIC_DISEASE)))
            .thenReturn(Collections.singletonList(testRule));
        when(recommendationRepository.existsActiveRecommendation(eq(tenantId), eq(patientId), any()))
            .thenReturn(false);
        when(cqlEngineClient.evaluateCql(eq(tenantId), eq("DiabetesManagement"), eq(patientId), isNull()))
            .thenReturn("{\"triggered\":true}");
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        // When
        CdsEvaluateResponse response = service.evaluateRules(tenantId, request);

        // Then
        assertThat(response.getRulesEvaluated()).isEqualTo(1);
        verify(ruleRepository, times(1))
            .findByTenantIdAndCategoryAndActiveTrueOrderByPriorityAsc(
                tenantId, CdsRuleEntity.CdsCategory.CHRONIC_DISEASE);
    }

    @Test
    @DisplayName("evaluateRules() - Should handle CQL engine exceptions gracefully")
    void evaluateRules_ShouldHandleCqlEngineExceptionsGracefully() {
        // Given
        CdsEvaluateRequest request = CdsEvaluateRequest.builder()
            .patientId(patientId)
            .build();

        List<CdsRuleEntity> activeRules = Collections.singletonList(testRule);

        when(ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId))
            .thenReturn(activeRules);
        when(recommendationRepository.existsActiveRecommendation(eq(tenantId), eq(patientId), any()))
            .thenReturn(false);
        when(cqlEngineClient.evaluateCql(eq(tenantId), eq("DiabetesManagement"), eq(patientId), isNull()))
            .thenThrow(new RuntimeException("CQL engine connection failed"));
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        // When
        CdsEvaluateResponse response = service.evaluateRules(tenantId, request);

        // Then
        assertThat(response.getRulesEvaluated()).isEqualTo(1);
        // When CQL engine fails, service falls back to default evaluation which returns true
        // So a recommendation IS created as a fallback behavior
        assertThat(response.getRecommendationsGenerated()).isEqualTo(1);
        assertThat(response.getNewRecommendations()).hasSize(1);
        verify(recommendationRepository, times(1)).save(any(CdsRecommendationEntity.class));
    }

    @Test
    @DisplayName("evaluateRules() - Should create recommendation with correct due date for EMERGENT urgency")
    void evaluateRules_ShouldCreateRecommendationWithCorrectDueDateForEmergentUrgency() {
        // Given
        testRule.setDefaultUrgency(CdsRuleEntity.CdsUrgency.EMERGENT);

        CdsEvaluateRequest request = CdsEvaluateRequest.builder()
            .patientId(patientId)
            .build();

        when(ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId))
            .thenReturn(Collections.singletonList(testRule));
        when(recommendationRepository.existsActiveRecommendation(eq(tenantId), eq(patientId), any()))
            .thenReturn(false);
        when(cqlEngineClient.evaluateCql(eq(tenantId), eq("DiabetesManagement"), eq(patientId), isNull()))
            .thenReturn("{\"triggered\":true}");
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        // When
        service.evaluateRules(tenantId, request);

        // Then
        ArgumentCaptor<CdsRecommendationEntity> captor = ArgumentCaptor.forClass(CdsRecommendationEntity.class);
        verify(recommendationRepository).save(captor.capture());
        CdsRecommendationEntity savedRecommendation = captor.getValue();

        assertThat(savedRecommendation.getUrgency()).isEqualTo(CdsRuleEntity.CdsUrgency.EMERGENT);
        assertThat(savedRecommendation.getDueDate()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("evaluateRules() - Should create recommendation with correct due date for URGENT urgency")
    void evaluateRules_ShouldCreateRecommendationWithCorrectDueDateForUrgentUrgency() {
        // Given
        testRule.setDefaultUrgency(CdsRuleEntity.CdsUrgency.URGENT);

        CdsEvaluateRequest request = CdsEvaluateRequest.builder()
            .patientId(patientId)
            .build();

        when(ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId))
            .thenReturn(Collections.singletonList(testRule));
        when(recommendationRepository.existsActiveRecommendation(eq(tenantId), eq(patientId), any()))
            .thenReturn(false);
        when(cqlEngineClient.evaluateCql(eq(tenantId), eq("DiabetesManagement"), eq(patientId), isNull()))
            .thenReturn("{\"triggered\":true}");
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        // When
        service.evaluateRules(tenantId, request);

        // Then
        ArgumentCaptor<CdsRecommendationEntity> captor = ArgumentCaptor.forClass(CdsRecommendationEntity.class);
        verify(recommendationRepository).save(captor.capture());
        CdsRecommendationEntity savedRecommendation = captor.getValue();

        assertThat(savedRecommendation.getUrgency()).isEqualTo(CdsRuleEntity.CdsUrgency.URGENT);
        Instant expectedDueDate = Instant.now().plusSeconds(48 * 60 * 60); // 48 hours
        assertThat(savedRecommendation.getDueDate()).isCloseTo(expectedDueDate, within(1, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("evaluateRules() - Should include evaluation details in response")
    void evaluateRules_ShouldIncludeEvaluationDetailsInResponse() {
        // Given
        CdsEvaluateRequest request = CdsEvaluateRequest.builder()
            .patientId(patientId)
            .build();

        when(ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId))
            .thenReturn(Collections.singletonList(testRule));
        when(recommendationRepository.existsActiveRecommendation(eq(tenantId), eq(patientId), any()))
            .thenReturn(false);
        when(cqlEngineClient.evaluateCql(eq(tenantId), eq("DiabetesManagement"), eq(patientId), isNull()))
            .thenReturn("{\"triggered\":true}");
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        // When
        CdsEvaluateResponse response = service.evaluateRules(tenantId, request);

        // Then
        assertThat(response.getEvaluationDetails()).hasSize(1);
        assertThat(response.getEvaluationDetails().get(0).getRuleCode()).isEqualTo("RULE-001");
        assertThat(response.getEvaluationDetails().get(0).getRuleName()).isEqualTo("Diabetes HbA1c Screening");
        assertThat(response.getEvaluationDetails().get(0).getTriggered()).isTrue();
        assertThat(response.getEvaluationDetails().get(0).getEvaluationTimeMs()).isNotNull();
    }

    // ============================================
    // Tests for acknowledgeRecommendation()
    // ============================================

    @Test
    @DisplayName("acknowledgeRecommendation() - Should update status to ACKNOWLEDGED")
    void acknowledgeRecommendation_ShouldUpdateStatusToAcknowledged() {
        // Given
        CdsAcknowledgeRequest request = CdsAcknowledgeRequest.builder()
            .recommendationId(recommendationId)
            .action("ACKNOWLEDGED")
            .userId("user-123")
            .userName("Dr. Smith")
            .userRole("PHYSICIAN")
            .notes("Acknowledged recommendation")
            .build();

        when(recommendationRepository.findById(recommendationId))
            .thenReturn(Optional.of(testRecommendation));
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(acknowledgmentRepository.save(any(CdsAcknowledgmentEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CdsRecommendationDTO result = service.acknowledgeRecommendation(tenantId, request);

        // Then
        assertThat(result).isNotNull();
        ArgumentCaptor<CdsRecommendationEntity> recCaptor = ArgumentCaptor.forClass(CdsRecommendationEntity.class);
        verify(recommendationRepository).save(recCaptor.capture());
        CdsRecommendationEntity savedRec = recCaptor.getValue();

        assertThat(savedRec.getStatus()).isEqualTo(CdsRecommendationEntity.CdsStatus.ACKNOWLEDGED);
        assertThat(savedRec.getAcknowledgedAt()).isNotNull();
        assertThat(savedRec.getAcknowledgedBy()).isEqualTo("user-123");
        assertThat(savedRec.getAcknowledgmentNotes()).isEqualTo("Acknowledged recommendation");

        // Verify acknowledgment entity was saved
        ArgumentCaptor<CdsAcknowledgmentEntity> ackCaptor = ArgumentCaptor.forClass(CdsAcknowledgmentEntity.class);
        verify(acknowledgmentRepository).save(ackCaptor.capture());
        CdsAcknowledgmentEntity savedAck = ackCaptor.getValue();

        assertThat(savedAck.getRecommendationId()).isEqualTo(recommendationId);
        assertThat(savedAck.getActionType()).isEqualTo(CdsAcknowledgmentEntity.ActionType.ACKNOWLEDGED);
        assertThat(savedAck.getUserId()).isEqualTo("user-123");
        assertThat(savedAck.getPreviousStatus()).isEqualTo("ACTIVE");
        assertThat(savedAck.getNewStatus()).isEqualTo("ACKNOWLEDGED");
    }

    @Test
    @DisplayName("acknowledgeRecommendation() - Should update status to IN_PROGRESS when ACCEPTED")
    void acknowledgeRecommendation_ShouldUpdateStatusToInProgressWhenAccepted() {
        // Given
        CdsAcknowledgeRequest request = CdsAcknowledgeRequest.builder()
            .recommendationId(recommendationId)
            .action("ACCEPTED")
            .userId("user-123")
            .userName("Dr. Smith")
            .userRole("PHYSICIAN")
            .build();

        when(recommendationRepository.findById(recommendationId))
            .thenReturn(Optional.of(testRecommendation));
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(acknowledgmentRepository.save(any(CdsAcknowledgmentEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CdsRecommendationDTO result = service.acknowledgeRecommendation(tenantId, request);

        // Then
        ArgumentCaptor<CdsRecommendationEntity> captor = ArgumentCaptor.forClass(CdsRecommendationEntity.class);
        verify(recommendationRepository).save(captor.capture());
        CdsRecommendationEntity savedRec = captor.getValue();

        assertThat(savedRec.getStatus()).isEqualTo(CdsRecommendationEntity.CdsStatus.IN_PROGRESS);
        assertThat(savedRec.getAcknowledgedAt()).isNotNull();
        assertThat(savedRec.getAcknowledgedBy()).isEqualTo("user-123");
    }

    @Test
    @DisplayName("acknowledgeRecommendation() - Should update status to COMPLETED with outcome")
    void acknowledgeRecommendation_ShouldUpdateStatusToCompletedWithOutcome() {
        // Given
        CdsAcknowledgeRequest request = CdsAcknowledgeRequest.builder()
            .recommendationId(recommendationId)
            .action("COMPLETED")
            .userId("user-123")
            .userName("Dr. Smith")
            .userRole("PHYSICIAN")
            .outcome("HbA1c test ordered and completed. Results: 7.2%")
            .build();

        when(recommendationRepository.findById(recommendationId))
            .thenReturn(Optional.of(testRecommendation));
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(acknowledgmentRepository.save(any(CdsAcknowledgmentEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CdsRecommendationDTO result = service.acknowledgeRecommendation(tenantId, request);

        // Then
        ArgumentCaptor<CdsRecommendationEntity> captor = ArgumentCaptor.forClass(CdsRecommendationEntity.class);
        verify(recommendationRepository).save(captor.capture());
        CdsRecommendationEntity savedRec = captor.getValue();

        assertThat(savedRec.getStatus()).isEqualTo(CdsRecommendationEntity.CdsStatus.COMPLETED);
        assertThat(savedRec.getCompletedAt()).isNotNull();
        assertThat(savedRec.getCompletedBy()).isEqualTo("user-123");
        assertThat(savedRec.getCompletionOutcome()).isEqualTo("HbA1c test ordered and completed. Results: 7.2%");
    }

    @Test
    @DisplayName("acknowledgeRecommendation() - Should update status to DECLINED with reason")
    void acknowledgeRecommendation_ShouldUpdateStatusToDeclinedWithReason() {
        // Given
        CdsAcknowledgeRequest request = CdsAcknowledgeRequest.builder()
            .recommendationId(recommendationId)
            .action("DECLINED")
            .userId("user-123")
            .userName("Dr. Smith")
            .userRole("PHYSICIAN")
            .reason("Patient recently had HbA1c test at another facility")
            .build();

        when(recommendationRepository.findById(recommendationId))
            .thenReturn(Optional.of(testRecommendation));
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(acknowledgmentRepository.save(any(CdsAcknowledgmentEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CdsRecommendationDTO result = service.acknowledgeRecommendation(tenantId, request);

        // Then
        ArgumentCaptor<CdsRecommendationEntity> captor = ArgumentCaptor.forClass(CdsRecommendationEntity.class);
        verify(recommendationRepository).save(captor.capture());
        CdsRecommendationEntity savedRec = captor.getValue();

        assertThat(savedRec.getStatus()).isEqualTo(CdsRecommendationEntity.CdsStatus.DECLINED);
        assertThat(savedRec.getDeclinedAt()).isNotNull();
        assertThat(savedRec.getDeclinedBy()).isEqualTo("user-123");
        assertThat(savedRec.getDeclineReason()).isEqualTo("Patient recently had HbA1c test at another facility");
    }

    @Test
    @DisplayName("acknowledgeRecommendation() - Should update status to DISMISSED")
    void acknowledgeRecommendation_ShouldUpdateStatusToDismissed() {
        // Given
        CdsAcknowledgeRequest request = CdsAcknowledgeRequest.builder()
            .recommendationId(recommendationId)
            .action("DISMISSED")
            .userId("user-123")
            .userName("Dr. Smith")
            .userRole("PHYSICIAN")
            .reason("Patient no longer has diabetes diagnosis")
            .build();

        when(recommendationRepository.findById(recommendationId))
            .thenReturn(Optional.of(testRecommendation));
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(acknowledgmentRepository.save(any(CdsAcknowledgmentEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CdsRecommendationDTO result = service.acknowledgeRecommendation(tenantId, request);

        // Then
        ArgumentCaptor<CdsRecommendationEntity> captor = ArgumentCaptor.forClass(CdsRecommendationEntity.class);
        verify(recommendationRepository).save(captor.capture());
        CdsRecommendationEntity savedRec = captor.getValue();

        assertThat(savedRec.getStatus()).isEqualTo(CdsRecommendationEntity.CdsStatus.DISMISSED);
        assertThat(savedRec.getDeclinedAt()).isNotNull();
        assertThat(savedRec.getDeclinedBy()).isEqualTo("user-123");
        assertThat(savedRec.getDeclineReason()).isEqualTo("Patient no longer has diabetes diagnosis");
    }

    @Test
    @DisplayName("acknowledgeRecommendation() - Should throw exception when recommendation not found")
    void acknowledgeRecommendation_ShouldThrowExceptionWhenRecommendationNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        CdsAcknowledgeRequest request = CdsAcknowledgeRequest.builder()
            .recommendationId(nonExistentId)
            .action("ACKNOWLEDGED")
            .userId("user-123")
            .build();

        when(recommendationRepository.findById(nonExistentId))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.acknowledgeRecommendation(tenantId, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Recommendation not found");

        verify(recommendationRepository, never()).save(any());
        verify(acknowledgmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("acknowledgeRecommendation() - Should enforce tenant isolation")
    void acknowledgeRecommendation_ShouldEnforceTenantIsolation() {
        // Given
        String differentTenantId = "different-tenant";
        CdsAcknowledgeRequest request = CdsAcknowledgeRequest.builder()
            .recommendationId(recommendationId)
            .action("ACKNOWLEDGED")
            .userId("user-123")
            .build();

        when(recommendationRepository.findById(recommendationId))
            .thenReturn(Optional.of(testRecommendation));

        // When/Then
        assertThatThrownBy(() -> service.acknowledgeRecommendation(differentTenantId, request))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Access denied");

        verify(recommendationRepository, never()).save(any());
        verify(acknowledgmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("acknowledgeRecommendation() - Should throw exception for invalid action type")
    void acknowledgeRecommendation_ShouldThrowExceptionForInvalidActionType() {
        // Given
        CdsAcknowledgeRequest request = CdsAcknowledgeRequest.builder()
            .recommendationId(recommendationId)
            .action("INVALID_ACTION")
            .userId("user-123")
            .build();

        when(recommendationRepository.findById(recommendationId))
            .thenReturn(Optional.of(testRecommendation));

        // When/Then
        assertThatThrownBy(() -> service.acknowledgeRecommendation(tenantId, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid action type");

        verify(recommendationRepository, never()).save(any());
        verify(acknowledgmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("acknowledgeRecommendation() - Should preserve status for DEFERRED action")
    void acknowledgeRecommendation_ShouldPreserveStatusForDeferredAction() {
        // Given
        CdsAcknowledgeRequest request = CdsAcknowledgeRequest.builder()
            .recommendationId(recommendationId)
            .action("DEFERRED")
            .userId("user-123")
            .followUpDate(Instant.now().plus(7, ChronoUnit.DAYS))
            .followUpNotes("Defer until next visit")
            .build();

        when(recommendationRepository.findById(recommendationId))
            .thenReturn(Optional.of(testRecommendation));
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(acknowledgmentRepository.save(any(CdsAcknowledgmentEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CdsRecommendationDTO result = service.acknowledgeRecommendation(tenantId, request);

        // Then
        ArgumentCaptor<CdsRecommendationEntity> captor = ArgumentCaptor.forClass(CdsRecommendationEntity.class);
        verify(recommendationRepository).save(captor.capture());
        CdsRecommendationEntity savedRec = captor.getValue();

        // Status should remain ACTIVE
        assertThat(savedRec.getStatus()).isEqualTo(CdsRecommendationEntity.CdsStatus.ACTIVE);

        // Verify acknowledgment was recorded
        ArgumentCaptor<CdsAcknowledgmentEntity> ackCaptor = ArgumentCaptor.forClass(CdsAcknowledgmentEntity.class);
        verify(acknowledgmentRepository).save(ackCaptor.capture());
        CdsAcknowledgmentEntity savedAck = ackCaptor.getValue();

        assertThat(savedAck.getActionType()).isEqualTo(CdsAcknowledgmentEntity.ActionType.DEFERRED);
        assertThat(savedAck.getFollowUpDate()).isNotNull();
        assertThat(savedAck.getFollowUpNotes()).isEqualTo("Defer until next visit");
    }

    @Test
    @DisplayName("acknowledgeRecommendation() - Should preserve status for SNOOZED action")
    void acknowledgeRecommendation_ShouldPreserveStatusForSnoozedAction() {
        // Given
        CdsAcknowledgeRequest request = CdsAcknowledgeRequest.builder()
            .recommendationId(recommendationId)
            .action("SNOOZED")
            .userId("user-123")
            .build();

        when(recommendationRepository.findById(recommendationId))
            .thenReturn(Optional.of(testRecommendation));
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(acknowledgmentRepository.save(any(CdsAcknowledgmentEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        service.acknowledgeRecommendation(tenantId, request);

        // Then
        ArgumentCaptor<CdsRecommendationEntity> captor = ArgumentCaptor.forClass(CdsRecommendationEntity.class);
        verify(recommendationRepository).save(captor.capture());
        CdsRecommendationEntity savedRec = captor.getValue();

        // Status should remain ACTIVE
        assertThat(savedRec.getStatus()).isEqualTo(CdsRecommendationEntity.CdsStatus.ACTIVE);
    }

    @Test
    @DisplayName("acknowledgeRecommendation() - Should record status transition in acknowledgment")
    void acknowledgeRecommendation_ShouldRecordStatusTransitionInAcknowledgment() {
        // Given
        testRecommendation.setStatus(CdsRecommendationEntity.CdsStatus.ACKNOWLEDGED);

        CdsAcknowledgeRequest request = CdsAcknowledgeRequest.builder()
            .recommendationId(recommendationId)
            .action("COMPLETED")
            .userId("user-123")
            .outcome("Test completed successfully")
            .build();

        when(recommendationRepository.findById(recommendationId))
            .thenReturn(Optional.of(testRecommendation));
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(acknowledgmentRepository.save(any(CdsAcknowledgmentEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        service.acknowledgeRecommendation(tenantId, request);

        // Then
        ArgumentCaptor<CdsAcknowledgmentEntity> ackCaptor = ArgumentCaptor.forClass(CdsAcknowledgmentEntity.class);
        verify(acknowledgmentRepository).save(ackCaptor.capture());
        CdsAcknowledgmentEntity savedAck = ackCaptor.getValue();

        assertThat(savedAck.getPreviousStatus()).isEqualTo("ACKNOWLEDGED");
        assertThat(savedAck.getNewStatus()).isEqualTo("COMPLETED");
    }

    // ============================================
    // Edge Cases and Error Handling
    // ============================================

    @Test
    @DisplayName("evaluateRules() - Should handle rule without CQL configuration")
    void evaluateRules_ShouldHandleRuleWithoutCqlConfiguration() {
        // Given
        testRule.setCqlLibraryName(null);
        testRule.setCqlExpression(null);

        CdsEvaluateRequest request = CdsEvaluateRequest.builder()
            .patientId(patientId)
            .build();

        when(ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId))
            .thenReturn(Collections.singletonList(testRule));
        when(recommendationRepository.existsActiveRecommendation(eq(tenantId), eq(patientId), any()))
            .thenReturn(false);
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        // When
        CdsEvaluateResponse response = service.evaluateRules(tenantId, request);

        // Then
        assertThat(response.getRecommendationsGenerated()).isEqualTo(1); // Uses default evaluation
        verify(cqlEngineClient, never()).evaluateCql(anyString(), anyString(), any(UUID.class), any());
    }

    @Test
    @DisplayName("evaluateRules() - Should handle multi-tenant isolation")
    void evaluateRules_ShouldHandleMultiTenantIsolation() {
        // Given
        UUID wrongTenantRuleId = UUID.randomUUID();
        CdsRuleEntity wrongTenantRule = CdsRuleEntity.builder()
            .id(wrongTenantRuleId)
            .tenantId("different-tenant")
            .ruleCode("RULE-OTHER")
            .ruleName("Other Tenant Rule")
            .category(CdsRuleEntity.CdsCategory.PREVENTIVE)
            .priority(1)
            .active(true)
            .defaultUrgency(CdsRuleEntity.CdsUrgency.ROUTINE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        CdsEvaluateRequest request = CdsEvaluateRequest.builder()
            .patientId(patientId)
            .ruleIds(Arrays.asList(ruleId, wrongTenantRuleId))
            .build();

        when(ruleRepository.findAllById(Arrays.asList(ruleId, wrongTenantRuleId)))
            .thenReturn(Arrays.asList(testRule, wrongTenantRule));
        when(recommendationRepository.existsActiveRecommendation(eq(tenantId), eq(patientId), any()))
            .thenReturn(false);
        when(cqlEngineClient.evaluateCql(eq(tenantId), anyString(), eq(patientId), isNull()))
            .thenReturn("{\"triggered\":true}");
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        // When
        CdsEvaluateResponse response = service.evaluateRules(tenantId, request);

        // Then
        // Only the rule from the correct tenant should be evaluated
        assertThat(response.getRulesEvaluated()).isEqualTo(1);
    }

    @Test
    @DisplayName("evaluateRules() - Should include recommendation counts by category")
    void evaluateRules_ShouldIncludeRecommendationCountsByCategory() {
        // Given
        CdsEvaluateRequest request = CdsEvaluateRequest.builder()
            .patientId(patientId)
            .build();

        when(ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId))
            .thenReturn(Collections.singletonList(testRule));
        when(recommendationRepository.existsActiveRecommendation(eq(tenantId), eq(patientId), any()))
            .thenReturn(false);
        when(cqlEngineClient.evaluateCql(eq(tenantId), eq("DiabetesManagement"), eq(patientId), isNull()))
            .thenReturn("{\"triggered\":true}");
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        // When
        CdsEvaluateResponse response = service.evaluateRules(tenantId, request);

        // Then
        assertThat(response.getRecommendationsByCategory()).isNotNull();
        assertThat(response.getRecommendationsByCategory()).containsKey("CHRONIC_DISEASE");
        assertThat(response.getRecommendationsByCategory().get("CHRONIC_DISEASE")).isEqualTo(1);
    }

    @Test
    @DisplayName("evaluateRules() - Should include recommendation counts by urgency")
    void evaluateRules_ShouldIncludeRecommendationCountsByUrgency() {
        // Given
        CdsEvaluateRequest request = CdsEvaluateRequest.builder()
            .patientId(patientId)
            .build();

        when(ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId))
            .thenReturn(Collections.singletonList(testRule));
        when(recommendationRepository.existsActiveRecommendation(eq(tenantId), eq(patientId), any()))
            .thenReturn(false);
        when(cqlEngineClient.evaluateCql(eq(tenantId), eq("DiabetesManagement"), eq(patientId), isNull()))
            .thenReturn("{\"triggered\":true}");
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        // When
        CdsEvaluateResponse response = service.evaluateRules(tenantId, request);

        // Then
        assertThat(response.getRecommendationsByUrgency()).isNotNull();
        assertThat(response.getRecommendationsByUrgency()).containsKey("ROUTINE");
        assertThat(response.getRecommendationsByUrgency().get("ROUTINE")).isEqualTo(1);
    }

    @Test
    @DisplayName("getRecommendationCountsByUrgency() - Should return counts for each urgency")
    void getRecommendationCountsByUrgency_ShouldReturnCounts() {
        for (CdsRuleEntity.CdsUrgency urgency : CdsRuleEntity.CdsUrgency.values()) {
            when(recommendationRepository.countActiveByUrgency(tenantId, patientId, urgency))
                .thenReturn(2L);
        }

        Map<String, Long> counts = service.getRecommendationCountsByUrgency(tenantId, patientId);

        assertThat(counts).hasSize(CdsRuleEntity.CdsUrgency.values().length);
        assertThat(counts.get("ROUTINE")).isEqualTo(2L);
    }

    @Test
    @DisplayName("getActiveRecommendationCount() - Should return active count")
    void getActiveRecommendationCount_ShouldReturnCount() {
        when(recommendationRepository.countActiveRecommendations(tenantId, patientId))
            .thenReturn(5L);

        Long count = service.getActiveRecommendationCount(tenantId, patientId);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("getOverdueRecommendations() - Should map overdue recommendations")
    void getOverdueRecommendations_ShouldMapOverdueRecommendations() {
        when(recommendationRepository.findOverdueRecommendations(eq(tenantId), eq(patientId), any(Instant.class)))
            .thenReturn(List.of(testRecommendation));

        List<CdsRecommendationDTO> overdue = service.getOverdueRecommendations(tenantId, patientId);

        assertThat(overdue).hasSize(1);
        assertThat(overdue.get(0).getId()).isEqualTo(testRecommendation.getId());
    }

    @Test
    @DisplayName("evaluateRules() - Should ignore invalid categories")
    void evaluateRules_ShouldIgnoreInvalidCategories() {
        CdsEvaluateRequest request = CdsEvaluateRequest.builder()
            .patientId(patientId)
            .categories(List.of("invalid", "chronic_disease"))
            .build();

        when(ruleRepository.findByTenantIdAndCategoryAndActiveTrueOrderByPriorityAsc(
            tenantId, CdsRuleEntity.CdsCategory.CHRONIC_DISEASE))
            .thenReturn(List.of(testRule));
        when(recommendationRepository.existsActiveRecommendation(eq(tenantId), eq(patientId), any()))
            .thenReturn(false);
        when(cqlEngineClient.evaluateCql(eq(tenantId), eq("DiabetesManagement"), eq(patientId), isNull()))
            .thenReturn("{\"triggered\":true}");
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        CdsEvaluateResponse response = service.evaluateRules(tenantId, request);

        assertThat(response.getRulesEvaluated()).isEqualTo(1);
    }

    @Test
    @DisplayName("evaluateRules() - Should filter inactive or wrong-tenant rules by ID")
    void evaluateRules_ShouldFilterInactiveOrWrongTenantRules() {
        CdsRuleEntity inactiveRule = CdsRuleEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .ruleCode("RULE-INACTIVE")
            .ruleName("Inactive Rule")
            .category(CdsRuleEntity.CdsCategory.PREVENTIVE)
            .priority(2)
            .active(false)
            .defaultUrgency(CdsRuleEntity.CdsUrgency.ROUTINE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        CdsRuleEntity otherTenantRule = CdsRuleEntity.builder()
            .id(UUID.randomUUID())
            .tenantId("other-tenant")
            .ruleCode("RULE-OTHER")
            .ruleName("Other Tenant Rule")
            .category(CdsRuleEntity.CdsCategory.PREVENTIVE)
            .priority(3)
            .active(true)
            .defaultUrgency(CdsRuleEntity.CdsUrgency.ROUTINE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        CdsEvaluateRequest request = CdsEvaluateRequest.builder()
            .patientId(patientId)
            .ruleIds(List.of(testRule.getId(), inactiveRule.getId(), otherTenantRule.getId()))
            .build();

        when(ruleRepository.findAllById(any()))
            .thenReturn(List.of(testRule, inactiveRule, otherTenantRule));
        when(recommendationRepository.existsActiveRecommendation(eq(tenantId), eq(patientId), any()))
            .thenReturn(false);
        when(cqlEngineClient.evaluateCql(eq(tenantId), eq("DiabetesManagement"), eq(patientId), isNull()))
            .thenReturn("{\"triggered\":true}");
        when(recommendationRepository.save(any(CdsRecommendationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.findActiveRecommendations(tenantId, patientId))
            .thenReturn(Collections.emptyList());

        CdsEvaluateResponse response = service.evaluateRules(tenantId, request);

        assertThat(response.getRulesEvaluated()).isEqualTo(1);
    }
}
