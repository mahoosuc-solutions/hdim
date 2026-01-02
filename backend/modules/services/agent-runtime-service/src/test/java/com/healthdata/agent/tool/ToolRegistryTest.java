package com.healthdata.agent.tool;

import com.healthdata.agent.core.AgentContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ToolRegistry Tests")
class ToolRegistryTest {

    @Mock
    private Tool mockTool1;

    @Mock
    private Tool mockTool2;

    @Mock
    private Tool mockTool3;

    private ToolRegistry toolRegistry;

    private AgentContext testContext;

    @BeforeEach
    void setUp() {
        // Setup mock tool definitions
        ToolDefinition def1 = ToolDefinition.builder()
            .name("fhir_query")
            .description("Query FHIR resources")
            .category(ToolDefinition.ToolCategory.FHIR_QUERY)
            .build();

        ToolDefinition def2 = ToolDefinition.builder()
            .name("cql_execute")
            .description("Execute CQL measures")
            .category(ToolDefinition.ToolCategory.CQL_EXECUTION)
            .build();

        ToolDefinition def3 = ToolDefinition.builder()
            .name("send_notification")
            .description("Send notifications")
            .category(ToolDefinition.ToolCategory.NOTIFICATION)
            .build();

        when(mockTool1.getName()).thenReturn("fhir_query");
        when(mockTool1.getDefinition()).thenReturn(def1);

        when(mockTool2.getName()).thenReturn("cql_execute");
        when(mockTool2.getDefinition()).thenReturn(def2);

        when(mockTool3.getName()).thenReturn("send_notification");
        when(mockTool3.getDefinition()).thenReturn(def3);

        toolRegistry = new ToolRegistry(List.of(mockTool1, mockTool2, mockTool3));

        testContext = AgentContext.builder()
            .tenantId("tenant-123")
            .userId("user-456")
            .sessionId("session-001")
            .correlationId("corr-001")
            .agentType("clinical-assistant")
            .roles(Set.of("CLINICAL_USER"))
            .build();
    }

    @Nested
    @DisplayName("Tool Registration")
    class RegistrationTests {

        @Test
        @DisplayName("should register tools on initialization")
        void registerToolsOnInit() {
            assertThat(toolRegistry.getToolCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should dynamically register new tool")
        void dynamicRegistration() {
            // Given
            Tool newTool = createMockTool("new_tool", ToolDefinition.ToolCategory.REPORTING);

            // When
            toolRegistry.registerTool(newTool);

            // Then
            assertThat(toolRegistry.getToolCount()).isEqualTo(4);
            assertThat(toolRegistry.hasTool("new_tool")).isTrue();
        }

        @Test
        @DisplayName("should overwrite existing tool when registering duplicate")
        void overwriteExistingTool() {
            // Given
            Tool duplicateTool = createMockTool("fhir_query", ToolDefinition.ToolCategory.REPORTING);

            // When
            toolRegistry.registerTool(duplicateTool);

            // Then
            assertThat(toolRegistry.getToolCount()).isEqualTo(3);
            Tool retrieved = toolRegistry.getToolOrThrow("fhir_query");
            assertThat(retrieved.getDefinition().getCategory())
                .isEqualTo(ToolDefinition.ToolCategory.REPORTING);
        }

        @Test
        @DisplayName("should unregister tool")
        void unregisterTool() {
            // When
            toolRegistry.unregisterTool("cql_execute");

            // Then
            assertThat(toolRegistry.getToolCount()).isEqualTo(2);
            assertThat(toolRegistry.hasTool("cql_execute")).isFalse();
        }
    }

    @Nested
    @DisplayName("Tool Retrieval")
    class RetrievalTests {

        @Test
        @DisplayName("should get tool by name")
        void getToolByName() {
            Optional<Tool> tool = toolRegistry.getTool("fhir_query");

            assertThat(tool).isPresent();
            assertThat(tool.get().getName()).isEqualTo("fhir_query");
        }

        @Test
        @DisplayName("should return empty for non-existent tool")
        void getToolNotFound() {
            Optional<Tool> tool = toolRegistry.getTool("non_existent");

            assertThat(tool).isEmpty();
        }

        @Test
        @DisplayName("should throw exception for non-existent tool when using getOrThrow")
        void getToolOrThrowNotFound() {
            assertThatThrownBy(() -> toolRegistry.getToolOrThrow("non_existent"))
                .isInstanceOf(ToolRegistry.ToolNotFoundException.class)
                .hasMessageContaining("non_existent");
        }

        @Test
        @DisplayName("should list all tools")
        void listAllTools() {
            List<Tool> tools = toolRegistry.listTools();

            assertThat(tools).hasSize(3);
        }

        @Test
        @DisplayName("should list tools by category")
        void listToolsByCategory() {
            List<Tool> fhirTools = toolRegistry.listToolsByCategory(ToolDefinition.ToolCategory.FHIR_QUERY);

            assertThat(fhirTools).hasSize(1);
            assertThat(fhirTools.get(0).getName()).isEqualTo("fhir_query");
        }
    }

    @Nested
    @DisplayName("Context-based Tool Filtering")
    class ContextFilteringTests {

        @Test
        @DisplayName("should list available tools in context")
        void listAvailableToolsInContext() {
            // Given - all tools are available by default
            when(mockTool1.isAvailable(any())).thenReturn(true);
            when(mockTool2.isAvailable(any())).thenReturn(true);
            when(mockTool3.isAvailable(any())).thenReturn(false);

            // When
            List<Tool> availableTools = toolRegistry.listAvailableTools(testContext);

            // Then
            assertThat(availableTools).hasSize(2);
            assertThat(availableTools.stream().map(Tool::getName))
                .containsExactlyInAnyOrder("fhir_query", "cql_execute");
        }

        @Test
        @DisplayName("should get tool definitions for context")
        void getToolDefinitionsForContext() {
            // Given
            when(mockTool1.isAvailable(any())).thenReturn(true);
            when(mockTool2.isAvailable(any())).thenReturn(true);
            when(mockTool3.isAvailable(any())).thenReturn(false);

            // When
            List<ToolDefinition> definitions = toolRegistry.getToolDefinitions(testContext);

            // Then
            assertThat(definitions).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Tool Definition Export")
    class DefinitionExportTests {

        @Test
        @DisplayName("should get all tool definitions")
        void getAllDefinitions() {
            List<ToolDefinition> definitions = toolRegistry.getToolDefinitions();

            assertThat(definitions).hasSize(3);
        }

        @Test
        @DisplayName("should get tool definitions by names")
        void getDefinitionsByNames() {
            List<ToolDefinition> definitions = toolRegistry.getToolDefinitions(
                List.of("fhir_query", "send_notification"));

            assertThat(definitions).hasSize(2);
            assertThat(definitions.stream().map(ToolDefinition::getName))
                .containsExactlyInAnyOrder("fhir_query", "send_notification");
        }

        @Test
        @DisplayName("should skip non-existent tools in name list")
        void skipNonExistentInNameList() {
            List<ToolDefinition> definitions = toolRegistry.getToolDefinitions(
                List.of("fhir_query", "non_existent", "cql_execute"));

            assertThat(definitions).hasSize(2);
        }
    }

    private Tool createMockTool(String name, ToolDefinition.ToolCategory category) {
        return new Tool() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                    .name(name)
                    .description("Mock tool: " + name)
                    .category(category)
                    .build();
            }

            @Override
            public Mono<ToolResult> execute(Map<String, Object> arguments, AgentContext context) {
                return Mono.just(ToolResult.success("Mock result"));
            }
        };
    }
}
