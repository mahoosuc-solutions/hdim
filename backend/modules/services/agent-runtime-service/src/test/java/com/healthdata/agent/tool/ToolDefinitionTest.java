package com.healthdata.agent.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ToolDefinition Tests")
class ToolDefinitionTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("should build tool definition with all fields")
        void buildWithAllFields() {
            Map<String, Object> inputSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                    "patientId", Map.of("type", "string"),
                    "resourceType", Map.of("type", "string")
                )
            );

            ToolDefinition definition = ToolDefinition.builder()
                .name("fhir_query")
                .description("Query FHIR resources")
                .inputSchema(inputSchema)
                .requiredParams(List.of("patientId"))
                .requiresApproval(true)
                .approvalCategory(ToolDefinition.ApprovalCategory.CLINICAL)
                .requiredApprovalRole("CLINICAL_ADMIN")
                .category(ToolDefinition.ToolCategory.FHIR_QUERY)
                .build();

            assertThat(definition.getName()).isEqualTo("fhir_query");
            assertThat(definition.getDescription()).isEqualTo("Query FHIR resources");
            assertThat(definition.getInputSchema()).isEqualTo(inputSchema);
            assertThat(definition.getRequiredParams()).containsExactly("patientId");
            assertThat(definition.isRequiresApproval()).isTrue();
            assertThat(definition.getApprovalCategory()).isEqualTo(ToolDefinition.ApprovalCategory.CLINICAL);
            assertThat(definition.getRequiredApprovalRole()).isEqualTo("CLINICAL_ADMIN");
            assertThat(definition.getCategory()).isEqualTo(ToolDefinition.ToolCategory.FHIR_QUERY);
        }

        @Test
        @DisplayName("should have default values")
        void defaultValues() {
            ToolDefinition definition = ToolDefinition.builder()
                .name("test_tool")
                .build();

            assertThat(definition.isRequiresApproval()).isFalse();
            assertThat(definition.getApprovalCategory()).isEqualTo(ToolDefinition.ApprovalCategory.NONE);
        }
    }

    @Nested
    @DisplayName("Approval Logic Tests")
    class ApprovalLogicTests {

        @Test
        @DisplayName("should need approval when requiresApproval is true")
        void needsApprovalWhenFlagSet() {
            ToolDefinition definition = ToolDefinition.builder()
                .name("test_tool")
                .requiresApproval(true)
                .build();

            assertThat(definition.needsApproval()).isTrue();
        }

        @Test
        @DisplayName("should need approval when category is not NONE")
        void needsApprovalWhenCategorySet() {
            ToolDefinition definition = ToolDefinition.builder()
                .name("test_tool")
                .approvalCategory(ToolDefinition.ApprovalCategory.STANDARD)
                .build();

            assertThat(definition.needsApproval()).isTrue();
        }

        @Test
        @DisplayName("should not need approval when both flags are default")
        void noApprovalByDefault() {
            ToolDefinition definition = ToolDefinition.builder()
                .name("test_tool")
                .build();

            assertThat(definition.needsApproval()).isFalse();
        }

        @Test
        @DisplayName("should need approval for critical category")
        void needsApprovalForCritical() {
            ToolDefinition definition = ToolDefinition.builder()
                .name("test_tool")
                .approvalCategory(ToolDefinition.ApprovalCategory.CRITICAL)
                .build();

            assertThat(definition.needsApproval()).isTrue();
        }
    }

    @Nested
    @DisplayName("Format Conversion Tests")
    class FormatConversionTests {

        @Test
        @DisplayName("should convert to Claude API format")
        void toClaudeFormat() {
            Map<String, Object> inputSchema = Map.of(
                "type", "object",
                "properties", Map.of("query", Map.of("type", "string"))
            );

            ToolDefinition definition = ToolDefinition.builder()
                .name("search_tool")
                .description("Search for resources")
                .inputSchema(inputSchema)
                .build();

            Map<String, Object> claudeFormat = definition.toClaudeFormat();

            assertThat(claudeFormat).containsEntry("name", "search_tool");
            assertThat(claudeFormat).containsEntry("description", "Search for resources");
            assertThat(claudeFormat).containsEntry("input_schema", inputSchema);
        }

        @Test
        @DisplayName("should convert to OpenAI function format")
        void toOpenAIFormat() {
            Map<String, Object> inputSchema = Map.of(
                "type", "object",
                "properties", Map.of("query", Map.of("type", "string"))
            );

            ToolDefinition definition = ToolDefinition.builder()
                .name("search_tool")
                .description("Search for resources")
                .inputSchema(inputSchema)
                .build();

            Map<String, Object> openAIFormat = definition.toOpenAIFormat();

            assertThat(openAIFormat).containsEntry("type", "function");
            @SuppressWarnings("unchecked")
            Map<String, Object> function = (Map<String, Object>) openAIFormat.get("function");
            assertThat(function).containsEntry("name", "search_tool");
            assertThat(function).containsEntry("description", "Search for resources");
            assertThat(function).containsEntry("parameters", inputSchema);
        }
    }

    @Nested
    @DisplayName("Category Enum Tests")
    class CategoryEnumTests {

        @Test
        @DisplayName("should have all expected tool categories")
        void allToolCategories() {
            assertThat(ToolDefinition.ToolCategory.values())
                .containsExactlyInAnyOrder(
                    ToolDefinition.ToolCategory.FHIR_QUERY,
                    ToolDefinition.ToolCategory.CQL_EXECUTION,
                    ToolDefinition.ToolCategory.DATA_RETRIEVAL,
                    ToolDefinition.ToolCategory.DATA_MUTATION,
                    ToolDefinition.ToolCategory.NOTIFICATION,
                    ToolDefinition.ToolCategory.REPORTING,
                    ToolDefinition.ToolCategory.EXTERNAL_API
                );
        }

        @Test
        @DisplayName("should have all expected approval categories")
        void allApprovalCategories() {
            assertThat(ToolDefinition.ApprovalCategory.values())
                .containsExactlyInAnyOrder(
                    ToolDefinition.ApprovalCategory.NONE,
                    ToolDefinition.ApprovalCategory.LOW_RISK,
                    ToolDefinition.ApprovalCategory.STANDARD,
                    ToolDefinition.ApprovalCategory.CLINICAL,
                    ToolDefinition.ApprovalCategory.CRITICAL
                );
        }
    }
}
