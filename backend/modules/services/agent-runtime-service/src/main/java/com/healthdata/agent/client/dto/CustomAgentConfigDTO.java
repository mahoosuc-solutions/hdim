package com.healthdata.agent.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO representing a custom agent configuration from the Agent Builder service.
 * Mirrors the AgentConfiguration entity structure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomAgentConfigDTO {

    private String id;
    private String tenantId;
    private String name;
    private String displayName;
    private String description;
    private String agentType;
    private Status status;

    // Agent Persona
    private String persona;
    private String systemPrompt;
    private String userPromptTemplate;

    // Model Configuration
    private String modelProvider;
    private String modelId;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;

    // Tool Configuration
    private List<ToolConfig> enabledTools;

    // Guardrails
    private GuardrailConfig guardrails;

    // Access Control
    private List<String> allowedRoles;
    private boolean requiresPatientContext;
    private List<String> allowedTenants;

    // UI Configuration
    private UIConfig uiConfig;

    // Metadata
    private String createdBy;
    private Instant createdAt;
    private String lastModifiedBy;
    private Instant lastModifiedAt;
    private String version;

    public enum Status {
        DRAFT,
        TESTING,
        ACTIVE,
        DEPRECATED,
        ARCHIVED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolConfig {
        private String toolName;
        private boolean enabled;
        private Map<String, Object> config;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuardrailConfig {
        private boolean enableClinicalSafety;
        private boolean enablePHIProtection;
        private boolean strictMode;
        private List<String> blockedPatterns;
        private List<String> requiredDisclaimers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UIConfig {
        private String icon;
        private String color;
        private String category;
        private int displayOrder;
        private List<String> suggestedPrompts;
    }
}
