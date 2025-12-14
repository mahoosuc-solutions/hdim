package com.healthdata.agent.agents;

import com.healthdata.agent.agents.custom.CustomAgentProvider;
import com.healthdata.agent.core.AgentContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Registry for managing available AI agents.
 * Discovers and provides access to both built-in (Spring bean) agents
 * and custom agents defined via the Agent Builder service.
 */
@Slf4j
@Service
public class AgentRegistry {

    private final Map<String, AgentDefinition> builtInAgents = new ConcurrentHashMap<>();

    private CustomAgentProvider customAgentProvider;

    public AgentRegistry(List<AgentDefinition> agentList) {
        // Register all available built-in agents
        agentList.forEach(agent -> {
            builtInAgents.put(agent.getAgentType(), agent);
            log.info("Registered built-in agent: {} - {}", agent.getAgentType(), agent.getDisplayName());
        });

        log.info("Initialized agent registry with {} built-in agents: {}",
            builtInAgents.size(), builtInAgents.keySet());
    }

    /**
     * Lazy inject CustomAgentProvider to avoid circular dependency.
     */
    @Autowired
    @Lazy
    public void setCustomAgentProvider(CustomAgentProvider customAgentProvider) {
        this.customAgentProvider = customAgentProvider;
        log.info("CustomAgentProvider injected into AgentRegistry");
    }

    /**
     * Get an agent by type.
     * First checks built-in agents, then falls back to custom agents.
     *
     * @param agentType The agent type identifier
     * @return Optional containing the agent if found
     */
    public Optional<AgentDefinition> getAgent(String agentType) {
        // First check built-in agents
        AgentDefinition builtIn = builtInAgents.get(agentType);
        if (builtIn != null) {
            return Optional.of(builtIn);
        }

        // Fall back to custom agents (if provider is available)
        if (customAgentProvider != null) {
            // For getAgent without context, we can't check tenant - this is for compatibility
            // Real lookups should use getAgent(agentType, context)
            return Optional.empty();
        }

        return Optional.empty();
    }

    /**
     * Get an agent by type with tenant context.
     * First checks built-in agents, then falls back to custom agents.
     *
     * @param agentType The agent type identifier
     * @param context   The agent context (provides tenant info)
     * @return Optional containing the agent if found
     */
    public Optional<AgentDefinition> getAgent(String agentType, AgentContext context) {
        // First check built-in agents
        AgentDefinition builtIn = builtInAgents.get(agentType);
        if (builtIn != null) {
            return Optional.of(builtIn);
        }

        // Fall back to custom agents
        if (customAgentProvider != null && context != null) {
            return customAgentProvider.getAgent(agentType, context.getTenantId());
        }

        return Optional.empty();
    }

    /**
     * Get an agent by type or throw exception.
     */
    public AgentDefinition getAgentOrThrow(String agentType) {
        return getAgent(agentType)
            .orElseThrow(() -> new AgentNotFoundException("Agent not found: " + agentType));
    }

    /**
     * Get an agent by type with context, or throw exception.
     */
    public AgentDefinition getAgentOrThrow(String agentType, AgentContext context) {
        return getAgent(agentType, context)
            .orElseThrow(() -> new AgentNotFoundException("Agent not found: " + agentType));
    }

    /**
     * List all built-in registered agents.
     */
    public List<AgentDefinition> listAgents() {
        return List.copyOf(builtInAgents.values());
    }

    /**
     * List all agents available for the given context (built-in + custom).
     */
    public List<AgentDefinition> listAvailableAgents(AgentContext context) {
        List<AgentDefinition> available = new ArrayList<>();

        // Add available built-in agents
        builtInAgents.values().stream()
            .filter(agent -> agent.isAvailable(context))
            .filter(agent -> hasRequiredRoles(agent, context))
            .forEach(available::add);

        // Add available custom agents
        if (customAgentProvider != null && context != null) {
            customAgentProvider.getAgentsForTenant(context.getTenantId()).stream()
                .filter(agent -> agent.isAvailable(context))
                .filter(agent -> hasRequiredRoles(agent, context))
                .forEach(available::add);
        }

        return available;
    }

    /**
     * List only built-in agents available for the given context.
     */
    public List<AgentDefinition> listBuiltInAgents(AgentContext context) {
        return builtInAgents.values().stream()
            .filter(agent -> agent.isAvailable(context))
            .filter(agent -> hasRequiredRoles(agent, context))
            .collect(Collectors.toList());
    }

    /**
     * List only custom agents available for the given context.
     */
    public List<AgentDefinition> listCustomAgents(AgentContext context) {
        if (customAgentProvider == null || context == null) {
            return List.of();
        }
        return customAgentProvider.getAgentsForTenant(context.getTenantId()).stream()
            .filter(agent -> agent.isAvailable(context))
            .filter(agent -> hasRequiredRoles(agent, context))
            .collect(Collectors.toList());
    }

    /**
     * Check if an agent exists (built-in only).
     */
    public boolean hasAgent(String agentType) {
        return builtInAgents.containsKey(agentType);
    }

    /**
     * Check if an agent exists (including custom agents).
     */
    public boolean hasAgent(String agentType, AgentContext context) {
        if (builtInAgents.containsKey(agentType)) {
            return true;
        }
        if (customAgentProvider != null && context != null) {
            return customAgentProvider.getAgent(agentType, context.getTenantId()).isPresent();
        }
        return false;
    }

    /**
     * Check if an agent type is a custom agent.
     */
    public boolean isCustomAgent(String agentType) {
        return customAgentProvider != null && customAgentProvider.isCustomAgentType(agentType);
    }

    /**
     * Get built-in agent count.
     */
    public int getAgentCount() {
        return builtInAgents.size();
    }

    /**
     * Get total agent count including custom agents for a tenant.
     */
    public int getTotalAgentCount(AgentContext context) {
        int count = builtInAgents.size();
        if (customAgentProvider != null && context != null) {
            count += customAgentProvider.getAgentsForTenant(context.getTenantId()).size();
        }
        return count;
    }

    /**
     * Get agent info for API responses (built-in agents only).
     */
    public List<AgentInfo> getAgentInfoList() {
        return builtInAgents.values().stream()
            .map(agent -> new AgentInfo(
                agent.getAgentType(),
                agent.getDisplayName(),
                agent.getDescription(),
                agent.getEnabledTools(),
                agent.getRequiredRoles(),
                false  // isCustom
            ))
            .collect(Collectors.toList());
    }

    /**
     * Get agent info for API responses (including custom agents).
     */
    public List<AgentInfo> getAgentInfoList(AgentContext context) {
        List<AgentInfo> infos = new ArrayList<>();

        // Built-in agents
        builtInAgents.values().stream()
            .filter(agent -> agent.isAvailable(context))
            .filter(agent -> hasRequiredRoles(agent, context))
            .map(agent -> new AgentInfo(
                agent.getAgentType(),
                agent.getDisplayName(),
                agent.getDescription(),
                agent.getEnabledTools(),
                agent.getRequiredRoles(),
                false
            ))
            .forEach(infos::add);

        // Custom agents
        if (customAgentProvider != null && context != null) {
            customAgentProvider.getAgentsForTenant(context.getTenantId()).stream()
                .filter(agent -> agent.isAvailable(context))
                .filter(agent -> hasRequiredRoles(agent, context))
                .map(agent -> new AgentInfo(
                    agent.getAgentType(),
                    agent.getDisplayName(),
                    agent.getDescription(),
                    agent.getEnabledTools(),
                    agent.getRequiredRoles(),
                    true
                ))
                .forEach(infos::add);
        }

        return infos;
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
        List<String> requiredRoles,
        boolean isCustom
    ) {
        /**
         * Convenience constructor for built-in agents.
         */
        public AgentInfo(String agentType, String displayName, String description,
                        List<String> enabledTools, List<String> requiredRoles) {
            this(agentType, displayName, description, enabledTools, requiredRoles, false);
        }
    }

    /**
     * Exception thrown when an agent is not found.
     */
    public static class AgentNotFoundException extends RuntimeException {
        public AgentNotFoundException(String message) {
            super(message);
        }
    }
}
