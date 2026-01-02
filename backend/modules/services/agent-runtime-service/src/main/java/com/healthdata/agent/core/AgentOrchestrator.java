package com.healthdata.agent.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agent.approval.ApprovalIntegration;
import com.healthdata.agent.approval.ApprovalIntegration.ApprovalResult;
import com.healthdata.agent.llm.LLMProvider;
import com.healthdata.agent.llm.LLMProviderFactory;
import com.healthdata.agent.llm.model.LLMRequest;
import com.healthdata.agent.llm.model.LLMResponse;
import com.healthdata.agent.llm.model.LLMStreamChunk;
import com.healthdata.agent.memory.CompositeMemoryManager;
import com.healthdata.agent.memory.MemoryManager;
import com.healthdata.agent.tool.Tool;
import com.healthdata.agent.tool.ToolDefinition;
import com.healthdata.agent.tool.ToolRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Core orchestrator for AI agent execution.
 * Manages the agent loop: prompt -> LLM -> tool execution -> response.
 */
@Slf4j
@Service
public class AgentOrchestrator {

    private final LLMProviderFactory llmProviderFactory;
    private final ToolRegistry toolRegistry;
    private final CompositeMemoryManager memoryManager;
    private final ObjectMapper objectMapper;
    private final GuardrailService guardrailService;
    private final ApprovalIntegration approvalIntegration;

    // Metrics
    private final Counter taskCounter;
    private final Counter toolInvocationCounter;
    private final Timer taskTimer;

    // Configuration
    @Value("${hdim.agent.orchestrator.max-iterations:10}")
    private int maxIterations;

    @Value("${hdim.agent.orchestrator.timeout-seconds:120}")
    private int timeoutSeconds;

    // Active tasks tracking
    private final Map<String, AgentTask> activeTasks = new ConcurrentHashMap<>();

    public AgentOrchestrator(
            LLMProviderFactory llmProviderFactory,
            ToolRegistry toolRegistry,
            CompositeMemoryManager memoryManager,
            ObjectMapper objectMapper,
            GuardrailService guardrailService,
            ApprovalIntegration approvalIntegration,
            MeterRegistry meterRegistry) {
        this.llmProviderFactory = llmProviderFactory;
        this.toolRegistry = toolRegistry;
        this.memoryManager = memoryManager;
        this.objectMapper = objectMapper;
        this.guardrailService = guardrailService;
        this.approvalIntegration = approvalIntegration;

        this.taskCounter = Counter.builder("agent.tasks")
            .description("Number of agent tasks executed")
            .register(meterRegistry);

        this.toolInvocationCounter = Counter.builder("agent.tool_invocations")
            .description("Number of tool invocations")
            .register(meterRegistry);

        this.taskTimer = Timer.builder("agent.task_duration")
            .description("Agent task duration")
            .register(meterRegistry);
    }

    /**
     * Execute an agent task with the given request.
     */
    public Mono<AgentResponse> execute(AgentRequest request, AgentContext context) {
        String taskId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();

        log.info("Starting agent task: taskId={}, agentType={}, tenant={}",
            taskId, context.getAgentType(), context.getTenantId());

        taskCounter.increment();

        AgentTask task = new AgentTask(taskId, context, request, startTime);
        activeTasks.put(taskId, task);

        return taskTimer.record(() ->
            executeAgentLoop(task)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnSuccess(response -> {
                    activeTasks.remove(taskId);
                    recordTaskExecution(task, response, "COMPLETED");
                })
                .doOnError(e -> {
                    activeTasks.remove(taskId);
                    recordTaskExecution(task, null, "FAILED");
                    log.error("Agent task failed: taskId={}, error={}", taskId, e.getMessage());
                })
        );
    }

    /**
     * Execute agent with streaming response.
     */
    public Flux<AgentStreamEvent> executeStreaming(AgentRequest request, AgentContext context) {
        String taskId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();

        log.info("Starting streaming agent task: taskId={}, agentType={}, tenant={}",
            taskId, context.getAgentType(), context.getTenantId());

        taskCounter.increment();

        AgentTask task = new AgentTask(taskId, context, request, startTime);
        activeTasks.put(taskId, task);

        return executeAgentLoopStreaming(task)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .doOnComplete(() -> {
                activeTasks.remove(taskId);
            })
            .doOnError(e -> {
                activeTasks.remove(taskId);
                log.error("Streaming agent task failed: taskId={}, error={}", taskId, e.getMessage());
            });
    }

    /**
     * Cancel an active task.
     */
    public Mono<Boolean> cancelTask(String taskId) {
        AgentTask task = activeTasks.remove(taskId);
        if (task != null) {
            log.info("Cancelled agent task: {}", taskId);
            return Mono.just(true);
        }
        return Mono.just(false);
    }

    /**
     * Get active task status.
     */
    public Optional<AgentTask> getTaskStatus(String taskId) {
        return Optional.ofNullable(activeTasks.get(taskId));
    }

    private Mono<AgentResponse> executeAgentLoop(AgentTask task) {
        return Mono.defer(() -> {
            AgentContext context = task.context();
            AgentRequest request = task.request();

            // Build initial LLM request
            LLMRequest llmRequest = buildLLMRequest(request, context);

            // Get conversation history and add to request
            return memoryManager.getConversationHistory(context, 20)
                .flatMap(history -> {
                    // Add history to request
                    List<LLMRequest.Message> messages = new ArrayList<>(history);
                    messages.add(LLMRequest.Message.user(request.userMessage()));
                    llmRequest.setMessages(messages);

                    // Execute the agent loop
                    return executeIterations(llmRequest, context, 0);
                })
                .flatMap(response -> {
                    // Store messages in memory
                    return memoryManager.storeMessage(context, LLMRequest.Message.user(request.userMessage()))
                        .then(memoryManager.storeMessage(context, LLMRequest.Message.assistant(response.content())))
                        .thenReturn(response);
                });
        });
    }

    private Mono<AgentResponse> executeIterations(LLMRequest request, AgentContext context, int iteration) {
        if (iteration >= maxIterations) {
            return Mono.just(AgentResponse.error("Maximum iterations reached"));
        }

        // Get available tools
        List<ToolDefinition> tools = toolRegistry.getToolDefinitions(context);

        // Select provider
        LLMProvider provider = llmProviderFactory.selectOptimalProvider(request);

        // Execute LLM call
        return Mono.fromCallable(() -> provider.completeWithTools(request, tools))
            .flatMap(response -> {
                // Apply guardrails
                GuardrailService.GuardrailResult guardrailResult = guardrailService.check(response, context);
                if (guardrailResult.blocked()) {
                    log.warn("Response blocked by guardrails: {}", guardrailResult.reason());
                    return Mono.just(AgentResponse.blocked(guardrailResult.reason()));
                }

                // Check if tool calls are needed
                if (response.hasToolCalls()) {
                    return executeToolCalls(response.getToolCalls(), context)
                        .flatMap(toolResults -> {
                            // Add assistant message with tool calls
                            request.getMessages().add(LLMRequest.Message.assistant(
                                response.getContent(), response.getToolCalls()));

                            // Add tool results
                            request.getMessages().add(LLMRequest.Message.toolResults(toolResults));

                            // Continue iteration
                            return executeIterations(request, context, iteration + 1);
                        });
                }

                // No tool calls - return final response
                return Mono.just(AgentResponse.success(
                    response.getContent(),
                    response.getUsage(),
                    response.getModel()
                ));
            });
    }

    private Mono<List<LLMRequest.ToolResult>> executeToolCalls(
            List<LLMRequest.ToolCall> toolCalls,
            AgentContext context) {

        return Flux.fromIterable(toolCalls)
            .flatMap(toolCall -> executeToolCall(toolCall, context))
            .collectList();
    }

    private Mono<LLMRequest.ToolResult> executeToolCall(LLMRequest.ToolCall toolCall, AgentContext context) {
        toolInvocationCounter.increment();

        String toolName = toolCall.getName();
        log.info("Executing tool: {}, callId={}", toolName, toolCall.getId());

        return Mono.defer(() -> {
            // Get tool
            Tool tool = toolRegistry.getTool(toolName)
                .orElseThrow(() -> new RuntimeException("Tool not found: " + toolName));

            // Validate arguments
            Tool.ValidationResult validation = tool.validate(toolCall.getArguments());
            if (!validation.isValid()) {
                return Mono.just(LLMRequest.ToolResult.builder()
                    .toolCallId(toolCall.getId())
                    .toolName(toolName)
                    .content("Validation error: " + String.join(", ", validation.errors()))
                    .isError(true)
                    .build());
            }

            // Check if tool requires human-in-the-loop approval
            if (tool.getDefinition().needsApproval()) {
                ApprovalResult approvalResult = approvalIntegration.checkAndCreateApprovalRequest(
                    tool.getDefinition(),
                    objectMapper.valueToTree(toolCall.getArguments()),
                    context
                );

                if (!approvalResult.canProceed()) {
                    log.info("Tool execution pending approval: tool={}, approvalId={}, status={}",
                        toolName, approvalResult.approvalId(), approvalResult.status());
                    return Mono.just(LLMRequest.ToolResult.builder()
                        .toolCallId(toolCall.getId())
                        .toolName(toolName)
                        .content(approvalResult.message() != null
                            ? approvalResult.message()
                            : "This action requires human approval. Approval request has been created.")
                        .isError(true)
                        .build());
                }
            }

            // Execute tool
            return tool.execute(toolCall.getArguments(), context)
                .map(result -> LLMRequest.ToolResult.builder()
                    .toolCallId(toolCall.getId())
                    .toolName(toolName)
                    .content(result.toToolResultContent())
                    .isError(!result.success())
                    .build())
                .onErrorResume(e -> {
                    log.error("Tool execution failed: tool={}, error={}", toolName, e.getMessage());
                    return Mono.just(LLMRequest.ToolResult.builder()
                        .toolCallId(toolCall.getId())
                        .toolName(toolName)
                        .content("Tool execution error: " + e.getMessage())
                        .isError(true)
                        .build());
                });
        });
    }

    private Flux<AgentStreamEvent> executeAgentLoopStreaming(AgentTask task) {
        return Flux.defer(() -> {
            AgentContext context = task.context();
            AgentRequest request = task.request();

            LLMRequest llmRequest = buildLLMRequest(request, context);

            return memoryManager.getConversationHistory(context, 20)
                .flatMapMany(history -> {
                    List<LLMRequest.Message> messages = new ArrayList<>(history);
                    messages.add(LLMRequest.Message.user(request.userMessage()));
                    llmRequest.setMessages(messages);

                    LLMProvider provider = llmProviderFactory.selectOptimalProvider(llmRequest);

                    return provider.completeStreaming(llmRequest)
                        .map(chunk -> mapChunkToEvent(chunk, task.taskId()));
                });
        });
    }

    private AgentStreamEvent mapChunkToEvent(LLMStreamChunk chunk, String taskId) {
        return switch (chunk.getType()) {
            case CONTENT_BLOCK_DELTA -> AgentStreamEvent.textDelta(taskId, chunk.getDelta());
            case TOOL_USE_START -> AgentStreamEvent.toolStart(taskId, chunk.getToolCall());
            case TOOL_USE_DELTA -> AgentStreamEvent.toolDelta(taskId, chunk.getDelta());
            case DONE -> AgentStreamEvent.done(taskId, chunk.getUsage());
            case ERROR -> AgentStreamEvent.error(taskId, chunk.getDelta());
            default -> AgentStreamEvent.empty(taskId);
        };
    }

    private LLMRequest buildLLMRequest(AgentRequest request, AgentContext context) {
        return LLMRequest.builder()
            .systemPrompt(request.systemPrompt())
            .maxTokens(request.maxTokens() > 0 ? request.maxTokens() : 4096)
            .temperature(request.temperature() > 0 ? request.temperature() : 0.3)
            .model(request.model())
            .tenantId(context.getTenantId())
            .sessionId(context.getSessionId())
            .correlationId(context.getCorrelationId())
            .build();
    }

    private void recordTaskExecution(AgentTask task, AgentResponse response, String status) {
        long durationMs = Duration.between(task.startTime(), Instant.now()).toMillis();

        MemoryManager.TaskExecution execution = new MemoryManager.TaskExecution(
            task.taskId(),
            task.context().getAgentType(),
            task.request().userMessage(),
            response != null ? response.content() : null,
            status,
            durationMs,
            task.startTime(),
            Instant.now(),
            Map.of("model", response != null && response.model() != null ? response.model() : "unknown")
        );

        memoryManager.storeTaskExecution(task.context(), execution)
            .subscribe(
                v -> log.debug("Recorded task execution: {}", task.taskId()),
                e -> log.error("Failed to record task execution: {}", e.getMessage())
            );
    }

    /**
     * Agent task tracking record.
     */
    public record AgentTask(
        String taskId,
        AgentContext context,
        AgentRequest request,
        Instant startTime
    ) {}

    /**
     * Agent request.
     */
    public record AgentRequest(
        String userMessage,
        String systemPrompt,
        String model,
        int maxTokens,
        double temperature,
        List<String> enabledTools,
        Map<String, Object> metadata
    ) {
        public static AgentRequest simple(String message) {
            return new AgentRequest(message, null, null, 0, 0, null, null);
        }

        public static AgentRequest withSystemPrompt(String message, String systemPrompt) {
            return new AgentRequest(message, systemPrompt, null, 0, 0, null, null);
        }
    }

    /**
     * Agent response.
     */
    public record AgentResponse(
        boolean success,
        String content,
        String error,
        LLMResponse.TokenUsage usage,
        String model,
        boolean blocked,
        String blockReason
    ) {
        public static AgentResponse success(String content, LLMResponse.TokenUsage usage, String model) {
            return new AgentResponse(true, content, null, usage, model, false, null);
        }

        public static AgentResponse error(String error) {
            return new AgentResponse(false, null, error, null, null, false, null);
        }

        public static AgentResponse blocked(String reason) {
            return new AgentResponse(false, null, null, null, null, true, reason);
        }
    }

    /**
     * Streaming event.
     */
    public record AgentStreamEvent(
        String taskId,
        String type,
        String delta,
        LLMRequest.ToolCall toolCall,
        LLMResponse.TokenUsage usage,
        String error
    ) {
        public static AgentStreamEvent textDelta(String taskId, String delta) {
            return new AgentStreamEvent(taskId, "text_delta", delta, null, null, null);
        }

        public static AgentStreamEvent toolStart(String taskId, LLMRequest.ToolCall toolCall) {
            return new AgentStreamEvent(taskId, "tool_start", null, toolCall, null, null);
        }

        public static AgentStreamEvent toolDelta(String taskId, String delta) {
            return new AgentStreamEvent(taskId, "tool_delta", delta, null, null, null);
        }

        public static AgentStreamEvent done(String taskId, LLMResponse.TokenUsage usage) {
            return new AgentStreamEvent(taskId, "done", null, null, usage, null);
        }

        public static AgentStreamEvent error(String taskId, String error) {
            return new AgentStreamEvent(taskId, "error", null, null, null, error);
        }

        public static AgentStreamEvent empty(String taskId) {
            return new AgentStreamEvent(taskId, "empty", null, null, null, null);
        }
    }
}
