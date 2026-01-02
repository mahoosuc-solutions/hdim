package com.healthdata.agent.agents;

import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.core.AgentOrchestrator.AgentResponse;
import com.healthdata.agent.core.AgentOrchestrator.AgentStreamEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Interface for defining specialized AI agents.
 * Each agent has a specific purpose, system prompt, and tool configuration.
 */
public interface AgentDefinition {

    /**
     * Unique identifier for this agent type.
     */
    String getAgentType();

    /**
     * Human-readable display name.
     */
    String getDisplayName();

    /**
     * Description of agent capabilities.
     */
    String getDescription();

    /**
     * System prompt defining agent behavior.
     */
    String getSystemPrompt();

    /**
     * List of tool names enabled for this agent.
     */
    List<String> getEnabledTools();

    /**
     * Default LLM parameters for this agent.
     */
    Map<String, Object> getDefaultParameters();

    /**
     * Execute the agent with a user message.
     */
    Mono<AgentResponse> execute(String message, AgentContext context);

    /**
     * Execute with streaming response.
     */
    Flux<AgentStreamEvent> executeStreaming(String message, AgentContext context);

    /**
     * Check if this agent is available for the given context.
     */
    default boolean isAvailable(AgentContext context) {
        return true;
    }

    /**
     * Get required roles to use this agent.
     */
    default List<String> getRequiredRoles() {
        return List.of();
    }

    /**
     * Whether this agent requires patient context.
     */
    default boolean requiresPatientContext() {
        return false;
    }
}
