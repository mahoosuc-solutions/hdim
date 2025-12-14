package com.healthdata.agent.api;

import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.core.AgentOrchestrator;
import com.healthdata.agent.core.AgentOrchestrator.AgentRequest;
import com.healthdata.agent.core.AgentOrchestrator.AgentResponse;
import com.healthdata.agent.core.AgentOrchestrator.AgentStreamEvent;
import com.healthdata.agent.llm.LLMProviderFactory;
import com.healthdata.agent.memory.CompositeMemoryManager;
import com.healthdata.agent.tool.ToolRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * REST API for AI Agent operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Tag(name = "AI Agents", description = "AI Agent execution and management")
public class AgentController {

    private final AgentOrchestrator orchestrator;
    private final ToolRegistry toolRegistry;
    private final LLMProviderFactory llmProviderFactory;
    private final CompositeMemoryManager memoryManager;

    @PostMapping("/{agentType}/execute")
    @Operation(summary = "Execute an agent task", description = "Execute an AI agent with the given message")
    public Mono<ResponseEntity<AgentResponseDTO>> execute(
            @PathVariable String agentType,
            @Valid @RequestBody AgentRequestDTO request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, agentType, request.sessionId());

        AgentRequest agentRequest = new AgentRequest(
            request.message(),
            request.systemPrompt(),
            request.model(),
            request.maxTokens() != null ? request.maxTokens() : 4096,
            request.temperature() != null ? request.temperature() : 0.3,
            request.enabledTools(),
            request.metadata()
        );

        return orchestrator.execute(agentRequest, context)
            .map(response -> ResponseEntity.ok(mapToDTO(response, context.getSessionId())))
            .onErrorResume(e -> {
                log.error("Agent execution failed: {}", e.getMessage(), e);
                return Mono.just(ResponseEntity.internalServerError()
                    .body(AgentResponseDTO.error(e.getMessage())));
            });
    }

    @PostMapping(value = "/{agentType}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Execute agent with streaming", description = "Execute an AI agent with streaming response")
    public Flux<AgentStreamEventDTO> executeStreaming(
            @PathVariable String agentType,
            @Valid @RequestBody AgentRequestDTO request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, agentType, request.sessionId());

        AgentRequest agentRequest = new AgentRequest(
            request.message(),
            request.systemPrompt(),
            request.model(),
            request.maxTokens() != null ? request.maxTokens() : 4096,
            request.temperature() != null ? request.temperature() : 0.3,
            request.enabledTools(),
            request.metadata()
        );

        return orchestrator.executeStreaming(agentRequest, context)
            .map(this::mapStreamEventToDTO);
    }

    @DeleteMapping("/tasks/{taskId}")
    @Operation(summary = "Cancel a running task", description = "Cancel an active agent task")
    public Mono<ResponseEntity<Map<String, Object>>> cancelTask(@PathVariable String taskId) {
        return orchestrator.cancelTask(taskId)
            .map(cancelled -> {
                if (cancelled) {
                    return ResponseEntity.ok(Map.of("cancelled", true, "taskId", taskId));
                }
                return ResponseEntity.notFound().<Map<String, Object>>build();
            });
    }

    @GetMapping("/tasks/{taskId}/status")
    @Operation(summary = "Get task status", description = "Get the status of an active agent task")
    public ResponseEntity<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        return orchestrator.getTaskStatus(taskId)
            .map(task -> ResponseEntity.ok(Map.<String, Object>of(
                "taskId", task.taskId(),
                "agentType", task.context().getAgentType(),
                "startTime", task.startTime().toString(),
                "status", "IN_PROGRESS"
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tools")
    @Operation(summary = "List available tools", description = "List all tools available to agents")
    public ResponseEntity<List<ToolInfoDTO>> listTools(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, null, null);

        List<ToolInfoDTO> tools = toolRegistry.listAvailableTools(context).stream()
            .map(tool -> new ToolInfoDTO(
                tool.getName(),
                tool.getDefinition().getDescription(),
                tool.getDefinition().getCategory().name(),
                tool.getDefinition().isRequiresApproval()
            ))
            .toList();

        return ResponseEntity.ok(tools);
    }

    @GetMapping("/providers")
    @Operation(summary = "List LLM providers", description = "List available and healthy LLM providers")
    public ResponseEntity<Map<String, Object>> listProviders() {
        return ResponseEntity.ok(Map.of(
            "registered", llmProviderFactory.listProviders(),
            "healthy", llmProviderFactory.listHealthyProviders(),
            "healthStatus", llmProviderFactory.getHealthStatus()
        ));
    }

    @DeleteMapping("/sessions/{sessionId}/memory")
    @Operation(summary = "Clear session memory", description = "Clear conversation memory for a session")
    public Mono<ResponseEntity<Void>> clearMemory(
            @PathVariable String sessionId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, null, sessionId);

        return memoryManager.clearConversation(context)
            .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @GetMapping("/health")
    @Operation(summary = "Agent service health", description = "Get health status of the agent runtime service")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "providers", llmProviderFactory.getHealthStatus(),
            "toolCount", toolRegistry.getToolCount()
        ));
    }

    private AgentContext buildContext(String tenantId, Jwt jwt, String agentType, String sessionId) {
        String userId = jwt != null ? jwt.getSubject() : "anonymous";
        @SuppressWarnings("unchecked")
        List<String> roles = jwt != null
            ? jwt.getClaimAsStringList("roles")
            : List.of();

        return AgentContext.builder()
            .tenantId(tenantId)
            .userId(userId)
            .sessionId(sessionId != null ? sessionId : UUID.randomUUID().toString())
            .correlationId(UUID.randomUUID().toString())
            .roles(Set.copyOf(roles != null ? roles : List.of()))
            .agentType(agentType)
            .origin("api")
            .build();
    }

    private AgentResponseDTO mapToDTO(AgentResponse response, String sessionId) {
        return new AgentResponseDTO(
            response.success(),
            response.content(),
            response.error(),
            response.usage() != null ? new TokenUsageDTO(
                response.usage().getInputTokens(),
                response.usage().getOutputTokens(),
                response.usage().getTotalTokens()
            ) : null,
            response.model(),
            sessionId,
            response.blocked(),
            response.blockReason()
        );
    }

    private AgentStreamEventDTO mapStreamEventToDTO(AgentStreamEvent event) {
        return new AgentStreamEventDTO(
            event.taskId(),
            event.type(),
            event.delta(),
            event.error()
        );
    }

    // DTOs
    public record AgentRequestDTO(
        @NotBlank String message,
        String systemPrompt,
        String model,
        Integer maxTokens,
        Double temperature,
        List<String> enabledTools,
        String sessionId,
        String patientId,
        Map<String, Object> metadata
    ) {}

    public record AgentResponseDTO(
        boolean success,
        String content,
        String error,
        TokenUsageDTO usage,
        String model,
        String sessionId,
        boolean blocked,
        String blockReason
    ) {
        public static AgentResponseDTO error(String error) {
            return new AgentResponseDTO(false, null, error, null, null, null, false, null);
        }
    }

    public record TokenUsageDTO(int inputTokens, int outputTokens, int totalTokens) {}

    public record AgentStreamEventDTO(String taskId, String type, String delta, String error) {}

    public record ToolInfoDTO(String name, String description, String category, boolean requiresApproval) {}
}
