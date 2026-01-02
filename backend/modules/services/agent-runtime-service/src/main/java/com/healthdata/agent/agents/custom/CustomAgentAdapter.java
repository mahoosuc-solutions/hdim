package com.healthdata.agent.agents.custom;

import com.healthdata.agent.agents.AgentDefinition;
import com.healthdata.agent.client.dto.CustomAgentConfigDTO;
import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.core.AgentOrchestrator;
import com.healthdata.agent.core.AgentOrchestrator.AgentRequest;
import com.healthdata.agent.core.AgentOrchestrator.AgentResponse;
import com.healthdata.agent.core.AgentOrchestrator.AgentStreamEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adapter that wraps a CustomAgentConfigDTO to implement AgentDefinition.
 * Allows custom agents defined via the Agent Builder UI to be executed
 * by the standard agent runtime infrastructure.
 */
@Slf4j
@RequiredArgsConstructor
public class CustomAgentAdapter implements AgentDefinition {

    private final CustomAgentConfigDTO config;
    private final AgentOrchestrator orchestrator;

    @Override
    public String getAgentType() {
        return config.getAgentType();
    }

    @Override
    public String getDisplayName() {
        return config.getDisplayName() != null ? config.getDisplayName() : config.getName();
    }

    @Override
    public String getDescription() {
        return config.getDescription();
    }

    @Override
    public String getSystemPrompt() {
        return buildSystemPrompt();
    }

    @Override
    public List<String> getEnabledTools() {
        if (config.getEnabledTools() == null) {
            return List.of();
        }
        return config.getEnabledTools().stream()
            .filter(CustomAgentConfigDTO.ToolConfig::isEnabled)
            .map(CustomAgentConfigDTO.ToolConfig::getToolName)
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("model", resolveModelId());
        params.put("maxTokens", config.getMaxTokens() != null ? config.getMaxTokens() : 4096);
        params.put("temperature", config.getTemperature() != null ? config.getTemperature() : 0.3);
        if (config.getTopP() != null) {
            params.put("topP", config.getTopP());
        }
        return params;
    }

    @Override
    public boolean isAvailable(AgentContext context) {
        // Check tenant access
        if (config.getAllowedTenants() != null && !config.getAllowedTenants().isEmpty()) {
            if (!config.getAllowedTenants().contains(context.getTenantId())) {
                return false;
            }
        }

        // Check status
        return config.getStatus() == CustomAgentConfigDTO.Status.ACTIVE ||
               config.getStatus() == CustomAgentConfigDTO.Status.TESTING;
    }

    @Override
    public List<String> getRequiredRoles() {
        return config.getAllowedRoles() != null ? config.getAllowedRoles() : List.of();
    }

    @Override
    public boolean requiresPatientContext() {
        return config.isRequiresPatientContext();
    }

    @Override
    public Mono<AgentResponse> execute(String message, AgentContext context) {
        String effectivePrompt = applyUserPromptTemplate(message);

        AgentRequest request = new AgentRequest(
            effectivePrompt,
            getSystemPrompt(),
            resolveModelId(),
            (Integer) getDefaultParameters().get("maxTokens"),
            (Double) getDefaultParameters().get("temperature"),
            getEnabledTools(),
            buildMetadata()
        );

        log.debug("Executing custom agent: type={}, config={}", config.getAgentType(), config.getId());
        return orchestrator.execute(request, context.toBuilder()
            .agentType(config.getAgentType())
            .build());
    }

    @Override
    public Flux<AgentStreamEvent> executeStreaming(String message, AgentContext context) {
        String effectivePrompt = applyUserPromptTemplate(message);

        AgentRequest request = new AgentRequest(
            effectivePrompt,
            getSystemPrompt(),
            resolveModelId(),
            (Integer) getDefaultParameters().get("maxTokens"),
            (Double) getDefaultParameters().get("temperature"),
            getEnabledTools(),
            buildMetadata()
        );

        log.debug("Executing custom agent streaming: type={}, config={}", config.getAgentType(), config.getId());
        return orchestrator.executeStreaming(request, context.toBuilder()
            .agentType(config.getAgentType())
            .build());
    }

    /**
     * Get the underlying configuration.
     */
    public CustomAgentConfigDTO getConfig() {
        return config;
    }

    /**
     * Build the complete system prompt including persona, guardrails, and disclaimers.
     */
    private String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();

        // Add persona if defined
        if (config.getPersona() != null && !config.getPersona().isBlank()) {
            prompt.append(config.getPersona()).append("\n\n");
        }

        // Add main system prompt
        if (config.getSystemPrompt() != null && !config.getSystemPrompt().isBlank()) {
            prompt.append(config.getSystemPrompt());
        }

        // Add guardrail instructions
        if (config.getGuardrails() != null) {
            prompt.append(buildGuardrailInstructions());
        }

        return prompt.toString();
    }

    /**
     * Build guardrail instructions for the system prompt.
     */
    private String buildGuardrailInstructions() {
        CustomAgentConfigDTO.GuardrailConfig guardrails = config.getGuardrails();
        if (guardrails == null) {
            return "";
        }

        StringBuilder instructions = new StringBuilder("\n\n## SAFETY GUIDELINES\n");

        if (guardrails.isEnableClinicalSafety()) {
            instructions.append("""
                - NEVER provide definitive diagnoses. Use phrases like "findings suggest" or "consider evaluating for"
                - NEVER prescribe medications or specific dosages. Recommend "discuss with prescriber"
                - NEVER advise stopping medications. Say "consult with the prescribing provider"
                - ALWAYS recommend clinical review for high-risk findings
                """);
        }

        if (guardrails.isEnablePHIProtection()) {
            instructions.append("""
                - NEVER include patient identifiable information in responses unless explicitly requested
                - ALWAYS refer to patients by ID rather than name when possible
                - NEVER log or expose PHI in error messages
                """);
        }

        if (guardrails.isStrictMode()) {
            instructions.append("- Operate in strict mode: decline any requests outside defined capabilities\n");
        }

        if (guardrails.getRequiredDisclaimers() != null && !guardrails.getRequiredDisclaimers().isEmpty()) {
            instructions.append("\n## REQUIRED DISCLAIMERS\n");
            instructions.append("Include these disclaimers when appropriate:\n");
            for (String disclaimer : guardrails.getRequiredDisclaimers()) {
                instructions.append("- ").append(disclaimer).append("\n");
            }
        }

        return instructions.toString();
    }

    /**
     * Apply user prompt template if defined.
     */
    private String applyUserPromptTemplate(String userMessage) {
        if (config.getUserPromptTemplate() == null || config.getUserPromptTemplate().isBlank()) {
            return userMessage;
        }

        // Simple template replacement - {{message}} placeholder
        return config.getUserPromptTemplate().replace("{{message}}", userMessage);
    }

    /**
     * Resolve the model ID based on provider and model configuration.
     */
    private String resolveModelId() {
        if (config.getModelId() != null && !config.getModelId().isBlank()) {
            return config.getModelId();
        }

        // Default models based on provider
        return switch (config.getModelProvider() != null ? config.getModelProvider() : "anthropic") {
            case "azure-openai" -> "gpt-4o";
            case "bedrock" -> "anthropic.claude-3-5-sonnet-20241022-v2:0";
            default -> "claude-3-5-sonnet-20241022";
        };
    }

    /**
     * Build metadata for tracking and auditing.
     */
    private Map<String, Object> buildMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("agentType", config.getAgentType());
        metadata.put("configId", config.getId());
        metadata.put("configVersion", config.getVersion());
        metadata.put("customAgent", true);
        return metadata;
    }
}
