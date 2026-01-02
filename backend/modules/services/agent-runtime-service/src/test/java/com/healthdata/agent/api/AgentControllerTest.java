package com.healthdata.agent.api;

import com.healthdata.agent.api.AgentController.*;
import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.core.AgentOrchestrator;
import com.healthdata.agent.core.AgentOrchestrator.AgentRequest;
import com.healthdata.agent.core.AgentOrchestrator.AgentResponse;
import com.healthdata.agent.core.AgentOrchestrator.AgentTask;
import com.healthdata.agent.llm.LLMProviderFactory;
import com.healthdata.agent.llm.LLMProvider;
import com.healthdata.agent.llm.model.LLMResponse;
import com.healthdata.agent.memory.CompositeMemoryManager;
import com.healthdata.agent.tool.Tool;
import com.healthdata.agent.tool.ToolDefinition;
import com.healthdata.agent.tool.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentController Tests")
class AgentControllerTest {

    @Mock
    private AgentOrchestrator orchestrator;

    @Mock
    private ToolRegistry toolRegistry;

    @Mock
    private LLMProviderFactory llmProviderFactory;

    @Mock
    private CompositeMemoryManager memoryManager;

    @Mock
    private Jwt jwt;

    private AgentController controller;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String SESSION_ID = "session-789";
    private static final String AGENT_TYPE = "clinical-assistant";

    @BeforeEach
    void setUp() {
        controller = new AgentController(orchestrator, toolRegistry, llmProviderFactory, memoryManager);
    }

    private Jwt createMockJwt() {
        when(jwt.getSubject()).thenReturn(USER_ID);
        when(jwt.getClaimAsStringList("roles")).thenReturn(List.of("CLINICAL_USER", "VIEWER"));
        return jwt;
    }

    @Nested
    @DisplayName("Execute Endpoint Tests")
    class ExecuteTests {

        @Test
        @DisplayName("should execute agent successfully")
        void executeAgentSuccessfully() {
            // Given
            AgentRequestDTO request = new AgentRequestDTO(
                "What are my care gaps?",
                null,
                "claude-3-sonnet",
                4096,
                0.3,
                List.of("fhir_query", "care_gap_search"),
                SESSION_ID,
                "patient-001",
                Map.of("source", "dashboard")
            );

            LLMResponse.TokenUsage usage = LLMResponse.TokenUsage.builder()
                .inputTokens(100)
                .outputTokens(200)
                .totalTokens(300)
                .build();

            AgentResponse response = new AgentResponse(
                true, "Here are your care gaps...", null, usage, "claude-3-sonnet", false, null
            );

            when(orchestrator.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenReturn(Mono.just(response));

            // When
            ResponseEntity<AgentResponseDTO> entity = controller.execute(
                AGENT_TYPE, request, TENANT_ID, createMockJwt()
            ).block();

            // Then
            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(entity.getBody()).isNotNull();
            assertThat(entity.getBody().success()).isTrue();
            assertThat(entity.getBody().content()).isEqualTo("Here are your care gaps...");
            assertThat(entity.getBody().model()).isEqualTo("claude-3-sonnet");
            assertThat(entity.getBody().usage().totalTokens()).isEqualTo(300);

            // Verify context was built correctly
            ArgumentCaptor<AgentContext> contextCaptor = ArgumentCaptor.forClass(AgentContext.class);
            verify(orchestrator).execute(any(), contextCaptor.capture());
            AgentContext capturedContext = contextCaptor.getValue();
            assertThat(capturedContext.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(capturedContext.getUserId()).isEqualTo(USER_ID);
            assertThat(capturedContext.getAgentType()).isEqualTo(AGENT_TYPE);
        }

        @Test
        @DisplayName("should handle execution error gracefully")
        void handleExecutionError() {
            // Given
            AgentRequestDTO request = new AgentRequestDTO(
                "Test message", null, null, null, null, null, null, null, null
            );

            when(orchestrator.execute(any(), any()))
                .thenReturn(Mono.error(new RuntimeException("LLM provider unavailable")));

            // When
            ResponseEntity<AgentResponseDTO> entity = controller.execute(
                AGENT_TYPE, request, TENANT_ID, createMockJwt()
            ).block();

            // Then
            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(entity.getBody()).isNotNull();
            assertThat(entity.getBody().success()).isFalse();
            assertThat(entity.getBody().error()).contains("LLM provider unavailable");
        }

        @Test
        @DisplayName("should handle blocked response")
        void handleBlockedResponse() {
            // Given
            AgentRequestDTO request = new AgentRequestDTO(
                "Prescribe medication", null, null, null, null, null, null, null, null
            );

            AgentResponse blockedResponse = new AgentResponse(
                false, null, null, null, "claude-3-sonnet", true, "Content blocked by guardrails"
            );

            when(orchestrator.execute(any(), any())).thenReturn(Mono.just(blockedResponse));

            // When
            ResponseEntity<AgentResponseDTO> entity = controller.execute(
                AGENT_TYPE, request, TENANT_ID, createMockJwt()
            ).block();

            // Then
            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(entity.getBody()).isNotNull();
            assertThat(entity.getBody().blocked()).isTrue();
            assertThat(entity.getBody().blockReason()).isEqualTo("Content blocked by guardrails");
        }

        @Test
        @DisplayName("should use default values for optional request parameters")
        void useDefaultValues() {
            // Given
            AgentRequestDTO request = new AgentRequestDTO(
                "Test message", null, null, null, null, null, null, null, null
            );

            AgentResponse response = new AgentResponse(
                true, "Response", null, null, null, false, null
            );

            when(orchestrator.execute(any(AgentRequest.class), any()))
                .thenReturn(Mono.just(response));

            // When
            controller.execute(AGENT_TYPE, request, TENANT_ID, createMockJwt());

            // Then
            ArgumentCaptor<AgentRequest> requestCaptor = ArgumentCaptor.forClass(AgentRequest.class);
            verify(orchestrator).execute(requestCaptor.capture(), any());
            AgentRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.maxTokens()).isEqualTo(4096);
            assertThat(capturedRequest.temperature()).isEqualTo(0.3);
        }
    }

    @Nested
    @DisplayName("Task Management Tests")
    class TaskManagementTests {

        @Test
        @DisplayName("should cancel task successfully")
        void cancelTaskSuccessfully() {
            // Given
            String taskId = "task-001";
            when(orchestrator.cancelTask(taskId)).thenReturn(Mono.just(true));

            // When
            ResponseEntity<Map<String, Object>> entity = controller.cancelTask(taskId).block();

            // Then
            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(entity.getBody()).containsEntry("cancelled", true);
            assertThat(entity.getBody()).containsEntry("taskId", taskId);
        }

        @Test
        @DisplayName("should return 404 when task not found for cancellation")
        void cancelTaskNotFound() {
            // Given
            String taskId = "unknown-task";
            when(orchestrator.cancelTask(taskId)).thenReturn(Mono.just(false));

            // When
            ResponseEntity<Map<String, Object>> entity = controller.cancelTask(taskId).block();

            // Then
            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("should return task status")
        void getTaskStatus() {
            // Given
            String taskId = "task-002";
            AgentContext context = AgentContext.builder()
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .agentType(AGENT_TYPE)
                .build();

            AgentRequest request = new AgentRequest("test message", null, null, 4096, 0.3, null, null);
            AgentTask activeTask = new AgentTask(taskId, context, request, Instant.now());
            when(orchestrator.getTaskStatus(taskId)).thenReturn(Optional.of(activeTask));

            // When
            ResponseEntity<Map<String, Object>> result = controller.getTaskStatus(taskId);

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).containsEntry("taskId", taskId);
            assertThat(result.getBody()).containsEntry("agentType", AGENT_TYPE);
            assertThat(result.getBody()).containsEntry("status", "IN_PROGRESS");
        }

        @Test
        @DisplayName("should return 404 when task status not found")
        void getTaskStatusNotFound() {
            // Given
            String taskId = "unknown-task";
            when(orchestrator.getTaskStatus(taskId)).thenReturn(Optional.empty());

            // When
            ResponseEntity<Map<String, Object>> result = controller.getTaskStatus(taskId);

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Tools Endpoint Tests")
    class ToolsTests {

        @Test
        @DisplayName("should list available tools")
        void listTools() {
            // Given
            Tool mockTool1 = createMockTool("fhir_query", "Query FHIR resources", ToolDefinition.ToolCategory.FHIR_QUERY, false);
            Tool mockTool2 = createMockTool("send_notification", "Send notifications", ToolDefinition.ToolCategory.NOTIFICATION, true);

            when(toolRegistry.listAvailableTools(any(AgentContext.class)))
                .thenReturn(List.of(mockTool1, mockTool2));

            // When
            ResponseEntity<List<ToolInfoDTO>> result = controller.listTools(TENANT_ID, createMockJwt());

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).hasSize(2);
            assertThat(result.getBody().get(0).name()).isEqualTo("fhir_query");
            assertThat(result.getBody().get(1).requiresApproval()).isTrue();
        }

        private Tool createMockTool(String name, String description, ToolDefinition.ToolCategory category, boolean requiresApproval) {
            Tool tool = mock(Tool.class);
            ToolDefinition definition = ToolDefinition.builder()
                .name(name)
                .description(description)
                .category(category)
                .requiresApproval(requiresApproval)
                .build();

            when(tool.getName()).thenReturn(name);
            when(tool.getDefinition()).thenReturn(definition);

            return tool;
        }
    }

    @Nested
    @DisplayName("Providers Endpoint Tests")
    class ProvidersTests {

        @Test
        @DisplayName("should list LLM providers and health status")
        void listProviders() {
            // Given
            when(llmProviderFactory.listProviders()).thenReturn(List.of("claude", "azure-openai", "bedrock"));
            when(llmProviderFactory.listHealthyProviders()).thenReturn(List.of("claude", "azure-openai"));
            when(llmProviderFactory.getHealthStatus()).thenReturn(Map.of(
                "claude", LLMProvider.HealthStatus.healthy(50),
                "azure-openai", LLMProvider.HealthStatus.healthy(100),
                "bedrock", LLMProvider.HealthStatus.unhealthy("Connection timeout")
            ));

            // When
            ResponseEntity<Map<String, Object>> result = controller.listProviders();

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).containsKey("registered");
            assertThat(result.getBody()).containsKey("healthy");
            assertThat(result.getBody()).containsKey("healthStatus");

            @SuppressWarnings("unchecked")
            List<String> registered = (List<String>) result.getBody().get("registered");
            assertThat(registered).hasSize(3);

            @SuppressWarnings("unchecked")
            List<String> healthy = (List<String>) result.getBody().get("healthy");
            assertThat(healthy).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Memory Management Tests")
    class MemoryTests {

        @Test
        @DisplayName("should clear session memory")
        void clearMemory() {
            // Given
            when(memoryManager.clearConversation(any(AgentContext.class))).thenReturn(Mono.empty());

            // When
            ResponseEntity<Void> entity = controller.clearMemory(SESSION_ID, TENANT_ID, createMockJwt()).block();

            // Then
            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            ArgumentCaptor<AgentContext> contextCaptor = ArgumentCaptor.forClass(AgentContext.class);
            verify(memoryManager).clearConversation(contextCaptor.capture());
            assertThat(contextCaptor.getValue().getSessionId()).isEqualTo(SESSION_ID);
        }
    }

    @Nested
    @DisplayName("Health Endpoint Tests")
    class HealthTests {

        @Test
        @DisplayName("should return health status")
        void healthCheck() {
            // Given
            when(llmProviderFactory.getHealthStatus()).thenReturn(Map.of(
                "claude", LLMProvider.HealthStatus.healthy(50),
                "bedrock", LLMProvider.HealthStatus.unhealthy("DOWN")
            ));
            when(toolRegistry.getToolCount()).thenReturn(15);

            // When
            ResponseEntity<Map<String, Object>> result = controller.health();

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).containsEntry("status", "UP");
            assertThat(result.getBody()).containsEntry("toolCount", 15);
            assertThat(result.getBody()).containsKey("providers");
        }
    }

    @Nested
    @DisplayName("Context Building Tests")
    class ContextBuildingTests {

        @Test
        @DisplayName("should generate session ID when not provided")
        void generateSessionIdWhenNotProvided() {
            // Given
            AgentRequestDTO request = new AgentRequestDTO(
                "Test message", null, null, null, null, null, null, null, null
            );

            AgentResponse response = new AgentResponse(true, "Response", null, null, null, false, null);
            when(orchestrator.execute(any(), any())).thenReturn(Mono.just(response));

            // When
            controller.execute(AGENT_TYPE, request, TENANT_ID, createMockJwt());

            // Then
            ArgumentCaptor<AgentContext> contextCaptor = ArgumentCaptor.forClass(AgentContext.class);
            verify(orchestrator).execute(any(), contextCaptor.capture());
            assertThat(contextCaptor.getValue().getSessionId()).isNotNull();
            assertThat(contextCaptor.getValue().getSessionId()).isNotEmpty();
        }

        @Test
        @DisplayName("should handle null JWT for anonymous users")
        void handleNullJwt() {
            // Given
            AgentRequestDTO request = new AgentRequestDTO(
                "Test message", null, null, null, null, null, SESSION_ID, null, null
            );

            AgentResponse response = new AgentResponse(true, "Response", null, null, null, false, null);
            when(orchestrator.execute(any(), any())).thenReturn(Mono.just(response));

            // When
            controller.execute(AGENT_TYPE, request, TENANT_ID, null);

            // Then
            ArgumentCaptor<AgentContext> contextCaptor = ArgumentCaptor.forClass(AgentContext.class);
            verify(orchestrator).execute(any(), contextCaptor.capture());
            assertThat(contextCaptor.getValue().getUserId()).isEqualTo("anonymous");
            assertThat(contextCaptor.getValue().getRoles()).isEmpty();
        }
    }

    @Nested
    @DisplayName("DTO Tests")
    class DTOTests {

        @Test
        @DisplayName("AgentResponseDTO.error should create error response")
        void errorResponseDTO() {
            AgentResponseDTO errorResponse = AgentResponseDTO.error("Something went wrong");

            assertThat(errorResponse.success()).isFalse();
            assertThat(errorResponse.error()).isEqualTo("Something went wrong");
            assertThat(errorResponse.content()).isNull();
            assertThat(errorResponse.blocked()).isFalse();
        }
    }
}
