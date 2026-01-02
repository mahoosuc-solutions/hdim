package com.healthdata.agent.llm.providers;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
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

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Azure OpenAI LLM provider implementation.
 * Supports GPT-4, GPT-4 Turbo, and GPT-3.5 Turbo deployments.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "hdim.agent.llm.providers.azure-openai.enabled", havingValue = "true")
public class AzureOpenAIProvider extends AbstractLLMProvider {

    private static final String PROVIDER_NAME = "azure-openai";
    private static final List<String> AVAILABLE_MODELS = List.of(
        "gpt-4",
        "gpt-4-turbo",
        "gpt-4o",
        "gpt-4o-mini",
        "gpt-35-turbo"
    );

    private final OpenAIAsyncClient client;
    private final String deploymentId;

    public AzureOpenAIProvider(
            LLMProviderConfig config,
            MeterRegistry meterRegistry) {
        super(config.getProvider(PROVIDER_NAME), meterRegistry);

        this.deploymentId = settings.getDeploymentId();

        this.client = new OpenAIClientBuilder()
            .endpoint(settings.getEndpoint())
            .credential(new AzureKeyCredential(settings.getApiKey()))
            .buildAsyncClient();

        log.info("Initialized Azure OpenAI provider with deployment: {}", deploymentId);
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
        ChatCompletionsOptions options = buildChatOptions(request, null);

        ChatCompletions completions = client.getChatCompletions(deploymentId, options)
            .timeout(Duration.ofSeconds(settings.getTimeoutSeconds()))
            .block();

        return parseResponse(completions);
    }

    @Override
    protected Flux<LLMStreamChunk> doCompleteStreaming(LLMRequest request) {
        ChatCompletionsOptions options = buildChatOptions(request, null);

        return client.getChatCompletionsStream(deploymentId, options)
            .timeout(Duration.ofSeconds(settings.getTimeoutSeconds()))
            .map(this::parseStreamUpdate)
            .onErrorResume(e -> {
                log.error("Streaming error from Azure OpenAI: {}", e.getMessage());
                return Flux.just(LLMStreamChunk.error(e.getMessage()));
            });
    }

    @Override
    protected LLMResponse doCompleteWithTools(LLMRequest request, List<ToolDefinition> tools) {
        ChatCompletionsOptions options = buildChatOptions(request, tools);

        ChatCompletions completions = client.getChatCompletions(deploymentId, options)
            .timeout(Duration.ofSeconds(settings.getTimeoutSeconds()))
            .block();

        return parseResponse(completions);
    }

    private ChatCompletionsOptions buildChatOptions(LLMRequest request, List<ToolDefinition> tools) {
        List<ChatRequestMessage> messages = convertMessages(request);

        ChatCompletionsOptions options = new ChatCompletionsOptions(messages);
        options.setMaxTokens(request.getMaxTokens() > 0 ? request.getMaxTokens() : settings.getMaxTokens());
        options.setTemperature(request.getTemperature() > 0 ? request.getTemperature() : settings.getTemperature());

        if (request.getStopSequences() != null && !request.getStopSequences().isEmpty()) {
            options.setStop(request.getStopSequences());
        }

        // Add tools if provided
        if (tools != null && !tools.isEmpty()) {
            List<ChatCompletionsFunctionToolDefinition> toolDefinitions = tools.stream()
                .map(this::convertToolDefinition)
                .collect(Collectors.toList());
            options.setTools(new ArrayList<>(toolDefinitions));
        }

        // User identifier for tracking
        if (request.getTenantId() != null) {
            options.setUser(request.getTenantId());
        }

        return options;
    }

    private List<ChatRequestMessage> convertMessages(LLMRequest request) {
        List<ChatRequestMessage> messages = new ArrayList<>();

        // System prompt
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            messages.add(new ChatRequestSystemMessage(request.getSystemPrompt()));
        }

        // Conversation messages
        if (request.getMessages() != null) {
            for (LLMRequest.Message msg : request.getMessages()) {
                switch (msg.getRole()) {
                    case "user" -> messages.add(new ChatRequestUserMessage(msg.getContent()));
                    case "assistant" -> {
                        ChatRequestAssistantMessage assistantMsg = new ChatRequestAssistantMessage(msg.getContent());
                        if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                            List<ChatCompletionsToolCall> toolCalls = msg.getToolCalls().stream()
                                .map(tc -> new ChatCompletionsFunctionToolCall(
                                    tc.getId(),
                                    new FunctionCall(tc.getName(), serializeArguments(tc.getArguments()))
                                ))
                                .collect(Collectors.toList());
                            assistantMsg.setToolCalls(toolCalls);
                        }
                        messages.add(assistantMsg);
                    }
                    case "tool" -> {
                        if (msg.getToolResults() != null) {
                            for (LLMRequest.ToolResult result : msg.getToolResults()) {
                                messages.add(new ChatRequestToolMessage(result.getContent(), result.getToolCallId()));
                            }
                        }
                    }
                    case "system" -> messages.add(new ChatRequestSystemMessage(msg.getContent()));
                }
            }
        }

        return messages;
    }

    private ChatCompletionsFunctionToolDefinition convertToolDefinition(ToolDefinition tool) {
        FunctionDefinition function = new FunctionDefinition(tool.getName());
        function.setDescription(tool.getDescription());
        function.setParameters(BinaryData.fromObject(tool.getInputSchema()));
        return new ChatCompletionsFunctionToolDefinition(function);
    }

    private String serializeArguments(Map<String, Object> arguments) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(arguments);
        } catch (Exception e) {
            return "{}";
        }
    }

    private LLMResponse parseResponse(ChatCompletions completions) {
        if (completions == null || completions.getChoices().isEmpty()) {
            throw new LLMProviderException("Empty response from Azure OpenAI");
        }

        ChatChoice choice = completions.getChoices().get(0);
        ChatResponseMessage message = choice.getMessage();

        // Extract content
        String content = message.getContent();

        // Extract tool calls
        List<LLMRequest.ToolCall> toolCalls = null;
        if (message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
            toolCalls = message.getToolCalls().stream()
                .filter(tc -> tc instanceof ChatCompletionsFunctionToolCall)
                .map(tc -> {
                    ChatCompletionsFunctionToolCall funcCall = (ChatCompletionsFunctionToolCall) tc;
                    return LLMRequest.ToolCall.builder()
                        .id(funcCall.getId())
                        .name(funcCall.getFunction().getName())
                        .arguments(parseArguments(funcCall.getFunction().getArguments()))
                        .build();
                })
                .collect(Collectors.toList());
        }

        // Extract usage
        CompletionsUsage usage = completions.getUsage();
        LLMResponse.TokenUsage tokenUsage = LLMResponse.TokenUsage.of(
            usage.getPromptTokens(),
            usage.getCompletionTokens()
        );

        // Map finish reason
        String stopReason = mapFinishReason(choice.getFinishReason());

        return LLMResponse.builder()
            .id(completions.getId())
            .model(completions.getModel())
            .content(content != null ? content : "")
            .toolCalls(toolCalls)
            .stopReason(stopReason)
            .usage(tokenUsage)
            .build();
    }

    private Map<String, Object> parseArguments(String arguments) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(arguments, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String mapFinishReason(CompletionsFinishReason reason) {
        if (reason == null) return "unknown";
        String value = reason.toString().toLowerCase();
        return switch (value) {
            case "stop", "stopped" -> "end_turn";
            case "length", "token_limit_reached" -> "max_tokens";
            case "tool_calls", "function_call" -> "tool_use";
            case "content_filter", "content_filtered" -> "content_filtered";
            default -> value;
        };
    }

    private LLMStreamChunk parseStreamUpdate(ChatCompletions update) {
        if (update.getChoices() == null || update.getChoices().isEmpty()) {
            return LLMStreamChunk.builder()
                .type(LLMStreamChunk.ChunkType.MESSAGE_START)
                .build();
        }

        ChatChoice choice = update.getChoices().get(0);

        // Check for completion
        if (choice.getFinishReason() != null) {
            CompletionsUsage usage = update.getUsage();
            LLMResponse.TokenUsage tokenUsage = usage != null
                ? LLMResponse.TokenUsage.of(usage.getPromptTokens(), usage.getCompletionTokens())
                : null;

            return LLMStreamChunk.done(tokenUsage, mapFinishReason(choice.getFinishReason()));
        }

        ChatResponseMessage delta = choice.getDelta();
        if (delta == null) {
            return LLMStreamChunk.textDelta("");
        }

        // Handle tool calls in stream
        if (delta.getToolCalls() != null && !delta.getToolCalls().isEmpty()) {
            ChatCompletionsToolCall tc = delta.getToolCalls().get(0);
            if (tc instanceof ChatCompletionsFunctionToolCall funcCall) {
                if (funcCall.getFunction().getName() != null) {
                    return LLMStreamChunk.builder()
                        .type(LLMStreamChunk.ChunkType.TOOL_USE_START)
                        .toolCall(LLMRequest.ToolCall.builder()
                            .id(funcCall.getId())
                            .name(funcCall.getFunction().getName())
                            .build())
                        .build();
                } else {
                    return LLMStreamChunk.builder()
                        .type(LLMStreamChunk.ChunkType.TOOL_USE_DELTA)
                        .delta(funcCall.getFunction().getArguments())
                        .build();
                }
            }
        }

        // Handle text content
        String content = delta.getContent();
        return LLMStreamChunk.textDelta(content != null ? content : "");
    }
}
