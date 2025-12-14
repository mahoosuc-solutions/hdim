package com.healthdata.agent.approval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agent.approval.ApprovalIntegration.ApprovalResult;
import com.healthdata.agent.client.ApprovalServiceClient;
import com.healthdata.agent.client.ApprovalServiceClient.*;
import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.tool.ToolDefinition;
import com.healthdata.agent.tool.ToolDefinition.ApprovalCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalIntegration Tests")
class ApprovalIntegrationTest {

    @Mock
    private ApprovalServiceClient approvalServiceClient;

    @Captor
    private ArgumentCaptor<CreateApprovalRequest> requestCaptor;

    private ApprovalIntegration approvalIntegration;
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String PATIENT_ID = "patient-789";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        approvalIntegration = new ApprovalIntegration(approvalServiceClient, objectMapper);
        ReflectionTestUtils.setField(approvalIntegration, "defaultTimeoutHours", 24);
        ReflectionTestUtils.setField(approvalIntegration, "approvalEnabled", true);
    }

    private AgentContext createContext() {
        return AgentContext.builder()
            .tenantId(TENANT_ID)
            .userId(USER_ID)
            .patientId(PATIENT_ID)
            .sessionId("session-001")
            .correlationId("corr-001")
            .agentType("clinical-assistant")
            .roles(Set.of("CLINICAL_USER"))
            .build();
    }

    private ToolDefinition createToolDefinition(ApprovalCategory category, String role) {
        return ToolDefinition.builder()
            .name("MedicationTool")
            .description("Manages medication recommendations")
            .approvalCategory(category)
            .requiredApprovalRole(role)
            .build();
    }

    @Nested
    @DisplayName("Tool Approval Requests")
    class ToolApprovalTests {

        @Test
        @DisplayName("should not require approval when disabled")
        void noApprovalWhenDisabled() throws Exception {
            // Given
            ReflectionTestUtils.setField(approvalIntegration, "approvalEnabled", false);
            ToolDefinition tool = createToolDefinition(ApprovalCategory.CLINICAL, "CLINICAL_REVIEWER");
            JsonNode arguments = objectMapper.readTree("{\"action\": \"prescribe\"}");
            AgentContext context = createContext();

            // When
            ApprovalResult result = approvalIntegration.checkAndCreateApprovalRequest(tool, arguments, context);

            // Then
            assertThat(result.required()).isFalse();
            assertThat(result.canProceed()).isTrue();
            verifyNoInteractions(approvalServiceClient);
        }

        @Test
        @DisplayName("should not require approval for NONE category")
        void noApprovalForNoneCategory() throws Exception {
            // Given
            ToolDefinition tool = createToolDefinition(ApprovalCategory.NONE, null);
            JsonNode arguments = objectMapper.readTree("{\"action\": \"view\"}");
            AgentContext context = createContext();

            // When
            ApprovalResult result = approvalIntegration.checkAndCreateApprovalRequest(tool, arguments, context);

            // Then
            assertThat(result.required()).isFalse();
            assertThat(result.canProceed()).isTrue();
            verifyNoInteractions(approvalServiceClient);
        }

        @Test
        @DisplayName("should create approval request for CLINICAL category")
        void createApprovalForClinical() throws Exception {
            // Given
            UUID approvalId = UUID.randomUUID();
            ToolDefinition tool = createToolDefinition(ApprovalCategory.CLINICAL, "CLINICAL_REVIEWER");
            JsonNode arguments = objectMapper.readTree("{\"medication\": \"aspirin\", \"dose\": \"100mg\"}");
            AgentContext context = createContext();

            ApprovalResponse response = ApprovalResponse.builder()
                .id(approvalId)
                .status(ApprovalStatus.PENDING)
                .fallback(false)
                .build();

            when(approvalServiceClient.createApprovalRequest(eq(TENANT_ID), eq(USER_ID), any()))
                .thenReturn(response);

            // When
            ApprovalResult result = approvalIntegration.checkAndCreateApprovalRequest(tool, arguments, context);

            // Then
            assertThat(result.required()).isTrue();
            assertThat(result.approved()).isFalse();
            assertThat(result.blocked()).isFalse();
            assertThat(result.approvalId()).isEqualTo(approvalId);
            assertThat(result.status()).isEqualTo("PENDING");
            assertThat(result.canProceed()).isFalse();

            verify(approvalServiceClient).createApprovalRequest(eq(TENANT_ID), eq(USER_ID), requestCaptor.capture());
            CreateApprovalRequest captured = requestCaptor.getValue();
            assertThat(captured.requestType()).isEqualTo(RequestType.AGENT_ACTION);
            assertThat(captured.entityType()).isEqualTo("MedicationTool");
            assertThat(captured.riskLevel()).isEqualTo(RiskLevel.HIGH);
            assertThat(captured.assignedRole()).isEqualTo("CLINICAL_REVIEWER");
        }

        @Test
        @DisplayName("should map CRITICAL category to CRITICAL risk level")
        void mapCriticalRiskLevel() throws Exception {
            // Given
            UUID approvalId = UUID.randomUUID();
            ToolDefinition tool = createToolDefinition(ApprovalCategory.CRITICAL, "CLINICAL_DIRECTOR");
            JsonNode arguments = objectMapper.readTree("{\"emergency\": true}");
            AgentContext context = createContext();

            ApprovalResponse response = ApprovalResponse.builder()
                .id(approvalId)
                .status(ApprovalStatus.PENDING)
                .fallback(false)
                .build();

            when(approvalServiceClient.createApprovalRequest(eq(TENANT_ID), eq(USER_ID), any()))
                .thenReturn(response);

            // When
            approvalIntegration.checkAndCreateApprovalRequest(tool, arguments, context);

            // Then
            verify(approvalServiceClient).createApprovalRequest(eq(TENANT_ID), eq(USER_ID), requestCaptor.capture());
            assertThat(requestCaptor.getValue().riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        }

        @Test
        @DisplayName("should block execution when approval service returns fallback")
        void blockOnFallback() throws Exception {
            // Given
            ToolDefinition tool = createToolDefinition(ApprovalCategory.CLINICAL, "CLINICAL_REVIEWER");
            JsonNode arguments = objectMapper.readTree("{\"action\": \"prescribe\"}");
            AgentContext context = createContext();

            ApprovalResponse fallbackResponse = ApprovalResponse.builder()
                .id(null)
                .status(ApprovalStatus.PENDING)
                .fallback(true)
                .fallbackReason("Service unavailable")
                .build();

            when(approvalServiceClient.createApprovalRequest(eq(TENANT_ID), eq(USER_ID), any()))
                .thenReturn(fallbackResponse);

            // When
            ApprovalResult result = approvalIntegration.checkAndCreateApprovalRequest(tool, arguments, context);

            // Then
            assertThat(result.blocked()).isTrue();
            assertThat(result.canProceed()).isFalse();
            assertThat(result.message()).contains("Approval service unavailable");
        }

        @Test
        @DisplayName("should block execution when exception occurs")
        void blockOnException() throws Exception {
            // Given
            ToolDefinition tool = createToolDefinition(ApprovalCategory.CLINICAL, "CLINICAL_REVIEWER");
            JsonNode arguments = objectMapper.readTree("{\"action\": \"prescribe\"}");
            AgentContext context = createContext();

            when(approvalServiceClient.createApprovalRequest(eq(TENANT_ID), eq(USER_ID), any()))
                .thenThrow(new RuntimeException("Connection refused"));

            // When
            ApprovalResult result = approvalIntegration.checkAndCreateApprovalRequest(tool, arguments, context);

            // Then
            assertThat(result.blocked()).isTrue();
            assertThat(result.canProceed()).isFalse();
            assertThat(result.message()).contains("Connection refused");
        }
    }

    @Nested
    @DisplayName("Guardrail Approval Requests")
    class GuardrailApprovalTests {

        @Test
        @DisplayName("should create guardrail approval request")
        void createGuardrailApprovalRequest() {
            // Given
            UUID approvalId = UUID.randomUUID();
            String content = "You should take 500mg of aspirin daily";
            String violations = "HIGH: OTC medication recommendation";
            AgentContext context = createContext();

            ApprovalResponse response = ApprovalResponse.builder()
                .id(approvalId)
                .status(ApprovalStatus.PENDING)
                .fallback(false)
                .build();

            when(approvalServiceClient.createApprovalRequest(eq(TENANT_ID), eq(USER_ID), any()))
                .thenReturn(response);

            // When
            ApprovalResult result = approvalIntegration.createGuardrailApprovalRequest(content, violations, context);

            // Then
            assertThat(result.required()).isTrue();
            assertThat(result.approvalId()).isEqualTo(approvalId);
            assertThat(result.status()).isEqualTo("PENDING");

            verify(approvalServiceClient).createApprovalRequest(eq(TENANT_ID), eq(USER_ID), requestCaptor.capture());
            CreateApprovalRequest captured = requestCaptor.getValue();
            assertThat(captured.requestType()).isEqualTo(RequestType.GUARDRAIL_REVIEW);
            assertThat(captured.entityType()).isEqualTo("AI_RESPONSE");
            assertThat(captured.actionRequested()).isEqualTo("DELIVER_TO_USER");
            assertThat(captured.riskLevel()).isEqualTo(RiskLevel.HIGH);
            assertThat(captured.assignedRole()).isEqualTo("CLINICAL_REVIEWER");
            assertThat(captured.payload()).containsKey("content");
            assertThat(captured.payload()).containsKey("violations");
        }

        @Test
        @DisplayName("should not create guardrail request when disabled")
        void noGuardrailRequestWhenDisabled() {
            // Given
            ReflectionTestUtils.setField(approvalIntegration, "approvalEnabled", false);
            AgentContext context = createContext();

            // When
            ApprovalResult result = approvalIntegration.createGuardrailApprovalRequest(
                "Some content", "Some violations", context);

            // Then
            assertThat(result.required()).isFalse();
            verifyNoInteractions(approvalServiceClient);
        }

        @Test
        @DisplayName("should block guardrail content on service fallback")
        void blockGuardrailOnFallback() {
            // Given
            AgentContext context = createContext();

            ApprovalResponse fallbackResponse = ApprovalResponse.builder()
                .fallback(true)
                .fallbackReason("Service down")
                .build();

            when(approvalServiceClient.createApprovalRequest(eq(TENANT_ID), eq(USER_ID), any()))
                .thenReturn(fallbackResponse);

            // When
            ApprovalResult result = approvalIntegration.createGuardrailApprovalRequest(
                "Flagged content", "Violations", context);

            // Then
            assertThat(result.blocked()).isTrue();
            assertThat(result.message()).contains("Flagged content blocked");
        }
    }

    @Nested
    @DisplayName("Approval Status Check")
    class StatusCheckTests {

        @Test
        @DisplayName("should return approval status")
        void checkApprovalStatus() {
            // Given
            UUID approvalId = UUID.randomUUID();
            ApprovalResponse response = ApprovalResponse.builder()
                .id(approvalId)
                .status(ApprovalStatus.APPROVED)
                .build();

            when(approvalServiceClient.getApprovalRequest(TENANT_ID, approvalId))
                .thenReturn(response);

            // When
            ApprovalStatus status = approvalIntegration.checkApprovalStatus(approvalId, TENANT_ID);

            // Then
            assertThat(status).isEqualTo(ApprovalStatus.APPROVED);
        }

        @Test
        @DisplayName("should return PENDING on error")
        void returnPendingOnError() {
            // Given
            UUID approvalId = UUID.randomUUID();
            when(approvalServiceClient.getApprovalRequest(TENANT_ID, approvalId))
                .thenThrow(new RuntimeException("Service error"));

            // When
            ApprovalStatus status = approvalIntegration.checkApprovalStatus(approvalId, TENANT_ID);

            // Then
            assertThat(status).isEqualTo(ApprovalStatus.PENDING);
        }
    }
}
