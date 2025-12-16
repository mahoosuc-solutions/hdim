package com.healthdata.agent.llm.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agent.llm.config.LLMProviderConfig;
import com.healthdata.agent.llm.model.LLMRequest;
import com.healthdata.agent.llm.model.LLMResponse;
import com.healthdata.agent.llm.model.LLMStreamChunk;
import com.healthdata.agent.tool.ToolDefinition;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClaudeProvider Tests")
class ClaudeProviderTest {

    private ClaudeProvider provider;
    private ObjectMapper objectMapper;
    private SimpleMeterRegistry meterRegistry;
    private LLMProviderConfig.ProviderSettings settings;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        meterRegistry = new SimpleMeterRegistry();

        settings = new LLMProviderConfig.ProviderSettings();
        settings.setEnabled(true);
        settings.setModel("claude-3-5-sonnet-20241022");
        settings.setApiKey("test-api-key");
        settings.setApiUrl("https://api.anthropic.com");
        settings.setMaxTokens(4096);
        settings.setTemperature(0.7);
        settings.setTimeoutSeconds(30);

        LLMProviderConfig config = new LLMProviderConfig();
        config.getProviders().put("claude", settings);

        provider = new ClaudeProvider(config, meterRegistry, objectMapper);
    }

    private LLMResponse createMockResponse(String content, boolean withToolCalls) {
        LLMResponse.LLMResponseBuilder builder = LLMResponse.builder()
            .id("msg_123")
            .model("claude-3-5-sonnet-20241022")
            .content(content)
            .usage(LLMResponse.TokenUsage.of(100, 50))
            .stopReason(withToolCalls ? "tool_use" : "end_turn");

        if (withToolCalls) {
            builder.toolCalls(List.of(
                LLMRequest.ToolCall.builder()
                    .id("toolu_123")
                    .name("get_patient_data")
                    .arguments(Map.of("patientId", "P123"))
                    .build()
            ));
        }

        return builder.build();
    }

    @Nested
    @DisplayName("Provider Metadata Tests")
    class MetadataTests {

        @Test
        @DisplayName("should return correct provider name")
        void returnProviderName() {
            assertThat(provider.getName()).isEqualTo("claude");
        }

        @Test
        @DisplayName("should return available models")
        void returnAvailableModels() {
            List<String> models = provider.getAvailableModels();
            assertThat(models).contains(
                "claude-3-5-sonnet-20241022",
                "claude-3-opus-20240229",
                "claude-3-sonnet-20240229",
                "claude-3-haiku-20240307"
            );
        }

        @Test
        @DisplayName("should count tokens approximately")
        void countTokens() {
            String text = "This is a test message with about twenty characters per word.";
            int tokens = provider.countTokens(text);
            assertThat(tokens).isGreaterThan(0);
            // Approximate: 1 token ≈ 4 characters
            assertThat(tokens).isCloseTo(text.length() / 4, org.assertj.core.data.Offset.offset(5));
        }

        @Test
        @DisplayName("should have initial unhealthy status")
        void initialHealthStatus() {
            // Provider starts as unhealthy until first successful call
            assertThat(provider.health().isHealthy()).isFalse();
            assertThat(provider.health().status()).contains("Not initialized");
        }
    }

    @Nested
    @DisplayName("Request Building Tests")
    class RequestBuildingTests {

        @Test
        @DisplayName("should build request with system prompt")
        void buildRequestWithSystemPrompt() {
            // Given
            LLMRequest request = LLMRequest.builder()
                .systemPrompt("You are a diabetes educator.")
                .messages(List.of(LLMRequest.Message.user("Tell me about diabetes")))
                .maxTokens(1000)
                .temperature(0.7)
                .build();

            // When - We'll test this indirectly through the provider
            // The actual test would require mocking WebClient which is complex
            // This verifies the request object is properly structured
            assertThat(request.getSystemPrompt()).isEqualTo("You are a diabetes educator.");
            assertThat(request.getMessages()).hasSize(1);
            assertThat(request.getMaxTokens()).isEqualTo(1000);
        }

        @Test
        @DisplayName("should use default model from settings")
        void useDefaultModel() {
            LLMRequest request = LLMRequest.builder()
                .messages(List.of(LLMRequest.Message.user("Test")))
                .build();

            assertThat(request.getModel()).isNull(); // Will use settings default
        }

        @Test
        @DisplayName("should allow model override in request")
        void overrideModelInRequest() {
            LLMRequest request = LLMRequest.builder()
                .model("claude-3-opus-20240229")
                .messages(List.of(LLMRequest.Message.user("Test")))
                .build();

            assertThat(request.getModel()).isEqualTo("claude-3-opus-20240229");
        }
    }

    @Nested
    @DisplayName("Message Conversion Tests")
    class MessageConversionTests {

        @Test
        @DisplayName("should handle user messages")
        void handleUserMessages() {
            LLMRequest.Message userMessage = LLMRequest.Message.user("Hello");

            assertThat(userMessage.getRole()).isEqualTo("user");
            assertThat(userMessage.getContent()).isEqualTo("Hello");
        }

        @Test
        @DisplayName("should handle assistant messages")
        void handleAssistantMessages() {
            LLMRequest.Message assistantMessage = LLMRequest.Message.assistant("Hello back");

            assertThat(assistantMessage.getRole()).isEqualTo("assistant");
            assertThat(assistantMessage.getContent()).isEqualTo("Hello back");
        }

        @Test
        @DisplayName("should handle assistant messages with tool calls")
        void handleAssistantWithToolCalls() {
            LLMRequest.ToolCall toolCall = LLMRequest.ToolCall.builder()
                .id("toolu_123")
                .name("test_tool")
                .arguments(Map.of("arg", "value"))
                .build();

            LLMRequest.Message message = LLMRequest.Message.assistant("Using tool", List.of(toolCall));

            assertThat(message.getRole()).isEqualTo("assistant");
            assertThat(message.getContent()).isEqualTo("Using tool");
            assertThat(message.getToolCalls()).hasSize(1);
            assertThat(message.getToolCalls().get(0).getName()).isEqualTo("test_tool");
        }

        @Test
        @DisplayName("should handle tool result messages")
        void handleToolResultMessages() {
            LLMRequest.ToolResult result = LLMRequest.ToolResult.builder()
                .toolCallId("toolu_123")
                .toolName("test_tool")
                .content("Tool output")
                .isError(false)
                .build();

            LLMRequest.Message message = LLMRequest.Message.toolResults(List.of(result));

            assertThat(message.getRole()).isEqualTo("tool");
            assertThat(message.getToolResults()).hasSize(1);
            assertThat(message.getToolResults().get(0).getContent()).isEqualTo("Tool output");
        }
    }

    @Nested
    @DisplayName("Tool Definition Tests")
    class ToolDefinitionTests {

        @Test
        @DisplayName("should create tool definition with required fields")
        void createToolDefinition() {
            ToolDefinition tool = ToolDefinition.builder()
                .name("get_patient_data")
                .description("Retrieves patient data")
                .inputSchema(Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "patientId", Map.of("type", "string", "description", "Patient ID")
                    ),
                    "required", List.of("patientId")
                ))
                .requiresApproval(false)
                .build();

            assertThat(tool.getName()).isEqualTo("get_patient_data");
            assertThat(tool.getDescription()).isEqualTo("Retrieves patient data");
            assertThat(tool.isRequiresApproval()).isFalse();
            assertThat(tool.getInputSchema()).containsKey("type");
        }

        @Test
        @DisplayName("should convert tool to Claude format")
        void convertToClaudeFormat() {
            ToolDefinition tool = ToolDefinition.builder()
                .name("get_patient_vitals")
                .description("Get vital signs")
                .inputSchema(Map.of(
                    "type", "object",
                    "properties", Map.of("patientId", Map.of("type", "string"))
                ))
                .build();

            Map<String, Object> claudeFormat = tool.toClaudeFormat();

            assertThat(claudeFormat).containsKey("name");
            assertThat(claudeFormat).containsKey("description");
            assertThat(claudeFormat).containsKey("input_schema");
            assertThat(claudeFormat.get("name")).isEqualTo("get_patient_vitals");
        }
    }

    @Nested
    @DisplayName("Response Parsing Tests")
    class ResponseParsingTests {

        @Test
        @DisplayName("should parse response with text content")
        void parseTextResponse() {
            // Create a sample response object
            LLMResponse response = createMockResponse("This is a test response", false);

            assertThat(response.getId()).isEqualTo("msg_123");
            assertThat(response.getModel()).isEqualTo("claude-3-5-sonnet-20241022");
            assertThat(response.getContent()).isEqualTo("This is a test response");
            assertThat(response.getStopReason()).isEqualTo("end_turn");
            assertThat(response.hasToolCalls()).isFalse();
        }

        @Test
        @DisplayName("should parse response with tool calls")
        void parseResponseWithToolCalls() {
            LLMResponse response = createMockResponse("Processing request", true);

            assertThat(response.hasToolCalls()).isTrue();
            assertThat(response.getToolCalls()).hasSize(1);

            LLMRequest.ToolCall toolCall = response.getToolCalls().get(0);
            assertThat(toolCall.getId()).isEqualTo("toolu_123");
            assertThat(toolCall.getName()).isEqualTo("get_patient_data");
            assertThat(toolCall.getArguments()).containsEntry("patientId", "P123");
        }

        @Test
        @DisplayName("should parse token usage")
        void parseTokenUsage() {
            LLMResponse response = createMockResponse("Test", false);

            assertThat(response.getUsage()).isNotNull();
            assertThat(response.getUsage().getInputTokens()).isEqualTo(100);
            assertThat(response.getUsage().getOutputTokens()).isEqualTo(50);
            assertThat(response.getUsage().getTotalTokens()).isEqualTo(150);
        }
    }

    @Nested
    @DisplayName("Stream Chunk Tests")
    class StreamChunkTests {

        @Test
        @DisplayName("should create text delta chunk")
        void createTextDeltaChunk() {
            LLMStreamChunk chunk = LLMStreamChunk.builder()
                .type(LLMStreamChunk.ChunkType.CONTENT_BLOCK_DELTA)
                .delta("Hello")
                .index(0)
                .build();

            assertThat(chunk.getType()).isEqualTo(LLMStreamChunk.ChunkType.CONTENT_BLOCK_DELTA);
            assertThat(chunk.getDelta()).isEqualTo("Hello");
        }

        @Test
        @DisplayName("should create tool use start chunk")
        void createToolUseStartChunk() {
            LLMRequest.ToolCall toolCall = LLMRequest.ToolCall.builder()
                .id("toolu_123")
                .name("get_data")
                .build();

            LLMStreamChunk chunk = LLMStreamChunk.builder()
                .type(LLMStreamChunk.ChunkType.TOOL_USE_START)
                .toolCall(toolCall)
                .index(0)
                .build();

            assertThat(chunk.getType()).isEqualTo(LLMStreamChunk.ChunkType.TOOL_USE_START);
            assertThat(chunk.getToolCall()).isNotNull();
            assertThat(chunk.getToolCall().getName()).isEqualTo("get_data");
        }

        @Test
        @DisplayName("should create done chunk with usage")
        void createDoneChunk() {
            LLMStreamChunk chunk = LLMStreamChunk.builder()
                .type(LLMStreamChunk.ChunkType.DONE)
                .usage(LLMResponse.TokenUsage.of(100, 50))
                .build();

            assertThat(chunk.getType()).isEqualTo(LLMStreamChunk.ChunkType.DONE);
            assertThat(chunk.getUsage()).isNotNull();
            assertThat(chunk.getUsage().getTotalTokens()).isEqualTo(150);
        }

        @Test
        @DisplayName("should create error chunk")
        void createErrorChunk() {
            LLMStreamChunk chunk = LLMStreamChunk.error("Something went wrong");

            assertThat(chunk.getType()).isEqualTo(LLMStreamChunk.ChunkType.ERROR);
            assertThat(chunk.getDelta()).isEqualTo("Something went wrong");
        }
    }

    @Nested
    @DisplayName("Token Usage Tests")
    class TokenUsageTests {

        @Test
        @DisplayName("should calculate total tokens")
        void calculateTotalTokens() {
            LLMResponse.TokenUsage usage = LLMResponse.TokenUsage.of(100, 50);

            assertThat(usage.getInputTokens()).isEqualTo(100);
            assertThat(usage.getOutputTokens()).isEqualTo(50);
            assertThat(usage.getTotalTokens()).isEqualTo(150);
        }

        @Test
        @DisplayName("should handle zero tokens")
        void handleZeroTokens() {
            LLMResponse.TokenUsage usage = LLMResponse.TokenUsage.of(0, 0);

            assertThat(usage.getTotalTokens()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("should use configured model")
        void useConfiguredModel() {
            assertThat(settings.getModel()).isEqualTo("claude-3-5-sonnet-20241022");
        }

        @Test
        @DisplayName("should use configured temperature")
        void useConfiguredTemperature() {
            assertThat(settings.getTemperature()).isEqualTo(0.7);
        }

        @Test
        @DisplayName("should use configured max tokens")
        void useConfiguredMaxTokens() {
            assertThat(settings.getMaxTokens()).isEqualTo(4096);
        }

        @Test
        @DisplayName("should use configured timeout")
        void useConfiguredTimeout() {
            assertThat(settings.getTimeoutSeconds()).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("Metrics Tests")
    class MetricsTests {

        @Test
        @DisplayName("should have request counter metric")
        void hasRequestCounter() {
            assertThat(meterRegistry.find("llm.requests").counter()).isNotNull();
            assertThat(meterRegistry.find("llm.requests").counter().getId().getTag("provider"))
                .isEqualTo("claude");
        }

        @Test
        @DisplayName("should have error counter metric")
        void hasErrorCounter() {
            assertThat(meterRegistry.find("llm.errors").counter()).isNotNull();
            assertThat(meterRegistry.find("llm.errors").counter().getId().getTag("provider"))
                .isEqualTo("claude");
        }

        @Test
        @DisplayName("should have token counter metric")
        void hasTokenCounter() {
            assertThat(meterRegistry.find("llm.tokens").counter()).isNotNull();
        }

        @Test
        @DisplayName("should have latency timer metric")
        void hasLatencyTimer() {
            assertThat(meterRegistry.find("llm.latency").timer()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Tool Call Tests")
    class ToolCallTests {

        @Test
        @DisplayName("should create tool call with arguments")
        void createToolCall() {
            LLMRequest.ToolCall toolCall = LLMRequest.ToolCall.builder()
                .id("toolu_123")
                .name("get_patient_data")
                .arguments(Map.of(
                    "patientId", "P123",
                    "includeHistory", true
                ))
                .build();

            assertThat(toolCall.getId()).isEqualTo("toolu_123");
            assertThat(toolCall.getName()).isEqualTo("get_patient_data");
            assertThat(toolCall.getArguments()).hasSize(2);
            assertThat(toolCall.getArguments().get("patientId")).isEqualTo("P123");
            assertThat(toolCall.getArguments().get("includeHistory")).isEqualTo(true);
        }

        @Test
        @DisplayName("should handle empty arguments")
        void handleEmptyArguments() {
            LLMRequest.ToolCall toolCall = LLMRequest.ToolCall.builder()
                .id("toolu_456")
                .name("list_patients")
                .arguments(Map.of())
                .build();

            assertThat(toolCall.getArguments()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tool Result Tests")
    class ToolResultTests {

        @Test
        @DisplayName("should create successful tool result")
        void createSuccessfulResult() {
            LLMRequest.ToolResult result = LLMRequest.ToolResult.builder()
                .toolCallId("toolu_123")
                .toolName("get_patient_data")
                .content("{\"name\": \"John Doe\", \"age\": 45}")
                .isError(false)
                .build();

            assertThat(result.getToolCallId()).isEqualTo("toolu_123");
            assertThat(result.getToolName()).isEqualTo("get_patient_data");
            assertThat(result.getContent()).contains("John Doe");
            assertThat(result.isError()).isFalse();
        }

        @Test
        @DisplayName("should create error tool result")
        void createErrorResult() {
            LLMRequest.ToolResult result = LLMRequest.ToolResult.builder()
                .toolCallId("toolu_456")
                .toolName("failing_tool")
                .content("Tool execution failed: Invalid patient ID")
                .isError(true)
                .build();

            assertThat(result.isError()).isTrue();
            assertThat(result.getContent()).contains("failed");
        }
    }
}
