package com.healthdata.agent.core;

import com.healthdata.agent.approval.ApprovalIntegration;
import com.healthdata.agent.approval.ApprovalIntegration.ApprovalResult;
import com.healthdata.agent.core.GuardrailService.GuardrailResult;
import com.healthdata.agent.core.GuardrailService.GuardrailViolation;
import com.healthdata.agent.llm.model.LLMResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GuardrailService Tests")
class GuardrailServiceTest {

    @Mock
    private ApprovalIntegration approvalIntegration;

    private GuardrailService guardrailService;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";

    @BeforeEach
    void setUp() {
        guardrailService = new GuardrailService(approvalIntegration);
        ReflectionTestUtils.setField(guardrailService, "guardrailsEnabled", true);
        ReflectionTestUtils.setField(guardrailService, "strictMode", true);
        ReflectionTestUtils.setField(guardrailService, "approvalIntegrationEnabled", true);
    }

    private AgentContext createContext() {
        return AgentContext.builder()
            .tenantId(TENANT_ID)
            .userId(USER_ID)
            .sessionId("session-001")
            .correlationId("corr-001")
            .agentType("clinical-assistant")
            .roles(Set.of("CLINICAL_USER"))
            .build();
    }

    private LLMResponse createResponse(String content) {
        return LLMResponse.builder()
            .content(content)
            .build();
    }

    @Nested
    @DisplayName("Basic Guardrail Checks")
    class BasicChecksTests {

        @Test
        @DisplayName("should allow safe content")
        void allowSafeContent() {
            // Given
            LLMResponse response = createResponse("Here's some general health information about staying hydrated.");
            AgentContext context = createContext();

            // When
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.blocked()).isFalse();
            assertThat(result.flagged()).isFalse();
            assertThat(result.violations()).isEmpty();
            verifyNoInteractions(approvalIntegration);
        }

        @Test
        @DisplayName("should allow when guardrails disabled")
        void allowWhenDisabled() {
            // Given
            ReflectionTestUtils.setField(guardrailService, "guardrailsEnabled", false);
            LLMResponse response = createResponse("You have cancer and should stop taking all medication.");
            AgentContext context = createContext();

            // When
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.blocked()).isFalse();
            assertThat(result.flagged()).isFalse();
            verifyNoInteractions(approvalIntegration);
        }

        @Test
        @DisplayName("should allow empty content")
        void allowEmptyContent() {
            // Given
            LLMResponse response = createResponse("");
            AgentContext context = createContext();

            // When
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.blocked()).isFalse();
            assertThat(result.flagged()).isFalse();
        }

        @Test
        @DisplayName("should allow null content")
        void allowNullContent() {
            // Given
            LLMResponse response = createResponse(null);
            AgentContext context = createContext();

            // When
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.blocked()).isFalse();
            assertThat(result.flagged()).isFalse();
        }
    }

    @Nested
    @DisplayName("Critical Pattern Detection")
    class CriticalPatternTests {

        @Test
        @DisplayName("should block definitive diagnosis of cancer")
        void blockCancerDiagnosis() {
            // Given
            LLMResponse response = createResponse("Based on these symptoms, you have cancer and need immediate treatment.");
            AgentContext context = createContext();

            // When
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.blocked()).isTrue();
            assertThat(result.flagged()).isFalse();
            assertThat(result.reason()).contains("patient safety");
            assertThat(result.violations()).isNotEmpty();
            assertThat(result.violations().get(0).severity()).isEqualTo("CRITICAL");
        }

        @Test
        @DisplayName("should block specific prescription dosage")
        void blockPrescriptionDosage() {
            // Given
            LLMResponse response = createResponse("I recommend you take 500mg of metformin twice daily.");
            AgentContext context = createContext();

            // When
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.blocked()).isTrue();
            assertThat(result.violations()).anyMatch(v ->
                v.description().contains("prescription") || v.description().contains("dosage"));
        }

        @Test
        @DisplayName("should block advice to stop medication")
        void blockStopMedicationAdvice() {
            // Given
            LLMResponse response = createResponse("You should stop taking your blood thinner medication immediately.");
            AgentContext context = createContext();

            // When
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.blocked()).isTrue();
            assertThat(result.violations()).anyMatch(v ->
                v.description().contains("stop medication"));
        }

        @Test
        @DisplayName("should block dismissing emergency symptoms")
        void blockDismissingEmergency() {
            // Given
            LLMResponse response = createResponse("Don't worry about that chest pain, it's fine and not serious.");
            AgentContext context = createContext();

            // When
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.blocked()).isTrue();
            assertThat(result.violations()).anyMatch(v ->
                v.description().contains("emergency"));
        }
    }

    @Nested
    @DisplayName("High Pattern Detection")
    class HighPatternTests {

        @Test
        @DisplayName("should block direct treatment recommendation in strict mode")
        void blockTreatmentRecommendation() {
            // Given
            LLMResponse response = createResponse("You should have surgery to remove the growth immediately.");
            AgentContext context = createContext();

            // When
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.blocked()).isTrue();
            assertThat(result.violations()).anyMatch(v ->
                "HIGH".equals(v.severity()));
        }

        @Test
        @DisplayName("should flag treatment recommendation when strict mode disabled")
        void flagTreatmentWhenNotStrict() {
            // Given
            ReflectionTestUtils.setField(guardrailService, "strictMode", false);
            LLMResponse response = createResponse("You should have surgery to remove the growth.");
            AgentContext context = createContext();

            // Need to mock the approval integration since HIGH pattern will trigger flagging
            UUID approvalId = UUID.randomUUID();
            ApprovalResult approvalResult = ApprovalResult.pending(approvalId, "PENDING");
            when(approvalIntegration.createGuardrailApprovalRequest(anyString(), anyString(), eq(context)))
                .thenReturn(approvalResult);

            // When
            // HIGH patterns are only blocked in strict mode, otherwise flagged
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.blocked()).isFalse();
            assertThat(result.flagged()).isTrue();
        }

        @Test
        @DisplayName("should block definitive test interpretation")
        void blockDefinitiveTestInterpretation() {
            // Given
            LLMResponse response = createResponse("The test results confirm that you have a serious condition.");
            AgentContext context = createContext();

            // When
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.blocked()).isTrue(); // In strict mode
        }
    }

    @Nested
    @DisplayName("Medium Pattern Detection with Approval Integration")
    class MediumPatternWithApprovalTests {

        @Test
        @DisplayName("should flag OTC recommendation and create approval request")
        void flagOTCWithApproval() {
            // Given
            UUID approvalId = UUID.randomUUID();
            LLMResponse response = createResponse("You could try taking ibuprofen for the pain.");
            AgentContext context = createContext();

            ApprovalResult approvalResult = ApprovalResult.pending(approvalId, "PENDING");
            when(approvalIntegration.createGuardrailApprovalRequest(anyString(), anyString(), eq(context)))
                .thenReturn(approvalResult);

            // When
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.blocked()).isFalse();
            assertThat(result.flagged()).isTrue();
            assertThat(result.hasPendingApproval()).isTrue();
            assertThat(result.approvalId()).isEqualTo(approvalId);
            assertThat(result.violations()).anyMatch(v ->
                v.description().contains("OTC"));

            verify(approvalIntegration).createGuardrailApprovalRequest(
                contains("ibuprofen"),
                contains("MEDIUM"),
                eq(context)
            );
        }

        @Test
        @DisplayName("should not create approval when integration disabled")
        void noApprovalWhenDisabled() {
            // Given
            ReflectionTestUtils.setField(guardrailService, "approvalIntegrationEnabled", false);
            LLMResponse response = createResponse("You could try taking tylenol for the headache.");
            AgentContext context = createContext();

            // When
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.flagged()).isTrue();
            assertThat(result.hasPendingApproval()).isFalse();
            assertThat(result.approvalId()).isNull();
            verifyNoInteractions(approvalIntegration);
        }

        @Test
        @DisplayName("should block if approval service blocks the content")
        void blockWhenApprovalServiceBlocks() {
            // Given
            LLMResponse response = createResponse("Use benadryl before sleeping.");
            AgentContext context = createContext();

            ApprovalResult blockedResult = ApprovalResult.blocked("Service unavailable", null);
            when(approvalIntegration.createGuardrailApprovalRequest(anyString(), anyString(), eq(context)))
                .thenReturn(blockedResult);

            // When
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.blocked()).isTrue();
            assertThat(result.flagged()).isFalse();
        }

        @Test
        @DisplayName("should still flag if approval not required")
        void flagWhenApprovalNotRequired() {
            // Given - Use exact word "take" to match pattern \b(take|use|try)\b
            LLMResponse response = createResponse("You should take aspirin for the pain.");
            AgentContext context = createContext();

            ApprovalResult notRequired = ApprovalResult.notRequired();
            when(approvalIntegration.createGuardrailApprovalRequest(anyString(), anyString(), eq(context)))
                .thenReturn(notRequired);

            // When
            GuardrailResult result = guardrailService.check(response, context);

            // Then
            assertThat(result.flagged()).isTrue();
            assertThat(result.blocked()).isFalse();
            assertThat(result.hasPendingApproval()).isFalse();
        }
    }

    @Nested
    @DisplayName("Content Sanitization")
    class SanitizationTests {

        @Test
        @DisplayName("should add disclaimer to sanitized content")
        void addDisclaimer() {
            // Given
            String content = "You might consider taking aspirin.";
            GuardrailViolation violation = new GuardrailViolation("MEDIUM", "OTC recommendation");

            // When
            String sanitized = guardrailService.sanitize(content, java.util.List.of(violation));

            // Then
            assertThat(sanitized).contains("Important Notice");
            assertThat(sanitized).contains("educational purposes");
            assertThat(sanitized).contains("consult your healthcare provider");
            assertThat(sanitized).contains(content);
        }

        @Test
        @DisplayName("should not modify content without violations")
        void noModificationWithoutViolations() {
            // Given
            String content = "Here is some general information.";

            // When
            String sanitized = guardrailService.sanitize(content, java.util.List.of());

            // Then
            assertThat(sanitized).isEqualTo(content);
        }
    }

    @Nested
    @DisplayName("GuardrailResult Record")
    class GuardrailResultTests {

        @Test
        @DisplayName("allowed result should have correct state")
        void allowedResultState() {
            GuardrailResult result = GuardrailResult.allowed();

            assertThat(result.blocked()).isFalse();
            assertThat(result.flagged()).isFalse();
            assertThat(result.violations()).isEmpty();
            assertThat(result.approvalId()).isNull();
            assertThat(result.hasPendingApproval()).isFalse();
        }

        @Test
        @DisplayName("blocked result should have correct state")
        void blockedResultState() {
            GuardrailViolation violation = new GuardrailViolation("CRITICAL", "Test violation");
            GuardrailResult result = GuardrailResult.blocked("Blocked reason", java.util.List.of(violation));

            assertThat(result.blocked()).isTrue();
            assertThat(result.flagged()).isFalse();
            assertThat(result.reason()).isEqualTo("Blocked reason");
            assertThat(result.violations()).hasSize(1);
            assertThat(result.hasPendingApproval()).isFalse();
        }

        @Test
        @DisplayName("flagged result should have correct state")
        void flaggedResultState() {
            GuardrailViolation violation = new GuardrailViolation("MEDIUM", "Test violation");
            GuardrailResult result = GuardrailResult.flagged(java.util.List.of(violation));

            assertThat(result.blocked()).isFalse();
            assertThat(result.flagged()).isTrue();
            assertThat(result.violations()).hasSize(1);
            assertThat(result.hasPendingApproval()).isFalse();
        }

        @Test
        @DisplayName("flaggedWithApproval result should have approval ID")
        void flaggedWithApprovalState() {
            UUID approvalId = UUID.randomUUID();
            GuardrailViolation violation = new GuardrailViolation("MEDIUM", "Test violation");
            GuardrailResult result = GuardrailResult.flaggedWithApproval(java.util.List.of(violation), approvalId);

            assertThat(result.blocked()).isFalse();
            assertThat(result.flagged()).isTrue();
            assertThat(result.approvalId()).isEqualTo(approvalId);
            assertThat(result.hasPendingApproval()).isTrue();
        }
    }
}
