package com.healthdata.hcc.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent.AgentType;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent.DecisionType;
import com.healthdata.audit.service.ai.AIAuditEventPublisher;
import com.healthdata.hcc.persistence.DocumentationGapEntity;
import com.healthdata.hcc.service.RafCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("HCC Audit Integration - Lightweight Tests")
class HccAuditIntegrationTest {

    @Mock
    private AIAuditEventPublisher auditEventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    private HccAuditIntegration auditIntegration;

    private static final String TENANT_ID = "tenant-123";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final String USER_ID = "user-456";

    @BeforeEach
    void setUp() {
        auditIntegration = new HccAuditIntegration(auditEventPublisher, objectMapper);
        
        // Enable audit
        ReflectionTestUtils.setField(auditIntegration, "auditEnabled", true);
        
        when(auditEventPublisher.publishAIDecision(any(AIAgentDecisionEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    @DisplayName("Should publish RAF calculation event with correct fields")
    void shouldPublishRafCalculationEvent() {
        // Given
        RafCalculationService.RafCalculationResult result = createRafCalculationResult();
        long inferenceTimeMs = 250L;

        // When
        auditIntegration.publishRafCalculationEvent(
                TENANT_ID, PATIENT_ID, result, inferenceTimeMs, USER_ID);

        // Then
        ArgumentCaptor<AIAgentDecisionEvent> eventCaptor = ArgumentCaptor.forClass(AIAgentDecisionEvent.class);
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());

        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getAgentId()).isEqualTo("hcc-coding");
        assertThat(event.getAgentType()).isEqualTo(AgentType.PREDICTIVE_ANALYTICS);
        assertThat(event.getDecisionType()).isEqualTo(DecisionType.RAF_CALCULATION);
        assertThat(event.getResourceType()).isEqualTo("Patient");
        assertThat(event.getResourceId()).isEqualTo(PATIENT_ID.toString());
        assertThat(event.getInferenceTimeMs()).isEqualTo(inferenceTimeMs);

        // Verify metrics
        Map<String, Object> inputMetrics = event.getInputMetrics();
        assertThat(inputMetrics.get("diagnosisCount")).isEqualTo(5);
        assertThat(inputMetrics.get("profileYear")).isEqualTo(2025);
        assertThat(inputMetrics.get("rafScoreV24")).isEqualTo(BigDecimal.valueOf(1.150));
        assertThat(inputMetrics.get("rafScoreV28")).isEqualTo(BigDecimal.valueOf(1.200));
        assertThat(inputMetrics.get("rafScoreBlended")).isEqualTo(BigDecimal.valueOf(1.1835));
        assertThat(inputMetrics.get("hccCountV24")).isEqualTo(3);
        assertThat(inputMetrics.get("hccCountV28")).isEqualTo(4);
    }

    @Test
    @DisplayName("Should publish HCC coding event")
    void shouldPublishHccCodingEvent() {
        // Given
        String icd10Code = "E11.65";
        String hccCodeV24 = "HCC18";
        String hccCodeV28 = "HCC19";
        Map<String, Object> codeMetadata = new HashMap<>();
        codeMetadata.put("description", "Type 2 diabetes with hyperglycemia");
        codeMetadata.put("category", "Endocrine");
        long processingTimeMs = 50L;

        // When
        auditIntegration.publishHccCodingEvent(
                TENANT_ID, PATIENT_ID, icd10Code, hccCodeV24, hccCodeV28, 
                codeMetadata, processingTimeMs, USER_ID);

        // Then
        ArgumentCaptor<AIAgentDecisionEvent> eventCaptor = ArgumentCaptor.forClass(AIAgentDecisionEvent.class);
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());

        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getAgentType()).isEqualTo(AgentType.CQL_ENGINE);
        assertThat(event.getDecisionType()).isEqualTo(DecisionType.HCC_CODING);
        assertThat(event.getResourceType()).isEqualTo("DiagnosisCode");
        assertThat(event.getResourceId()).isEqualTo(icd10Code);

        // Verify metrics
        Map<String, Object> inputMetrics = event.getInputMetrics();
        assertThat(inputMetrics.get("icd10Code")).isEqualTo(icd10Code);
        assertThat(inputMetrics.get("hccCodeV24")).isEqualTo(hccCodeV24);
        assertThat(inputMetrics.get("hccCodeV28")).isEqualTo(hccCodeV28);
        assertThat(inputMetrics.get("description")).isEqualTo("Type 2 diabetes with hyperglycemia");
    }

    @Test
    @DisplayName("Should publish documentation gap event")
    void shouldPublishDocumentationGapEvent() {
        // Given
        List<DocumentationGapEntity> gaps = createDocumentationGaps();
        int totalGaps = gaps.size();
        BigDecimal potentialUplift = BigDecimal.valueOf(0.350);
        long analysisTimeMs = 500L;

        // When
        auditIntegration.publishDocumentationGapEvent(
                TENANT_ID, PATIENT_ID, gaps, totalGaps, potentialUplift, 
                analysisTimeMs, USER_ID);

        // Then
        ArgumentCaptor<AIAgentDecisionEvent> eventCaptor = ArgumentCaptor.forClass(AIAgentDecisionEvent.class);
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());

        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getAgentType()).isEqualTo(AgentType.CARE_GAP_IDENTIFIER);
        assertThat(event.getDecisionType()).isEqualTo(DecisionType.CARE_GAP_IDENTIFICATION);
        assertThat(event.getResourceId()).isEqualTo(PATIENT_ID.toString());

        // Verify metrics
        Map<String, Object> inputMetrics = event.getInputMetrics();
        assertThat(inputMetrics.get("totalGaps")).isEqualTo(totalGaps);
        assertThat(inputMetrics.get("potentialRafUplift")).isEqualTo(potentialUplift);
        assertThat(inputMetrics.get("highPriorityGapCount")).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should publish gap addressed event")
    void shouldPublishGapAddressedEvent() {
        // Given
        DocumentationGapEntity gap = createDocumentationGap("E11.9", "HCC19", "HCC19", 
                DocumentationGapEntity.GapType.UNSPECIFIED, "HIGH");
        String addressedBy = "dr.jones@example.com";
        String newIcd10Code = "E11.65";

        // When
        auditIntegration.publishGapAddressedEvent(
                TENANT_ID, PATIENT_ID, gap, addressedBy, newIcd10Code);

        // Then
        ArgumentCaptor<AIAgentDecisionEvent> eventCaptor = ArgumentCaptor.forClass(AIAgentDecisionEvent.class);
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());

        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getAgentType()).isEqualTo(AgentType.CARE_GAP_IDENTIFIER);
        assertThat(event.getDecisionType()).isEqualTo(DecisionType.CARE_GAP_CLOSURE);
        assertThat(event.getResourceType()).isEqualTo("DocumentationGap");
        assertThat(event.getResourceId()).isEqualTo(gap.getId().toString());

        // Verify metrics
        Map<String, Object> inputMetrics = event.getInputMetrics();
        assertThat(inputMetrics.get("originalIcd10")).isEqualTo("E11.9");
        assertThat(inputMetrics.get("newIcd10Code")).isEqualTo(newIcd10Code);
        assertThat(inputMetrics.get("addressedBy")).isEqualTo(addressedBy);
        assertThat(inputMetrics.get("priority")).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Should not publish event when audit is disabled")
    void shouldNotPublishEventWhenAuditIsDisabled() {
        // Given
        ReflectionTestUtils.setField(auditIntegration, "auditEnabled", false);
        RafCalculationService.RafCalculationResult result = createRafCalculationResult();

        // When
        auditIntegration.publishRafCalculationEvent(
                TENANT_ID, PATIENT_ID, result, 100L, USER_ID);

        // Then
        verify(auditEventPublisher, never()).publishAIDecision(any());
    }

    @Test
    @DisplayName("Should not throw exception on audit failure")
    void shouldNotThrowExceptionOnAuditFailure() {
        // Given
        RafCalculationService.RafCalculationResult result = createRafCalculationResult();
        when(auditEventPublisher.publishAIDecision(any(AIAgentDecisionEvent.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka unavailable")));

        // When
        auditIntegration.publishRafCalculationEvent(
                TENANT_ID, PATIENT_ID, result, 100L, USER_ID);

        // Then - No exception should propagate
        verify(auditEventPublisher).publishAIDecision(any());
    }

    @Test
    @DisplayName("Should handle null HCC codes in coding event")
    void shouldHandleNullHccCodesInCodingEvent() {
        // Given
        String icd10Code = "Z99.89";
        String hccCodeV24 = null;  // Code doesn't map to HCC in V24
        String hccCodeV28 = null;  // Code doesn't map to HCC in V28

        // When
        auditIntegration.publishHccCodingEvent(
                TENANT_ID, PATIENT_ID, icd10Code, hccCodeV24, hccCodeV28, 
                null, 50L, USER_ID);

        // Then - Should not throw exception
        ArgumentCaptor<AIAgentDecisionEvent> eventCaptor = ArgumentCaptor.forClass(AIAgentDecisionEvent.class);
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());
        
        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getInputMetrics().get("hccCodeV24")).isNull();
        assertThat(event.getInputMetrics().get("hccCodeV28")).isNull();
    }

    @Test
    @DisplayName("Should include gap breakdown by type and priority")
    void shouldIncludeGapBreakdownByTypeAndPriority() {
        // Given
        List<DocumentationGapEntity> gaps = createMixedDocumentationGaps();
        int totalGaps = gaps.size();
        BigDecimal potentialUplift = BigDecimal.valueOf(0.450);

        // When
        auditIntegration.publishDocumentationGapEvent(
                TENANT_ID, PATIENT_ID, gaps, totalGaps, potentialUplift, 
                500L, USER_ID);

        // Then
        ArgumentCaptor<AIAgentDecisionEvent> eventCaptor = ArgumentCaptor.forClass(AIAgentDecisionEvent.class);
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());

        AIAgentDecisionEvent event = eventCaptor.getValue();
        Map<String, Object> inputMetrics = event.getInputMetrics();
        
        @SuppressWarnings("unchecked")
        Map<String, Long> gapsByType = (Map<String, Long>) inputMetrics.get("gapsByType");
        assertThat(gapsByType).containsKey("UNSPECIFIED");
        assertThat(gapsByType).containsKey("V28_SPECIFICITY");
        
        @SuppressWarnings("unchecked")
        Map<String, Long> gapsByPriority = (Map<String, Long>) inputMetrics.get("gapsByPriority");
        assertThat(gapsByPriority).containsKey("HIGH");
        assertThat(gapsByPriority).containsKey("MEDIUM");
    }

    // Helper methods

    private RafCalculationService.RafCalculationResult createRafCalculationResult() {
        return RafCalculationService.RafCalculationResult.builder()
                .patientId(PATIENT_ID)
                .profileYear(2025)
                .rafScoreV24(BigDecimal.valueOf(1.150))
                .rafScoreV28(BigDecimal.valueOf(1.200))
                .rafScoreBlended(BigDecimal.valueOf(1.1835))
                .hccsV24(Arrays.asList("HCC18", "HCC85", "HCC108"))
                .hccsV28(Arrays.asList("HCC19", "HCC86", "HCC108", "HCC23"))
                .diagnosisCount(5)
                .hccCountV24(3)
                .hccCountV28(4)
                .v24Weight(0.33)
                .v28Weight(0.67)
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    private List<DocumentationGapEntity> createDocumentationGaps() {
        List<DocumentationGapEntity> gaps = new ArrayList<>();
        gaps.add(createDocumentationGap("E11.9", "HCC19", "HCC19", 
                DocumentationGapEntity.GapType.UNSPECIFIED, "HIGH"));
        gaps.add(createDocumentationGap("I50.9", "HCC85", "HCC86", 
                DocumentationGapEntity.GapType.UNSPECIFIED, "MEDIUM"));
        gaps.add(createDocumentationGap("N18.9", "HCC136", "HCC328", 
                DocumentationGapEntity.GapType.UNSPECIFIED, "MEDIUM"));
        return gaps;
    }

    private List<DocumentationGapEntity> createMixedDocumentationGaps() {
        List<DocumentationGapEntity> gaps = new ArrayList<>();
        gaps.add(createDocumentationGap("E11.9", "HCC19", "HCC19", 
                DocumentationGapEntity.GapType.UNSPECIFIED, "HIGH"));
        gaps.add(createDocumentationGap("I50.9", "HCC85", null, 
                DocumentationGapEntity.GapType.V28_SPECIFICITY, "HIGH"));
        gaps.add(createDocumentationGap("N18.9", "HCC136", "HCC328", 
                DocumentationGapEntity.GapType.UNSPECIFIED, "MEDIUM"));
        return gaps;
    }

    private DocumentationGapEntity createDocumentationGap(
            String icd10Code, String hccV24, String hccV28,
            DocumentationGapEntity.GapType gapType, String priority) {
        
        DocumentationGapEntity gap = new DocumentationGapEntity();
        gap.setId(UUID.randomUUID());
        gap.setTenantId(TENANT_ID);
        gap.setPatientId(PATIENT_ID);
        gap.setProfileYear(2025);
        gap.setCurrentIcd10(icd10Code);
        gap.setCurrentHccV24(hccV24);
        gap.setCurrentHccV28(hccV28);
        gap.setGapType(gapType);
        gap.setPriority(priority);
        gap.setRafImpactV24(BigDecimal.valueOf(0.120));
        gap.setRafImpactV28(BigDecimal.valueOf(0.115));
        gap.setRafImpactBlended(BigDecimal.valueOf(0.117));
        gap.setStatus(DocumentationGapEntity.GapStatus.OPEN);
        
        return gap;
    }
}
