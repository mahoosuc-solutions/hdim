package com.healthdata.agentbuilder.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Reusable prompt template entity.
 */
@Entity
@Table(name = "prompt_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateCategory category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<TemplateVariable> variables;

    @Column(name = "usage_count")
    private Integer usageCount;

    @Column(name = "is_system")
    private Boolean isSystem;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (usageCount == null) {
            usageCount = 0;
        }
        if (isSystem == null) {
            isSystem = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum TemplateCategory {
        SYSTEM_PROMPT,
        CAPABILITIES,
        CONSTRAINTS,
        RESPONSE_FORMAT,
        CLINICAL_SAFETY,
        TOOL_USAGE,
        PERSONA,
        CUSTOM
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateVariable {
        private String name;
        private String description;
        private String defaultValue;
        private boolean required;
    }
}
