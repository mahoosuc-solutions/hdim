package com.healthdata.agent.client;

import com.healthdata.agent.client.dto.CustomAgentConfigDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign client for Agent Builder Service communication.
 * Retrieves custom agent configurations for runtime execution.
 */
@FeignClient(
    name = "agent-builder-service",
    url = "${hdim.services.agent-builder.url:http://agent-builder-service:8080}"
)
public interface AgentBuilderClient {

    /**
     * Get all active agent configurations for a tenant.
     */
    @GetMapping("/api/v1/agents/configurations")
    List<CustomAgentConfigDTO> getActiveConfigurations(
        @RequestParam("tenantId") String tenantId,
        @RequestParam(value = "status", defaultValue = "ACTIVE") String status
    );

    /**
     * Get a specific agent configuration by ID.
     */
    @GetMapping("/api/v1/agents/configurations/{configId}")
    CustomAgentConfigDTO getConfiguration(
        @PathVariable("configId") String configId
    );

    /**
     * Get agent configuration by agent type for a tenant.
     */
    @GetMapping("/api/v1/agents/configurations/by-type/{agentType}")
    CustomAgentConfigDTO getConfigurationByType(
        @PathVariable("agentType") String agentType,
        @RequestParam("tenantId") String tenantId
    );

    /**
     * Get all active configurations across all tenants (for cache warming).
     * Requires admin privileges.
     */
    @GetMapping("/api/v1/agents/configurations/all-active")
    List<CustomAgentConfigDTO> getAllActiveConfigurations();
}
