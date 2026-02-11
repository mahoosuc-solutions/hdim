package com.healthdata.agentvalidation.client;

import com.healthdata.agentvalidation.client.dto.AgentExecutionRequest;
import com.healthdata.agentvalidation.client.dto.AgentExecutionResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign client for Agent Runtime Service communication.
 * Used to execute agent interactions during validation testing.
 */
@FeignClient(
    name = "agent-runtime-service",
    url = "${hdim.services.agent-runtime.url:http://agent-runtime-service:8088}"
)
public interface AgentRuntimeClient {

    /**
     * Execute an agent interaction with the specified context.
     */
    @PostMapping("/api/v1/agent/execute")
    @CircuitBreaker(name = "agent-runtime")
    @Retry(name = "agent-runtime")
    AgentExecutionResponse executeAgent(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestHeader("X-User-ID") String userId,
        @RequestHeader("X-Trace-ID") String traceId,
        @RequestBody AgentExecutionRequest request
    );

    /**
     * Execute agent with a specific provider override.
     */
    @PostMapping("/api/v1/agent/execute")
    @CircuitBreaker(name = "agent-runtime")
    @Retry(name = "agent-runtime")
    AgentExecutionResponse executeAgentWithProvider(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestHeader("X-User-ID") String userId,
        @RequestHeader("X-Trace-ID") String traceId,
        @RequestHeader("X-LLM-Provider") String llmProvider,
        @RequestBody AgentExecutionRequest request
    );

    /**
     * Get available agent types.
     */
    @GetMapping("/api/v1/agent/types")
    @CircuitBreaker(name = "agent-runtime")
    List<String> getAvailableAgentTypes(
        @RequestHeader("X-Tenant-ID") String tenantId
    );

    /**
     * Get available LLM providers and their health status.
     */
    @GetMapping("/api/v1/agent/providers")
    @CircuitBreaker(name = "agent-runtime")
    Map<String, ProviderStatus> getProviderStatus();

    /**
     * LLM provider status information.
     */
    record ProviderStatus(
        String provider,
        boolean healthy,
        boolean enabled,
        String model,
        Long avgLatencyMs
    ) {}
}
