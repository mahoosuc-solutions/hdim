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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AWS Bedrock LLM provider implementation.
 * Supports Claude models via Bedrock (Claude 3.5 Sonnet, Claude 3 Opus, Haiku).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "hdim.agent.llm.providers.bedrock.enabled", havingValue = "true")
public class BedrockProvider extends AbstractLLMProvider {

    private static final String PROVIDER_NAME = "bedrock";
    private static final List<String> AVAILABLE_MODELS = List.of(
        "anthropic.claude-3-5-sonnet-20241022-v2:0",
        "anthropic.claude-3-opus-20240229-v1:0",
        "anthropic.claude-3-sonnet-20240229-v1:0",
        "anthropic.claude-3-haiku-20240307-v1:0",
        "amazon.titan-text-express-v1",
        "amazon.titan-text-lite-v1"
    );

    private final BedrockRuntimeClient syncClient;
    private final BedrockRuntimeAsyncClient asyncClient;
    private final ObjectMapper objectMapper;
    private final String modelId;

    public BedrockProvider(
            LLMProviderConfig config,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper) {
        super(config.getProvider(PROVIDER_NAME), meterRegistry);
        this.objectMapper = objectMapper;
        this.modelId = settings.getModelId() != null ? settings.getModelId() : "anthropic.claude-3-5-sonnet-20241022-v2:0";

        Region region = Region.of(settings.getRegion() != null ? settings.getRegion() : "us-east-1");

        this.syncClient = BedrockRuntimeClient.builder()
            .region(region)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();

        this.asyncClient = BedrockRuntimeAsyncClient.builder()
            .region(region)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();

        log.info("Initialized AWS Bedrock provider with model: {} in region: {}", modelId, region);
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
        String requestBody = buildRequestBody(request, null);

        InvokeModelRequest invokeRequest = InvokeModelRequest.builder()
            .modelId(getModelId(request))
            .contentType("application/json")
            .accept("application/json")
            .body(SdkBytes.fromUtf8String(requestBody))
            .build();

        InvokeModelResponse response = syncClient.invokeModel(invokeRequest);
        String responseBody = response.body().asUtf8String();

        return parseResponse(responseBody, getModelId(request));
    }

    @Override
    protected Flux<LLMStreamChunk> doCompleteStreaming(LLMRequest request) {
        String requestBody = buildRequestBody(request, null);

        InvokeModelWithResponseStreamRequest streamRequest = InvokeModelWithResponseStreamRequest.builder()
            .modelId(getModelId(request))
            .contentType("application/json")
            .accept("application/json")
            .body(SdkBytes.fromUtf8String(requestBody))
            .build();

        Sinks.Many<LLMStreamChunk> sink = Sinks.many().unicast().onBackpressureBuffer();

        asyncClient.invokeModelWithResponseStream(streamRequest,
            InvokeModelWithResponseStreamResponseHandler.builder()
                .onEventStream(stream -> {
                    stream.subscribe(event -> {
                        if (event instanceof PayloadPart payloadPart) {
                            String chunk = payloadPart.bytes().asUtf8String();
                            LLMStreamChunk streamChunk = parseStreamChunk(chunk);
                            sink.tryEmitNext(streamChunk);
                        }
                    });
                })
                .onComplete(() -> sink.tryEmitComplete())
                .onError(err -> sink.tryEmitError(err))
                .build()
        );

        return sink.asFlux()
            .timeout(Duration.ofSeconds(settings.getTimeoutSeconds()))
            .onErrorResume(e -> {
                log.error("Streaming error from Bedrock: {}", e.getMessage());
                return Flux.just(LLMStreamChunk.error(e.getMessage()));
            });
    }

    @Override
    protected LLMResponse doCompleteWithTools(LLMRequest request, List<ToolDefinition> tools) {
        String requestBody = buildRequestBody(request, tools);

        InvokeModelRequest invokeRequest = InvokeModelRequest.builder()
            .modelId(getModelId(request))
            .contentType("application/json")
            .accept("application/json")
            .body(SdkBytes.fromUtf8String(requestBody))
            .build();

        InvokeModelResponse response = syncClient.invokeModel(invokeRequest);
        String responseBody = response.body().asUtf8String();

        return parseResponse(responseBody, getModelId(request));
    }

    private String getModelId(LLMRequest request) {
        return request.getModel() != null ? request.getModel() : modelId;
    }

    private String buildRequestBody(LLMRequest request, List<ToolDefinition> tools) {
        String model = getModelId(request);

        // Route to appropriate format based on model provider
        if (model.startsWith("anthropic.")) {
            return buildClaudeRequestBody(request, tools);
        } else if (model.startsWith("amazon.titan")) {
            return buildTitanRequestBody(request);
        } else {
            throw new LLMProviderException("Unsupported Bedrock model: " + model);
        }
    }

    private String buildClaudeRequestBody(LLMRequest request, List<ToolDefinition> tools) {
        Map<String, Object> body = new LinkedHashMap<>();

        // Anthropic version for Bedrock
        body.put("anthropic_version", "bedrock-2023-05-31");
        body.put("max_tokens", request.getMaxTokens() > 0 ? request.getMaxTokens() : settings.getMaxTokens());

        // System prompt
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            body.put("system", request.getSystemPrompt());
        }

        // Messages
        List<Map<String, Object>> messages = convertMessagesToClaudeFormat(request.getMessages());
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

        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new LLMProviderException("Failed to serialize request: " + e.getMessage(), e);
        }
    }

    private String buildTitanRequestBody(LLMRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();

        // Build prompt from system + messages
        StringBuilder prompt = new StringBuilder();
        if (request.getSystemPrompt() != null) {
            prompt.append("System: ").append(request.getSystemPrompt()).append("\n\n");
        }
        if (request.getMessages() != null) {
            for (LLMRequest.Message msg : request.getMessages()) {
                String role = msg.getRole().equals("user") ? "User" : "Assistant";
                prompt.append(role).append(": ").append(msg.getContent()).append("\n\n");
            }
        }
        prompt.append("Assistant:");

        body.put("inputText", prompt.toString());

        Map<String, Object> textGenerationConfig = new LinkedHashMap<>();
        textGenerationConfig.put("maxTokenCount", request.getMaxTokens() > 0 ? request.getMaxTokens() : settings.getMaxTokens());
        textGenerationConfig.put("temperature", request.getTemperature() > 0 ? request.getTemperature() : settings.getTemperature());
        if (request.getStopSequences() != null) {
            textGenerationConfig.put("stopSequences", request.getStopSequences());
        }
        body.put("textGenerationConfig", textGenerationConfig);

        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new LLMProviderException("Failed to serialize request: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> convertMessagesToClaudeFormat(List<LLMRequest.Message> messages) {
        if (messages == null) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (LLMRequest.Message msg : messages) {
            Map<String, Object> converted = new LinkedHashMap<>();
            converted.put("role", msg.getRole().equals("tool") ? "user" : msg.getRole());

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
            } else if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
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
            } else {
                converted.put("content", msg.getContent());
            }

            result.add(converted);
        }
        return result;
    }

    private LLMResponse parseResponse(String responseBody, String model) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            if (model.startsWith("anthropic.")) {
                return parseClaudeResponse(root);
            } else if (model.startsWith("amazon.titan")) {
                return parseTitanResponse(root);
            } else {
                throw new LLMProviderException("Unsupported model response format: " + model);
            }
        } catch (JsonProcessingException e) {
            throw new LLMProviderException("Failed to parse Bedrock response: " + e.getMessage(), e);
        }
    }

    private LLMResponse parseClaudeResponse(JsonNode root) {
        String id = root.path("id").asText();
        String model = root.path("model").asText();
        String stopReason = root.path("stop_reason").asText();

        StringBuilder textContent = new StringBuilder();
        List<LLMRequest.ToolCall> toolCalls = new ArrayList<>();

        JsonNode contentArray = root.path("content");
        if (contentArray.isArray()) {
            for (JsonNode block : contentArray) {
                String type = block.path("type").asText();
                if ("text".equals(type)) {
                    textContent.append(block.path("text").asText());
                } else if ("tool_use".equals(type)) {
                    try {
                        LLMRequest.ToolCall toolCall = LLMRequest.ToolCall.builder()
                            .id(block.path("id").asText())
                            .name(block.path("name").asText())
                            .arguments(objectMapper.convertValue(block.path("input"), Map.class))
                            .build();
                        toolCalls.add(toolCall);
                    } catch (Exception e) {
                        log.warn("Failed to parse tool call: {}", e.getMessage());
                    }
                }
            }
        }

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
    }

    private LLMResponse parseTitanResponse(JsonNode root) {
        JsonNode results = root.path("results");
        String content = "";
        int outputTokens = 0;

        if (results.isArray() && results.size() > 0) {
            JsonNode firstResult = results.get(0);
            content = firstResult.path("outputText").asText("");
            outputTokens = firstResult.path("tokenCount").asInt(0);
        }

        int inputTokens = root.path("inputTextTokenCount").asInt(0);

        return LLMResponse.builder()
            .id(UUID.randomUUID().toString())
            .model("titan")
            .content(content)
            .stopReason("end_turn")
            .usage(LLMResponse.TokenUsage.of(inputTokens, outputTokens))
            .build();
    }

    private LLMStreamChunk parseStreamChunk(String chunk) {
        try {
            JsonNode root = objectMapper.readTree(chunk);

            // Claude format via Bedrock
            if (root.has("type")) {
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
                    default -> LLMStreamChunk.textDelta("");
                };
            }

            // Titan format
            if (root.has("outputText")) {
                return LLMStreamChunk.textDelta(root.path("outputText").asText(""));
            }

            return LLMStreamChunk.textDelta("");

        } catch (JsonProcessingException e) {
            return LLMStreamChunk.error("Failed to parse stream chunk: " + e.getMessage());
        }
    }
}
