package com.healthdata.agent.llm.providers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agent.llm.config.LLMProviderConfig;
import com.healthdata.agent.llm.model.LLMRequest;
import com.healthdata.agent.llm.model.LLMResponse;
import com.healthdata.agent.llm.model.LLMStreamChunk;
import com.healthdata.agent.tool.ToolDefinition;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Claude (Anthropic) LLM provider implementation.
 * Supports Claude 3.5 Sonnet, Claude 3 Opus, and Haiku models.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "hdim.agent.llm.providers.claude.enabled", havingValue = "true")
public class ClaudeProvider extends AbstractLLMProvider {

    private static final String PROVIDER_NAME = "claude";
    private static final String DEFAULT_API_URL = "https://api.anthropic.com";
    private static final String ANTHROPIC_VERSION = "2024-01-01";
    private static final List<String> AVAILABLE_MODELS = List.of(
        "claude-3-5-sonnet-20241022",
        "claude-3-opus-20240229",
        "claude-3-sonnet-20240229",
        "claude-3-haiku-20240307"
    );

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ClaudeProvider(
            LLMProviderConfig config,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper) {
        super(config.getProvider(PROVIDER_NAME), meterRegistry);
        this.objectMapper = objectMapper;

        String apiUrl = settings.getApiUrl() != null ? settings.getApiUrl() : DEFAULT_API_URL;

        this.webClient = WebClient.builder()
            .baseUrl(apiUrl)
            .defaultHeader("x-api-key", settings.getApiKey())
            .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();

        log.info("Initialized Claude provider with model: {}", settings.getModel());
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public List<String> getAvailableModels() {
        return AVAILABLE_MODELS;
    }

    @Override
    protected LLMResponse doComplete(LLMRequest request) {
        Map<String, Object> requestBody = buildRequestBody(request, null);

        String responseJson = webClient.post()
            .uri("/v1/messages")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(settings.getTimeoutSeconds()))
            .block();

        return parseResponse(responseJson);
    }

    @Override
    protected Flux<LLMStreamChunk> doCompleteStreaming(LLMRequest request) {
        Map<String, Object> requestBody = buildRequestBody(request, null);
        requestBody.put("stream", true);

        return webClient.post()
            .uri("/v1/messages")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToFlux(String.class)
            .timeout(Duration.ofSeconds(settings.getTimeoutSeconds()))
            .filter(line -> line.startsWith("data: "))
            .map(line -> line.substring(6))
            .filter(data -> !data.equals("[DONE]"))
            .map(this::parseStreamChunk)
            .onErrorResume(e -> {
                log.error("Streaming error from Claude: {}", e.getMessage());
                return Flux.just(LLMStreamChunk.error(e.getMessage()));
            });
    }

    @Override
    protected LLMResponse doCompleteWithTools(LLMRequest request, List<ToolDefinition> tools) {
        Map<String, Object> requestBody = buildRequestBody(request, tools);

        String responseJson = webClient.post()
            .uri("/v1/messages")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(settings.getTimeoutSeconds()))
            .block();

        return parseResponse(responseJson);
    }

    private Map<String, Object> buildRequestBody(LLMRequest request, List<ToolDefinition> tools) {
        Map<String, Object> body = new LinkedHashMap<>();

        // Model selection
        String model = request.getModel() != null ? request.getModel() : settings.getModel();
        body.put("model", model);
        body.put("max_tokens", request.getMaxTokens() > 0 ? request.getMaxTokens() : settings.getMaxTokens());

        // System prompt
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            body.put("system", request.getSystemPrompt());
        }

        // Messages - convert to Claude format
        List<Map<String, Object>> messages = convertMessages(request.getMessages());
        body.put("messages", messages);

        // Temperature
        body.put("temperature", request.getTemperature() > 0 ? request.getTemperature() : settings.getTemperature());

        // Stop sequences
        if (request.getStopSequences() != null && !request.getStopSequences().isEmpty()) {
            body.put("stop_sequences", request.getStopSequences());
        }

        // Tools
        if (tools != null && !tools.isEmpty()) {
            List<Map<String, Object>> toolsJson = tools.stream()
                .map(ToolDefinition::toClaudeFormat)
                .collect(Collectors.toList());
            body.put("tools", toolsJson);
        }

        // Metadata
        if (request.getTenantId() != null || request.getSessionId() != null) {
            Map<String, String> metadata = new HashMap<>();
            if (request.getTenantId() != null) {
                metadata.put("tenant_id", request.getTenantId());
            }
            if (request.getSessionId() != null) {
                metadata.put("session_id", request.getSessionId());
            }
            body.put("metadata", Map.of("user_id", metadata.toString()));
        }

        return body;
    }

    private List<Map<String, Object>> convertMessages(List<LLMRequest.Message> messages) {
        if (messages == null) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (LLMRequest.Message msg : messages) {
            Map<String, Object> converted = new LinkedHashMap<>();
            converted.put("role", msg.getRole().equals("tool") ? "user" : msg.getRole());

            // Handle tool results
            if (msg.getToolResults() != null && !msg.getToolResults().isEmpty()) {
                List<Map<String, Object>> content = new ArrayList<>();
                for (LLMRequest.ToolResult tr : msg.getToolResults()) {
                    Map<String, Object> toolResultBlock = new LinkedHashMap<>();
                    toolResultBlock.put("type", "tool_result");
                    toolResultBlock.put("tool_use_id", tr.getToolCallId());
                    toolResultBlock.put("content", tr.getContent());
                    if (tr.isError()) {
                        toolResultBlock.put("is_error", true);
                    }
                    content.add(toolResultBlock);
                }
                converted.put("content", content);
            }
            // Handle tool calls in assistant message
            else if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                List<Map<String, Object>> content = new ArrayList<>();
                if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                    content.add(Map.of("type", "text", "text", msg.getContent()));
                }
                for (LLMRequest.ToolCall tc : msg.getToolCalls()) {
                    Map<String, Object> toolUseBlock = new LinkedHashMap<>();
                    toolUseBlock.put("type", "tool_use");
                    toolUseBlock.put("id", tc.getId());
                    toolUseBlock.put("name", tc.getName());
                    toolUseBlock.put("input", tc.getArguments());
                    content.add(toolUseBlock);
                }
                converted.put("content", content);
            }
            // Regular text message
            else {
                converted.put("content", msg.getContent());
            }

            result.add(converted);
        }
        return result;
    }

    private LLMResponse parseResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);

            String id = root.path("id").asText();
            String model = root.path("model").asText();
            String stopReason = root.path("stop_reason").asText();

            // Extract content
            StringBuilder textContent = new StringBuilder();
            List<LLMRequest.ToolCall> toolCalls = new ArrayList<>();

            JsonNode contentArray = root.path("content");
            if (contentArray.isArray()) {
                for (JsonNode block : contentArray) {
                    String type = block.path("type").asText();
                    if ("text".equals(type)) {
                        textContent.append(block.path("text").asText());
                    } else if ("tool_use".equals(type)) {
                        LLMRequest.ToolCall toolCall = LLMRequest.ToolCall.builder()
                            .id(block.path("id").asText())
                            .name(block.path("name").asText())
                            .arguments(objectMapper.convertValue(block.path("input"), Map.class))
                            .build();
                        toolCalls.add(toolCall);
                    }
                }
            }

            // Extract usage
            JsonNode usageNode = root.path("usage");
            LLMResponse.TokenUsage usage = LLMResponse.TokenUsage.of(
                usageNode.path("input_tokens").asInt(0),
                usageNode.path("output_tokens").asInt(0)
            );

            return LLMResponse.builder()
                .id(id)
                .model(model)
                .content(textContent.toString())
                .toolCalls(toolCalls.isEmpty() ? null : toolCalls)
                .stopReason(stopReason)
                .usage(usage)
                .build();

        } catch (JsonProcessingException e) {
            throw new LLMProviderException("Failed to parse Claude response: " + e.getMessage(), e);
        }
    }

    private LLMStreamChunk parseStreamChunk(String data) {
        try {
            JsonNode root = objectMapper.readTree(data);
            String eventType = root.path("type").asText();

            return switch (eventType) {
                case "message_start" -> LLMStreamChunk.builder()
                    .type(LLMStreamChunk.ChunkType.MESSAGE_START)
                    .build();
                case "content_block_start" -> {
                    JsonNode block = root.path("content_block");
                    String blockType = block.path("type").asText();
                    if ("tool_use".equals(blockType)) {
                        yield LLMStreamChunk.builder()
                            .type(LLMStreamChunk.ChunkType.TOOL_USE_START)
                            .index(root.path("index").asInt())
                            .toolCall(LLMRequest.ToolCall.builder()
                                .id(block.path("id").asText())
                                .name(block.path("name").asText())
                                .build())
                            .build();
                    }
                    yield LLMStreamChunk.builder()
                        .type(LLMStreamChunk.ChunkType.CONTENT_BLOCK_START)
                        .index(root.path("index").asInt())
                        .build();
                }
                case "content_block_delta" -> {
                    JsonNode delta = root.path("delta");
                    String deltaType = delta.path("type").asText();
                    if ("input_json_delta".equals(deltaType)) {
                        yield LLMStreamChunk.builder()
                            .type(LLMStreamChunk.ChunkType.TOOL_USE_DELTA)
                            .delta(delta.path("partial_json").asText())
                            .index(root.path("index").asInt())
                            .build();
                    }
                    yield LLMStreamChunk.builder()
                        .type(LLMStreamChunk.ChunkType.CONTENT_BLOCK_DELTA)
                        .delta(delta.path("text").asText())
                        .index(root.path("index").asInt())
                        .build();
                }
                case "content_block_stop" -> LLMStreamChunk.builder()
                    .type(LLMStreamChunk.ChunkType.CONTENT_BLOCK_STOP)
                    .index(root.path("index").asInt())
                    .build();
                case "message_delta" -> {
                    JsonNode delta = root.path("delta");
                    JsonNode usage = root.path("usage");
                    yield LLMStreamChunk.builder()
                        .type(LLMStreamChunk.ChunkType.MESSAGE_DELTA)
                        .stopReason(delta.path("stop_reason").asText(null))
                        .usage(LLMResponse.TokenUsage.of(0, usage.path("output_tokens").asInt(0)))
                        .build();
                }
                case "message_stop" -> LLMStreamChunk.builder()
                    .type(LLMStreamChunk.ChunkType.DONE)
                    .build();
                case "error" -> LLMStreamChunk.error(root.path("error").path("message").asText("Unknown error"));
                default -> LLMStreamChunk.builder()
                    .type(LLMStreamChunk.ChunkType.CONTENT_BLOCK_DELTA)
                    .delta("")
                    .build();
            };
        } catch (JsonProcessingException e) {
            return LLMStreamChunk.error("Failed to parse stream chunk: " + e.getMessage());
        }
    }

    @Override
    public int countTokens(String text) {
        // Claude uses a similar tokenization to GPT models
        // Approximate: 1 token ≈ 4 characters for English
        return (int) Math.ceil(text.length() / 4.0);
    }
}
