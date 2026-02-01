package com.healthdata.agent.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.core.AgentOrchestrator.AgentResponse;
import com.healthdata.agent.llm.model.LLMRequest;
import com.healthdata.agent.llm.model.LLMResponse;
import com.healthdata.agent.tool.ToolDefinition;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent.AgentType;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent.DecisionType;
import com.healthdata.audit.service.ai.AIAuditEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Lightweight unit tests for AgentRuntimeAuditIntegration.
 * Uses mocked dependencies to test audit event publishing logic.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("Agent Runtime Audit Integration - Lightweight Tests")
class AgentRuntimeAuditIntegrationTest {

    @Mock
    private AIAuditEventPublisher auditEventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AgentRuntimeAuditIntegration auditIntegration;

    @Captor
    private ArgumentCaptor<AIAgentDecisionEvent> eventCaptor;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String PATIENT_ID = "patient-789";
    private static final String SESSION_ID = "session-001";
    private static final String CORRELATION_ID = "corr-001";
    private static final String AGENT_TYPE_STR = "clinical-assistant";

    private AgentContext context;
    private ObjectMapper realObjectMapper;

    @BeforeEach
    void setUp() {
        realObjectMapper = new ObjectMapper();

        context = AgentContext.builder()
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .patientId(PATIENT_ID)
                .sessionId(SESSION_ID)
                .correlationId(CORRELATION_ID)
                .agentType(AGENT_TYPE_STR)
                .build();

        // Enable audit
        ReflectionTestUtils.setField(auditIntegration, "auditEnabled", true);

        // Mock ObjectMapper to return real nodes
        when(objectMapper.createObjectNode()).thenAnswer(inv -> realObjectMapper.createObjectNode());
        when(objectMapper.valueToTree(any())).thenAnswer(inv -> realObjectMapper.valueToTree(inv.getArgument(0)));
        when(objectMapper.convertValue(any(), eq(Map.class))).thenAnswer(inv -> 
                realObjectMapper.convertValue(inv.getArgument(0), Map.class));
    }

    @Test
    @DisplayName("Should publish agent execution event with correct fields")
    void shouldPublishAgentExecutionEvent() {
        // Given
        String userMessage = "What are my care gaps?";
        LLMResponse.TokenUsage usage = LLMResponse.TokenUsage.builder()
                .inputTokens(100)
                .outputTokens(200)
                .totalTokens(300)
                .build();
        AgentResponse response = AgentResponse.success(
                "Based on your records, you have 2 care gaps...",
                usage,
                "claude-3-5-sonnet-20241022"
        );
        Long executionTimeMs = 1500L;

        // When
        auditIntegration.publishAgentExecutionEvent(
                context, userMessage, response, USER_ID, executionTimeMs);

        // Then
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());

        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getAgentId()).isEqualTo("ai-agent-runtime");
        assertThat(event.getAgentType()).isEqualTo(AIAgentDecisionEvent.AgentType.AI_AGENT);
        assertThat(event.getDecisionType()).isEqualTo(AIAgentDecisionEvent.DecisionType.AI_RECOMMENDATION);
        assertThat(event.getResourceId()).isEqualTo(PATIENT_ID);
        assertThat(event.getCorrelationId()).isEqualTo(CORRELATION_ID);
        assertThat(event.getResourceType()).isEqualTo("AgentExecution");
        assertThat(event.getConfidenceScore()).isEqualTo(0.9);

        // Verify metrics contains expected fields
        Map<String, Object> metrics = event.getInputMetrics();
        assertThat(metrics.get("message")).isEqualTo(userMessage);
        assertThat(metrics.get("agentType")).isEqualTo(AGENT_TYPE_STR);
        assertThat(metrics.get("sessionId")).isEqualTo(SESSION_ID);
        assertThat(metrics.get("patientId")).isEqualTo(PATIENT_ID);
        assertThat(metrics.get("success")).isEqualTo(true);
        assertThat(metrics.get("blocked")).isEqualTo(false);
        assertThat(metrics.get("model")).isEqualTo("claude-3-5-sonnet-20241022");
        assertThat(metrics.get("executionTimeMs")).isEqualTo(1500L);

        @SuppressWarnings("unchecked")
        Map<String, Object> usageMap = (Map<String, Object>) metrics.get("usage");
        assertThat(usageMap.get("inputTokens")).isEqualTo(100);
        assertThat(usageMap.get("outputTokens")).isEqualTo(200);
        assertThat(usageMap.get("totalTokens")).isEqualTo(300);
    }

    @Test
    @DisplayName("Should publish guardrail block event when response is blocked")
    void shouldPublishGuardrailBlockEvent() {
        // Given
        String userMessage = "Prescribe me medication for diabetes";
        AgentResponse response = AgentResponse.blocked("Prescription requests require human approval");
        Long executionTimeMs = 500L;

        // When
        auditIntegration.publishAgentExecutionEvent(
                context, userMessage, response, USER_ID, executionTimeMs);

        // Then
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());

        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getDecisionType()).isEqualTo(AIAgentDecisionEvent.DecisionType.GUARDRAIL_BLOCK);
    }

    @Test
    @DisplayName("Should publish tool execution event")
    void shouldPublishToolExecutionEvent() {
        // Given
        LLMRequest.ToolCall toolCall = LLMRequest.ToolCall.builder()
                .id("call-123")
                .name("get_patient_vitals")
                .arguments(Map.of("patientId", PATIENT_ID))
                .build();

        ToolDefinition toolDefinition = ToolDefinition.builder()
                .name("get_patient_vitals")
                .description("Get patient vital signs")
                .category(ToolDefinition.ToolCategory.FHIR_QUERY)
                .requiresApproval(false)
                .build();

        ObjectNode toolResult = realObjectMapper.createObjectNode();
        toolResult.put("bloodPressure", "120/80");
        toolResult.put("heartRate", 72);

        // When
        auditIntegration.publishToolExecutionEvent(
                context, toolCall, toolDefinition, toolResult, USER_ID);

        // Then
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());

        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getAgentId()).isEqualTo("ai-agent-runtime");
        assertThat(event.getAgentType()).isEqualTo(AIAgentDecisionEvent.AgentType.AI_AGENT);
        assertThat(event.getDecisionType()).isEqualTo(AIAgentDecisionEvent.DecisionType.TOOL_EXECUTION);
        assertThat(event.getCorrelationId()).isEqualTo(CORRELATION_ID);

        // Verify metrics
        Map<String, Object> metrics = event.getInputMetrics();
        assertThat(metrics.get("toolName")).isEqualTo("get_patient_vitals");
        assertThat(metrics.get("toolCallId")).isEqualTo("call-123");
        assertThat(metrics.get("toolCategory")).isEqualTo("FHIR_QUERY");
        assertThat(metrics.get("requiresApproval")).isEqualTo(false);

        @SuppressWarnings("unchecked")
        Map<String, Object> capturedToolResult = (Map<String, Object>) metrics.get("toolResult");
        assertThat(capturedToolResult.get("bloodPressure")).isEqualTo("120/80");
        assertThat(capturedToolResult.get("heartRate")).isEqualTo(72);
    }

    @Test
    @DisplayName("Should publish explicit guardrail block event")
    void shouldPublishExplicitGuardrailBlockEvent() {
        // Given
        String blockedContent = "You have cancer and need immediate chemotherapy.";
        String blockReason = "Definitive diagnosis blocked by clinical safety guardrails";
        List<String> violations = List.of(
                "CRITICAL: Definitive cancer diagnosis",
                "HIGH: Treatment recommendation without physician review"
        );

        // When
        auditIntegration.publishGuardrailBlockEvent(
                context, blockedContent, blockReason, violations, USER_ID);

        // Then
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());

        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getDecisionType()).isEqualTo(AIAgentDecisionEvent.DecisionType.GUARDRAIL_BLOCK);
        assertThat(event.getReasoning()).isEqualTo(blockReason);

        // Verify metrics
        Map<String, Object> metrics = event.getInputMetrics();
        assertThat(metrics.get("blockedContent")).isEqualTo(blockedContent);
        assertThat(metrics.get("agentType")).isEqualTo(AGENT_TYPE_STR);
        assertThat(metrics.get("blocked")).isEqualTo(true);
        assertThat(metrics.get("blockReason")).isEqualTo(blockReason);

        @SuppressWarnings("unchecked")
        List<String> capturedViolations = (List<String>) metrics.get("violations");
        assertThat(capturedViolations).hasSize(2);
        assertThat(capturedViolations).containsExactlyElementsOf(violations);
    }

    @Test
    @DisplayName("Should publish PHI access event")
    void shouldPublishPhiAccessEvent() {
        // Given
        String resourceType = "Observation";
        String resourceId = "obs-123";
        String accessPurpose = "Clinical decision support";

        // When
        auditIntegration.publishPhiAccessEvent(
                context, resourceType, resourceId, accessPurpose, USER_ID);

        // Then
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());

        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getDecisionType()).isEqualTo(AIAgentDecisionEvent.DecisionType.PHI_ACCESS);
        assertThat(event.getResourceId()).isEqualTo(resourceId);
        assertThat(event.getResourceType()).isEqualTo(resourceType);

        // Verify metrics
        Map<String, Object> metrics = event.getInputMetrics();
        assertThat(metrics.get("resourceType")).isEqualTo(resourceType);
        assertThat(metrics.get("resourceId")).isEqualTo(resourceId);
        assertThat(metrics.get("accessPurpose")).isEqualTo(accessPurpose);
        assertThat(metrics.get("accessGranted")).isEqualTo(true);
    }

    @Test
    @DisplayName("Should not publish event when audit is disabled")
    void shouldNotPublishEventWhenAuditDisabled() {
        // Given
        ReflectionTestUtils.setField(auditIntegration, "auditEnabled", false);
        AgentResponse response = AgentResponse.success("Test response", null, "claude-3-5-sonnet-20241022");

        // When
        auditIntegration.publishAgentExecutionEvent(
                context, "Test message", response, USER_ID, 1000L);

        // Then
        verify(auditEventPublisher, never()).publishAIDecision(any());
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValuesGracefully() {
        // Given
        AgentContext contextWithoutPatient = context.toBuilder().patientId(null).encounterId(null).build();
        AgentResponse response = AgentResponse.success("Response", null, null);

        // When
        auditIntegration.publishAgentExecutionEvent(
                contextWithoutPatient, "Message", response, USER_ID, null);

        // Then
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());

        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getAgentId()).isEqualTo("ai-agent-runtime");
        assertThat(event.getAgentType()).isEqualTo(AIAgentDecisionEvent.AgentType.AI_AGENT);
        assertThat(event.getResourceId()).isEqualTo("N/A"); // patientId should default to "N/A"
        assertThat(event.getCorrelationId()).isEqualTo(CORRELATION_ID);
    }

    @Test
    @DisplayName("Should not throw exception on audit failure")
    void shouldNotThrowExceptionOnAuditFailure() {
        // Given
        doThrow(new RuntimeException("Kafka unavailable"))
                .when(auditEventPublisher).publishAIDecision(any());

        AgentResponse response = AgentResponse.success("Test response", null, "claude-3-5-sonnet-20241022");

        // When & Then - should not throw
        auditIntegration.publishAgentExecutionEvent(
                context, "Test message", response, USER_ID, 1000L);

        // Verify the event was attempted
        verify(auditEventPublisher).publishAIDecision(any());
    }
}
