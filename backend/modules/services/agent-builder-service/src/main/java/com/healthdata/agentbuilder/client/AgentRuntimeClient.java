package com.healthdata.agentbuilder.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign client for communicating with the Agent Runtime Service.
 */
@FeignClient(
    name = "agent-runtime-service",
    url = "${hdim.agent-runtime.url:http://agent-runtime-service:8080}",
    fallbackFactory = AgentRuntimeClientFallbackFactory.class
)
public interface AgentRuntimeClient {

    /**
     * Execute an agent with the given request.
     */
    @PostMapping("/api/v1/agents/{slug}/execute")
    Map<String, Object> executeAgent(
        @PathVariable("slug") String agentSlug,
        @RequestBody Map<String, Object> request,
        @RequestHeader("X-Tenant-ID") String tenantId
    );

    /**
     * Stream agent response (returns streaming connection info).
     */
    @PostMapping("/api/v1/agents/{slug}/stream")
    Map<String, Object> streamAgent(
        @PathVariable("slug") String agentSlug,
        @RequestBody Map<String, Object> request,
        @RequestHeader("X-Tenant-ID") String tenantId
    );

    /**
     * Get available tools from the runtime.
     */
    @GetMapping("/api/v1/tools")
    List<ToolInfo> getAvailableTools(
        @RequestHeader("X-Tenant-ID") String tenantId
    );

    /**
     * Validate agent configuration against runtime capabilities.
     */
    @PostMapping("/api/v1/agents/validate")
    ValidationResult validateAgentConfiguration(
        @RequestBody Map<String, Object> configuration,
        @RequestHeader("X-Tenant-ID") String tenantId
    );

    /**
     * Get supported LLM providers.
     */
    @GetMapping("/api/v1/providers")
    List<ProviderInfo> getSupportedProviders();

    /**
     * Get supported models for a provider.
     */
    @GetMapping("/api/v1/providers/{provider}/models")
    List<ModelInfo> getSupportedModels(
        @PathVariable("provider") String provider
    );

    /**
     * Health check for the runtime service.
     */
    @GetMapping("/actuator/health")
    Map<String, Object> healthCheck();

    // DTOs for responses

    record ToolInfo(
        String name,
        String description,
        String category,
        Map<String, Object> inputSchema,
        boolean requiresApproval
    ) {}

    record ValidationResult(
        boolean valid,
        List<ValidationError> errors,
        List<String> warnings
    ) {}

    record ValidationError(
        String field,
        String message,
        String code
    ) {}

    record ProviderInfo(
        String name,
        String displayName,
        boolean available,
        boolean hipaaCompliant,
        List<String> regions
    ) {}

    record ModelInfo(
        String modelId,
        String displayName,
        int maxTokens,
        boolean supportsStreaming,
        boolean supportsTools
    ) {}
}
