package com.healthdata.agent.agents;

import com.healthdata.agent.core.AgentContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for managing available AI agents.
 * Discovers and provides access to all agent definitions.
 */
@Slf4j
@Service
public class AgentRegistry {

    private final Map<String, AgentDefinition> agents = new ConcurrentHashMap<>();

    public AgentRegistry(List<AgentDefinition> agentList) {
        // Register all available agents
        agentList.forEach(agent -> {
            agents.put(agent.getAgentType(), agent);
            log.info("Registered agent: {} - {}", agent.getAgentType(), agent.getDisplayName());
        });

        log.info("Initialized agent registry with {} agents: {}",
            agents.size(), agents.keySet());
    }

    /**
     * Get an agent by type.
     */
    public Optional<AgentDefinition> getAgent(String agentType) {
        return Optional.ofNullable(agents.get(agentType));
    }

    /**
     * Get an agent by type or throw exception.
     */
    public AgentDefinition getAgentOrThrow(String agentType) {
        return getAgent(agentType)
            .orElseThrow(() -> new AgentNotFoundException("Agent not found: " + agentType));
    }

    /**
     * List all registered agents.
     */
    public List<AgentDefinition> listAgents() {
        return List.copyOf(agents.values());
    }

    /**
     * List agents available for the given context.
     */
    public List<AgentDefinition> listAvailableAgents(AgentContext context) {
        return agents.values().stream()
            .filter(agent -> agent.isAvailable(context))
            .filter(agent -> hasRequiredRoles(agent, context))
            .collect(Collectors.toList());
    }

    /**
     * Check if an agent exists.
     */
    public boolean hasAgent(String agentType) {
        return agents.containsKey(agentType);
    }

    /**
     * Get agent count.
     */
    public int getAgentCount() {
        return agents.size();
    }

    /**
     * Get agent info for API responses.
     */
    public List<AgentInfo> getAgentInfoList() {
        return agents.values().stream()
            .map(agent -> new AgentInfo(
                agent.getAgentType(),
                agent.getDisplayName(),
                agent.getDescription(),
                agent.getEnabledTools(),
                agent.getRequiredRoles()
            ))
            .collect(Collectors.toList());
    }

    private boolean hasRequiredRoles(AgentDefinition agent, AgentContext context) {
        List<String> requiredRoles = agent.getRequiredRoles();
        if (requiredRoles == null || requiredRoles.isEmpty()) {
            return true;
        }
        return requiredRoles.stream().anyMatch(context::hasRole);
    }

    /**
     * Agent information DTO.
     */
    public record AgentInfo(
        String agentType,
        String displayName,
        String description,
        List<String> enabledTools,
        List<String> requiredRoles
    ) {}

    /**
     * Exception thrown when an agent is not found.
     */
    public static class AgentNotFoundException extends RuntimeException {
        public AgentNotFoundException(String message) {
            super(message);
        }
    }
}
