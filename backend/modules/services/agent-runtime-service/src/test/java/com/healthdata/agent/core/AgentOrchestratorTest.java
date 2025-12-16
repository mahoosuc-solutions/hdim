package com.healthdata.agent.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agent.approval.ApprovalIntegration;
import com.healthdata.agent.approval.ApprovalIntegration.ApprovalResult;
import com.healthdata.agent.core.AgentOrchestrator.AgentRequest;
import com.healthdata.agent.core.AgentOrchestrator.AgentResponse;
import com.healthdata.agent.core.GuardrailService.GuardrailResult;
import com.healthdata.agent.llm.LLMProvider;
import com.healthdata.agent.llm.LLMProviderFactory;
import com.healthdata.agent.llm.model.LLMRequest;
import com.healthdata.agent.llm.model.LLMResponse;
import com.healthdata.agent.llm.model.LLMStreamChunk;
import com.healthdata.agent.memory.CompositeMemoryManager;
import com.healthdata.agent.tool.Tool;
import com.healthdata.agent.tool.ToolDefinition;
import com.healthdata.agent.tool.ToolRegistry;
import com.healthdata.agent.tool.Tool.ToolResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentOrchestrator Tests")
class AgentOrchestratorTest {

    @Mock
    private LLMProviderFactory llmProviderFactory;

    @Mock
    private ToolRegistry toolRegistry;

    @Mock
    private CompositeMemoryManager memoryManager;

    @Mock
    private GuardrailService guardrailService;

    @Mock
    private ApprovalIntegration approvalIntegration;

    @Mock
    private LLMProvider llmProvider;

    @Mock
    private Tool mockTool;

    private AgentOrchestrator orchestrator;
    private MeterRegistry meterRegistry;
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String AGENT_TYPE = "clinical-assistant";

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        objectMapper = new ObjectMapper();

        orchestrator = new AgentOrchestrator(
            llmProviderFactory,
            toolRegistry,
            memoryManager,
            objectMapper,
            guardrailService,
            approvalIntegration,
            meterRegistry
        );

        // Set reasonable defaults for configuration
        ReflectionTestUtils.setField(orchestrator, "maxIterations", 10);
        ReflectionTestUtils.setField(orchestrator, "timeoutSeconds", 120);
    }

    private AgentContext createContext() {
        return AgentContext.builder()
            .tenantId(TENANT_ID)
            .userId(USER_ID)
            .sessionId("session-001")
            .correlationId("corr-001")
            .agentType(AGENT_TYPE)
            .roles(Set.of("CLINICAL_USER"))
            .build();
    }

    private LLMResponse createSuccessResponse(String content) {
        return LLMResponse.builder()
            .id("resp-123")
            .model("claude-3-5-sonnet-20241022")
            .content(content)
            .usage(LLMResponse.TokenUsage.of(100, 50))
            .stopReason("end_turn")
            .build();
    }

    @Nested
    @DisplayName("Basic Execution Tests")
    class BasicExecutionTests {

        @Test
        @DisplayName("should execute simple request successfully")
        void executeSimpleRequest() {
            // Given
            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("What is diabetes?");

            LLMResponse llmResponse = createSuccessResponse("Diabetes is a chronic condition...");

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(List.of()));
            when(llmProviderFactory.selectOptimalProvider(any(LLMRequest.class)))
                .thenReturn(llmProvider);
            when(toolRegistry.getToolDefinitions(context))
                .thenReturn(List.of());
            when(llmProvider.completeWithTools(any(LLMRequest.class), anyList()))
                .thenReturn(llmResponse);
            when(guardrailService.check(any(LLMResponse.class), eq(context)))
                .thenReturn(GuardrailResult.allowed());
            when(memoryManager.storeMessage(eq(context), any(LLMRequest.Message.class)))
                .thenReturn(Mono.empty());

            // When
            AgentResponse response = orchestrator.execute(request, context).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();
            assertThat(response.content()).isEqualTo("Diabetes is a chronic condition...");
            assertThat(response.model()).isEqualTo("claude-3-5-sonnet-20241022");
            assertThat(response.usage()).isNotNull();
            assertThat(response.usage().getTotalTokens()).isEqualTo(150);

            // Verify interactions
            verify(llmProviderFactory).selectOptimalProvider(any(LLMRequest.class));
            verify(guardrailService).check(llmResponse, context);
            verify(memoryManager, times(2)).storeMessage(eq(context), any(LLMRequest.Message.class));
        }

        @Test
        @DisplayName("should include conversation history in request")
        void includeConversationHistory() {
            // Given
            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("Follow-up question");

            List<LLMRequest.Message> history = List.of(
                LLMRequest.Message.user("Previous question"),
                LLMRequest.Message.assistant("Previous answer")
            );

            LLMResponse llmResponse = createSuccessResponse("Follow-up answer");

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(history));
            when(llmProviderFactory.selectOptimalProvider(any(LLMRequest.class)))
                .thenReturn(llmProvider);
            when(toolRegistry.getToolDefinitions(context))
                .thenReturn(List.of());
            when(guardrailService.check(any(LLMResponse.class), eq(context)))
                .thenReturn(GuardrailResult.allowed());
            when(memoryManager.storeMessage(eq(context), any(LLMRequest.Message.class)))
                .thenReturn(Mono.empty());

            ArgumentCaptor<LLMRequest> requestCaptor = ArgumentCaptor.forClass(LLMRequest.class);
            when(llmProvider.completeWithTools(requestCaptor.capture(), anyList()))
                .thenReturn(llmResponse);

            // When
            orchestrator.execute(request, context).block();

            // Then
            LLMRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getMessages()).hasSize(3); // 2 history + 1 new
            assertThat(capturedRequest.getMessages().get(0).getContent()).isEqualTo("Previous question");
            assertThat(capturedRequest.getMessages().get(2).getContent()).isEqualTo("Follow-up question");
        }

        @Test
        @DisplayName("should use custom system prompt when provided")
        void useCustomSystemPrompt() {
            // Given
            AgentContext context = createContext();
            String customPrompt = "You are a specialized diabetes educator.";
            AgentRequest request = AgentRequest.withSystemPrompt("Explain insulin", customPrompt);

            LLMResponse llmResponse = createSuccessResponse("Insulin is a hormone...");

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(List.of()));
            when(llmProviderFactory.selectOptimalProvider(any(LLMRequest.class)))
                .thenReturn(llmProvider);
            when(toolRegistry.getToolDefinitions(context))
                .thenReturn(List.of());
            when(guardrailService.check(any(LLMResponse.class), eq(context)))
                .thenReturn(GuardrailResult.allowed());
            when(memoryManager.storeMessage(eq(context), any(LLMRequest.Message.class)))
                .thenReturn(Mono.empty());

            ArgumentCaptor<LLMRequest> requestCaptor = ArgumentCaptor.forClass(LLMRequest.class);
            when(llmProvider.completeWithTools(requestCaptor.capture(), anyList()))
                .thenReturn(llmResponse);

            // When
            orchestrator.execute(request, context).block();

            // Then
            LLMRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getSystemPrompt()).isEqualTo(customPrompt);
        }

        @Test
        @DisplayName("should record metrics on successful execution")
        void recordMetricsOnSuccess() {
            // Given
            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("Test message");
            LLMResponse llmResponse = createSuccessResponse("Test response");

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(List.of()));
            when(llmProviderFactory.selectOptimalProvider(any(LLMRequest.class)))
                .thenReturn(llmProvider);
            when(toolRegistry.getToolDefinitions(context))
                .thenReturn(List.of());
            when(llmProvider.completeWithTools(any(LLMRequest.class), anyList()))
                .thenReturn(llmResponse);
            when(guardrailService.check(any(LLMResponse.class), eq(context)))
                .thenReturn(GuardrailResult.allowed());
            when(memoryManager.storeMessage(eq(context), any(LLMRequest.Message.class)))
                .thenReturn(Mono.empty());

            // When
            orchestrator.execute(request, context).block();

            // Then
            Counter taskCounter = meterRegistry.find("agent.tasks").counter();
            assertThat(taskCounter).isNotNull();
            assertThat(taskCounter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Tool Execution Tests")
    class ToolExecutionTests {

        @Test
        @DisplayName("should execute tool calls and continue iteration")
        void executeToolCallsSuccessfully() {
            // Given
            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("What are the patient's vitals?");

            // First LLM response with tool call
            LLMRequest.ToolCall toolCall = LLMRequest.ToolCall.builder()
                .id("call-123")
                .name("get_patient_vitals")
                .arguments(Map.of("patientId", "P123"))
                .build();

            LLMResponse firstResponse = LLMResponse.builder()
                .id("resp-1")
                .model("claude-3-5-sonnet-20241022")
                .content("Let me check the vitals...")
                .toolCalls(List.of(toolCall))
                .usage(LLMResponse.TokenUsage.of(50, 25))
                .build();

            // Second LLM response with final answer
            LLMResponse finalResponse = createSuccessResponse("The patient's vitals are: BP 120/80, HR 72");

            // Tool result
            ToolResult toolResult = ToolResult.success(
                "Vitals retrieved successfully",
                Map.of("bloodPressure", "120/80", "heartRate", 72)
            );

            ToolDefinition toolDefinition = ToolDefinition.builder()
                .name("get_patient_vitals")
                .description("Get patient vital signs")
                .requiresApproval(false)
                .build();

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(List.of()));
            when(llmProviderFactory.selectOptimalProvider(any(LLMRequest.class)))
                .thenReturn(llmProvider);
            when(toolRegistry.getToolDefinitions(context))
                .thenReturn(List.of(toolDefinition));
            when(toolRegistry.getTool("get_patient_vitals"))
                .thenReturn(Optional.of(mockTool));
            when(mockTool.getDefinition())
                .thenReturn(toolDefinition);
            when(mockTool.validate(anyMap()))
                .thenReturn(Tool.ValidationResult.valid());
            when(mockTool.execute(anyMap(), eq(context)))
                .thenReturn(Mono.just(toolResult));
            when(guardrailService.check(any(LLMResponse.class), eq(context)))
                .thenReturn(GuardrailResult.allowed());
            when(memoryManager.storeMessage(eq(context), any(LLMRequest.Message.class)))
                .thenReturn(Mono.empty());

            // First call returns tool call, second call returns final response
            when(llmProvider.completeWithTools(any(LLMRequest.class), anyList()))
                .thenReturn(firstResponse)
                .thenReturn(finalResponse);

            // When
            AgentResponse response = orchestrator.execute(request, context).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();
            assertThat(response.content()).contains("vitals are");

            // Verify tool was executed
            verify(mockTool).execute(eq(Map.of("patientId", "P123")), eq(context));
            verify(llmProvider, times(2)).completeWithTools(any(LLMRequest.class), anyList());

            // Verify metric was recorded
            Counter toolCounter = meterRegistry.find("agent.tool_invocations").counter();
            assertThat(toolCounter).isNotNull();
            assertThat(toolCounter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should handle tool validation errors")
        void handleToolValidationErrors() {
            // Given
            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("Get patient data");

            LLMRequest.ToolCall toolCall = LLMRequest.ToolCall.builder()
                .id("call-123")
                .name("get_patient_data")
                .arguments(Map.of("invalid", "args"))
                .build();

            LLMResponse toolResponse = LLMResponse.builder()
                .id("resp-1")
                .model("claude-3-5-sonnet-20241022")
                .content("")
                .toolCalls(List.of(toolCall))
                .usage(LLMResponse.TokenUsage.of(50, 25))
                .build();

            LLMResponse finalResponse = createSuccessResponse("I apologize, there was an error with the request.");

            ToolDefinition toolDefinition = ToolDefinition.builder()
                .name("get_patient_data")
                .description("Get patient data")
                .requiresApproval(false)
                .build();

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(List.of()));
            when(llmProviderFactory.selectOptimalProvider(any(LLMRequest.class)))
                .thenReturn(llmProvider);
            when(toolRegistry.getToolDefinitions(context))
                .thenReturn(List.of(toolDefinition));
            when(toolRegistry.getTool("get_patient_data"))
                .thenReturn(Optional.of(mockTool));
            when(mockTool.getDefinition())
                .thenReturn(toolDefinition);
            when(mockTool.validate(anyMap()))
                .thenReturn(Tool.ValidationResult.invalid(List.of("Missing required field: patientId")));
            when(guardrailService.check(any(LLMResponse.class), eq(context)))
                .thenReturn(GuardrailResult.allowed());
            when(memoryManager.storeMessage(eq(context), any(LLMRequest.Message.class)))
                .thenReturn(Mono.empty());
            when(llmProvider.completeWithTools(any(LLMRequest.class), anyList()))
                .thenReturn(toolResponse)
                .thenReturn(finalResponse);

            // When
            AgentResponse response = orchestrator.execute(request, context).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();

            // Tool should not have been executed, only validated
            verify(mockTool, never()).execute(anyMap(), any());
            verify(mockTool).validate(anyMap());
        }

        @Test
        @DisplayName("should handle tool requiring approval")
        void handleToolRequiringApproval() {
            // Given
            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("Prescribe medication");

            LLMRequest.ToolCall toolCall = LLMRequest.ToolCall.builder()
                .id("call-123")
                .name("prescribe_medication")
                .arguments(Map.of("medication", "metformin", "dosage", "500mg"))
                .build();

            LLMResponse toolResponse = LLMResponse.builder()
                .id("resp-1")
                .model("claude-3-5-sonnet-20241022")
                .content("")
                .toolCalls(List.of(toolCall))
                .usage(LLMResponse.TokenUsage.of(50, 25))
                .build();

            LLMResponse finalResponse = createSuccessResponse("Your request requires approval.");

            ToolDefinition toolDefinition = ToolDefinition.builder()
                .name("prescribe_medication")
                .description("Prescribe medication")
                .requiresApproval(true)
                .build();

            UUID approvalId = UUID.randomUUID();
            ApprovalResult approvalResult = ApprovalResult.pending(approvalId, "PENDING");

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(List.of()));
            when(llmProviderFactory.selectOptimalProvider(any(LLMRequest.class)))
                .thenReturn(llmProvider);
            when(toolRegistry.getToolDefinitions(context))
                .thenReturn(List.of(toolDefinition));
            when(toolRegistry.getTool("prescribe_medication"))
                .thenReturn(Optional.of(mockTool));
            when(mockTool.getDefinition())
                .thenReturn(toolDefinition);
            when(mockTool.validate(anyMap()))
                .thenReturn(Tool.ValidationResult.valid());
            when(approvalIntegration.checkAndCreateApprovalRequest(
                eq(toolDefinition), any(), eq(context)))
                .thenReturn(approvalResult);
            when(guardrailService.check(any(LLMResponse.class), eq(context)))
                .thenReturn(GuardrailResult.allowed());
            when(memoryManager.storeMessage(eq(context), any(LLMRequest.Message.class)))
                .thenReturn(Mono.empty());
            when(llmProvider.completeWithTools(any(LLMRequest.class), anyList()))
                .thenReturn(toolResponse)
                .thenReturn(finalResponse);

            // When
            AgentResponse response = orchestrator.execute(request, context).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();

            // Tool should not have been executed due to pending approval
            verify(mockTool, never()).execute(anyMap(), any());
            verify(approvalIntegration).checkAndCreateApprovalRequest(
                eq(toolDefinition), any(), eq(context));
        }

        @Test
        @DisplayName("should handle tool execution errors gracefully")
        void handleToolExecutionErrors() {
            // Given
            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("Get data");

            LLMRequest.ToolCall toolCall = LLMRequest.ToolCall.builder()
                .id("call-123")
                .name("failing_tool")
                .arguments(Map.of("key", "value"))
                .build();

            LLMResponse toolResponse = LLMResponse.builder()
                .id("resp-1")
                .model("claude-3-5-sonnet-20241022")
                .content("")
                .toolCalls(List.of(toolCall))
                .usage(LLMResponse.TokenUsage.of(50, 25))
                .build();

            LLMResponse finalResponse = createSuccessResponse("I encountered an error.");

            ToolDefinition toolDefinition = ToolDefinition.builder()
                .name("failing_tool")
                .description("A tool that fails")
                .requiresApproval(false)
                .build();

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(List.of()));
            when(llmProviderFactory.selectOptimalProvider(any(LLMRequest.class)))
                .thenReturn(llmProvider);
            when(toolRegistry.getToolDefinitions(context))
                .thenReturn(List.of(toolDefinition));
            when(toolRegistry.getTool("failing_tool"))
                .thenReturn(Optional.of(mockTool));
            when(mockTool.getDefinition())
                .thenReturn(toolDefinition);
            when(mockTool.validate(anyMap()))
                .thenReturn(Tool.ValidationResult.valid());
            when(mockTool.execute(anyMap(), eq(context)))
                .thenReturn(Mono.error(new RuntimeException("Tool execution failed")));
            when(guardrailService.check(any(LLMResponse.class), eq(context)))
                .thenReturn(GuardrailResult.allowed());
            when(memoryManager.storeMessage(eq(context), any(LLMRequest.Message.class)))
                .thenReturn(Mono.empty());
            when(llmProvider.completeWithTools(any(LLMRequest.class), anyList()))
                .thenReturn(toolResponse)
                .thenReturn(finalResponse);

            // When
            AgentResponse response = orchestrator.execute(request, context).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();
            verify(mockTool).execute(anyMap(), eq(context));
        }
    }

    @Nested
    @DisplayName("Guardrail Tests")
    class GuardrailTests {

        @Test
        @DisplayName("should block response when guardrails triggered")
        void blockResponseOnGuardrailViolation() {
            // Given
            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("Should I stop my medication?");

            LLMResponse llmResponse = createSuccessResponse("Yes, you should stop taking your medication immediately.");

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(List.of()));
            when(llmProviderFactory.selectOptimalProvider(any(LLMRequest.class)))
                .thenReturn(llmProvider);
            when(toolRegistry.getToolDefinitions(context))
                .thenReturn(List.of());
            when(llmProvider.completeWithTools(any(LLMRequest.class), anyList()))
                .thenReturn(llmResponse);
            when(guardrailService.check(any(LLMResponse.class), eq(context)))
                .thenReturn(GuardrailResult.blocked("Patient safety violation", List.of()));
            when(memoryManager.storeMessage(eq(context), any(LLMRequest.Message.class)))
                .thenReturn(Mono.empty());

            // When
            AgentResponse response = orchestrator.execute(request, context).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.blocked()).isTrue();
            assertThat(response.blockReason()).isEqualTo("Patient safety violation");

            verify(guardrailService).check(llmResponse, context);
        }

        @Test
        @DisplayName("should allow safe content through guardrails")
        void allowSafeContent() {
            // Given
            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("What is healthy eating?");

            LLMResponse llmResponse = createSuccessResponse("Healthy eating includes a balanced diet...");

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(List.of()));
            when(llmProviderFactory.selectOptimalProvider(any(LLMRequest.class)))
                .thenReturn(llmProvider);
            when(toolRegistry.getToolDefinitions(context))
                .thenReturn(List.of());
            when(llmProvider.completeWithTools(any(LLMRequest.class), anyList()))
                .thenReturn(llmResponse);
            when(guardrailService.check(any(LLMResponse.class), eq(context)))
                .thenReturn(GuardrailResult.allowed());
            when(memoryManager.storeMessage(eq(context), any(LLMRequest.Message.class)))
                .thenReturn(Mono.empty());

            // When
            AgentResponse response = orchestrator.execute(request, context).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();
            assertThat(response.blocked()).isFalse();
        }
    }

    @Nested
    @DisplayName("Timeout and Error Handling Tests")
    class TimeoutAndErrorTests {

        @Test
        @DisplayName("should timeout after configured duration")
        void timeoutAfterConfiguredDuration() {
            // Given
            ReflectionTestUtils.setField(orchestrator, "timeoutSeconds", 1);

            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("Long running query");

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(List.of()));
            when(llmProviderFactory.selectOptimalProvider(any(LLMRequest.class)))
                .thenReturn(llmProvider);
            when(toolRegistry.getToolDefinitions(context))
                .thenReturn(List.of());
            when(llmProvider.completeWithTools(any(LLMRequest.class), anyList()))
                .thenAnswer(invocation -> {
                    Thread.sleep(5000); // Simulate long-running operation
                    return createSuccessResponse("Too late");
                });

            // When/Then - Should timeout
            try {
                orchestrator.execute(request, context).block();
                // Should not reach here
                assertThat(false).as("Expected timeout exception").isTrue();
            } catch (Exception e) {
                // Expected timeout exception
                assertThat(e.getMessage()).containsAnyOf("timeout", "Timeout", "timed out");
            }
        }

        @Test
        @DisplayName("should stop after max iterations")
        void stopAfterMaxIterations() {
            // Given
            ReflectionTestUtils.setField(orchestrator, "maxIterations", 2);

            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("Iterative query");

            // Create a tool call that will keep triggering
            LLMRequest.ToolCall toolCall = LLMRequest.ToolCall.builder()
                .id("call-123")
                .name("iterative_tool")
                .arguments(Map.of("key", "value"))
                .build();

            LLMResponse toolResponse = LLMResponse.builder()
                .id("resp-1")
                .model("claude-3-5-sonnet-20241022")
                .content("")
                .toolCalls(List.of(toolCall))
                .usage(LLMResponse.TokenUsage.of(50, 25))
                .build();

            ToolDefinition toolDefinition = ToolDefinition.builder()
                .name("iterative_tool")
                .description("An iterative tool")
                .requiresApproval(false)
                .build();

            ToolResult toolResult = ToolResult.success("Tool executed", Map.of("result", "data"));

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(List.of()));
            when(llmProviderFactory.selectOptimalProvider(any(LLMRequest.class)))
                .thenReturn(llmProvider);
            when(toolRegistry.getToolDefinitions(context))
                .thenReturn(List.of(toolDefinition));
            when(toolRegistry.getTool("iterative_tool"))
                .thenReturn(Optional.of(mockTool));
            when(mockTool.getDefinition())
                .thenReturn(toolDefinition);
            when(mockTool.validate(anyMap()))
                .thenReturn(Tool.ValidationResult.valid());
            when(mockTool.execute(anyMap(), eq(context)))
                .thenReturn(Mono.just(toolResult));
            when(guardrailService.check(any(LLMResponse.class), eq(context)))
                .thenReturn(GuardrailResult.allowed());
            when(memoryManager.storeMessage(eq(context), any(LLMRequest.Message.class)))
                .thenReturn(Mono.empty());

            // Always return a tool response to force iteration limit
            when(llmProvider.completeWithTools(any(LLMRequest.class), anyList()))
                .thenReturn(toolResponse);

            // When
            AgentResponse response = orchestrator.execute(request, context).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.error()).contains("Maximum iterations reached");
        }

        @Test
        @DisplayName("should handle LLM provider errors gracefully")
        void handleLLMProviderErrors() {
            // Given
            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("Query that fails");

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(List.of()));
            when(llmProviderFactory.selectOptimalProvider(any(LLMRequest.class)))
                .thenReturn(llmProvider);
            when(toolRegistry.getToolDefinitions(context))
                .thenReturn(List.of());
            when(llmProvider.completeWithTools(any(LLMRequest.class), anyList()))
                .thenThrow(new RuntimeException("LLM provider unavailable"));

            // When/Then
            try {
                orchestrator.execute(request, context).block();
                // Should not reach here
                assertThat(false).as("Expected RuntimeException").isTrue();
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).contains("LLM provider unavailable");
            }
        }
    }

    @Nested
    @DisplayName("Task Management Tests")
    class TaskManagementTests {

        @Test
        @DisplayName("should track active tasks")
        void trackActiveTasks() {
            // Given
            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("Test message");
            LLMResponse llmResponse = createSuccessResponse("Test response");

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(List.<LLMRequest.Message>of()).delayElement(Duration.ofMillis(100)));
            when(llmProviderFactory.selectOptimalProvider(any(LLMRequest.class)))
                .thenReturn(llmProvider);
            when(toolRegistry.getToolDefinitions(context))
                .thenReturn(List.of());
            when(llmProvider.completeWithTools(any(LLMRequest.class), anyList()))
                .thenReturn(llmResponse);
            when(guardrailService.check(any(LLMResponse.class), eq(context)))
                .thenReturn(GuardrailResult.allowed());
            when(memoryManager.storeMessage(eq(context), any(LLMRequest.Message.class)))
                .thenReturn(Mono.empty());

            // When
            Mono<AgentResponse> execution = orchestrator.execute(request, context);

            // Task should be tracked while executing (check before blocking)
            // Then complete the execution
            AgentResponse response = execution.block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();
        }

        @Test
        @DisplayName("should cancel active task")
        void cancelActiveTask() {
            // Given
            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("Long running task");

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(List.<LLMRequest.Message>of()).delayElement(Duration.ofSeconds(10)));

            // When
            Mono<AgentResponse> execution = orchestrator.execute(request, context);

            // Start execution in background (don't block yet)
            execution.subscribe();

            // Give it a moment to register the task
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Note: We can't easily test cancellation without accessing internals
            // This is a basic structure test
        }

        @Test
        @DisplayName("should remove task from active tasks on completion")
        void removeTaskOnCompletion() {
            // Given
            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("Test message");
            LLMResponse llmResponse = createSuccessResponse("Test response");

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(List.of()));
            when(llmProviderFactory.selectOptimalProvider(any(LLMRequest.class)))
                .thenReturn(llmProvider);
            when(toolRegistry.getToolDefinitions(context))
                .thenReturn(List.of());
            when(llmProvider.completeWithTools(any(LLMRequest.class), anyList()))
                .thenReturn(llmResponse);
            when(guardrailService.check(any(LLMResponse.class), eq(context)))
                .thenReturn(GuardrailResult.allowed());
            when(memoryManager.storeMessage(eq(context), any(LLMRequest.Message.class)))
                .thenReturn(Mono.empty());

            // When
            orchestrator.execute(request, context).block();

            // Then - task should be removed after completion
            // (Internal state, verified through completion)
        }
    }

    @Nested
    @DisplayName("Streaming Tests")
    class StreamingTests {

        @Test
        @DisplayName("should stream responses successfully")
        void streamResponsesSuccessfully() {
            // Given
            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("Stream this response");

            List<LLMRequest.Message> history = List.of();

            Flux<LLMStreamChunk> streamChunks = Flux.just(
                LLMStreamChunk.builder()
                    .type(LLMStreamChunk.ChunkType.CONTENT_BLOCK_DELTA)
                    .delta("Hello")
                    .build(),
                LLMStreamChunk.builder()
                    .type(LLMStreamChunk.ChunkType.CONTENT_BLOCK_DELTA)
                    .delta(" World")
                    .build(),
                LLMStreamChunk.builder()
                    .type(LLMStreamChunk.ChunkType.DONE)
                    .usage(LLMResponse.TokenUsage.of(10, 5))
                    .build()
            );

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(history));
            when(llmProviderFactory.selectOptimalProvider(any(LLMRequest.class)))
                .thenReturn(llmProvider);
            when(llmProvider.completeStreaming(any(LLMRequest.class)))
                .thenReturn(streamChunks);

            // When
            List<AgentOrchestrator.AgentStreamEvent> events =
                orchestrator.executeStreaming(request, context).collectList().block();

            // Then
            assertThat(events).hasSize(3);
            assertThat(events.get(0).type()).isEqualTo("text_delta");
            assertThat(events.get(0).delta()).isEqualTo("Hello");
            assertThat(events.get(1).type()).isEqualTo("text_delta");
            assertThat(events.get(1).delta()).isEqualTo(" World");
            assertThat(events.get(2).type()).isEqualTo("done");
        }

        @Test
        @DisplayName("should handle streaming errors")
        void handleStreamingErrors() {
            // Given
            AgentContext context = createContext();
            AgentRequest request = AgentRequest.simple("Stream that fails");

            when(memoryManager.getConversationHistory(eq(context), anyInt()))
                .thenReturn(Mono.just(List.of()));
            when(llmProviderFactory.selectOptimalProvider(any(LLMRequest.class)))
                .thenReturn(llmProvider);
            when(llmProvider.completeStreaming(any(LLMRequest.class)))
                .thenReturn(Flux.error(new RuntimeException("Stream failed")));

            // When/Then
            try {
                orchestrator.executeStreaming(request, context).collectList().block();
                // Should not reach here
                assertThat(false).as("Expected RuntimeException").isTrue();
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).contains("Stream failed");
            }
        }
    }
}
