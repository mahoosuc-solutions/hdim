package com.healthdata.agentvalidation.client.dto;

import lombok.*;

import java.util.Map;

/**
 * Request DTO for agent execution.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentExecutionRequest {

    /**
     * The type of agent to execute (e.g., "clinical-decision", "care-gap-optimizer").
     */
    private String agentType;

    /**
     * The user's message/query to the agent.
     */
    private String userMessage;

    /**
     * Session/conversation ID for context continuity.
     */
    private String sessionId;

    /**
     * Additional context data for the agent.
     */
    private Map<String, Object> contextData;

    /**
     * Patient ID if the interaction is patient-specific.
     */
    private String patientId;

    /**
     * Maximum number of agent iterations.
     */
    @Builder.Default
    private int maxIterations = 10;

    /**
     * Whether to include detailed tool call information in response.
     */
    @Builder.Default
    private boolean includeToolCalls = true;

    /**
     * Whether to include trace information in response.
     */
    @Builder.Default
    private boolean includeTraceInfo = true;
}
