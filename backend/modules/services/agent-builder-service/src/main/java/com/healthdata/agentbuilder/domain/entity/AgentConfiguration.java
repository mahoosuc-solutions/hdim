package com.healthdata.agentbuilder.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Agent configuration entity for custom agents.
 */
@Entity
@Table(name = "agent_configurations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    private String description;

    @Column(nullable = false)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentStatus status;

    // Persona
    @Column(name = "persona_name")
    private String personaName;

    @Column(name = "persona_role")
    private String personaRole;

    @Column(name = "persona_avatar_url")
    private String personaAvatarUrl;

    // Model configuration
    @Column(name = "model_provider", nullable = false)
    private String modelProvider;

    @Column(name = "model_id")
    private String modelId;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(precision = 3, scale = 2)
    private BigDecimal temperature;

    // Prompts
    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "welcome_message", columnDefinition = "TEXT")
    private String welcomeMessage;

    // JSON configurations
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_configuration", columnDefinition = "jsonb")
    private List<ToolConfig> toolConfiguration;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "guardrail_configuration", columnDefinition = "jsonb")
    private GuardrailConfig guardrailConfiguration;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ui_configuration", columnDefinition = "jsonb")
    private UIConfig uiConfiguration;

    // Access control
    @Column(name = "allowed_roles")
    private String[] allowedRoles;

    @Column(name = "requires_patient_context")
    private Boolean requiresPatientContext;

    // Metadata
    private String[] tags;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    // Relationships
    @OneToMany(mappedBy = "agentConfiguration", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AgentVersion> versions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = AgentStatus.DRAFT;
        }
        if (version == null) {
            version = "1.0.0";
        }
        if (slug == null && name != null) {
            slug = generateSlug(name);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }

    public enum AgentStatus {
        DRAFT,
        TESTING,
        ACTIVE,
        DEPRECATED,
        ARCHIVED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolConfig {
        private String toolName;
        private boolean enabled;
        private Map<String, Object> config;
    }

    @Data
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UIConfig {
        private String primaryColor;
        private String position;
        private boolean showAvatar;
        private String chatWindowSize;
        private Map<String, Object> customStyles;
    }
}
