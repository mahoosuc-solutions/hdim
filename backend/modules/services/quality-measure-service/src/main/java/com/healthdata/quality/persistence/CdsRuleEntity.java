package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * CDS Rule Entity
 * Represents a Clinical Decision Support rule that can generate recommendations
 * for patients based on CQL evaluation.
 */
@Entity
@Table(name = "cds_rules", indexes = {
    @Index(name = "idx_cds_rule_tenant_active", columnList = "tenant_id, active"),
    @Index(name = "idx_cds_rule_category", columnList = "category"),
    @Index(name = "idx_cds_rule_priority", columnList = "priority")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdsRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private CdsCategory category;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "cql_library_id")
    private UUID cqlLibraryId;

    @Column(name = "cql_library_name", length = 255)
    private String cqlLibraryName;

    @Column(name = "cql_expression", length = 255)
    private String cqlExpression;

    @Column(name = "recommendation_template", columnDefinition = "TEXT")
    private String recommendationTemplate;

    @Column(name = "evidence_source", columnDefinition = "TEXT")
    private String evidenceSource;

    @Column(name = "clinical_guideline", length = 255)
    private String clinicalGuideline;

    @Column(name = "action_items", columnDefinition = "TEXT")
    private String actionItems;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_urgency", nullable = false, length = 20)
    @Builder.Default
    private CdsUrgency defaultUrgency = CdsUrgency.ROUTINE;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "requires_acknowledgment", nullable = false)
    private Boolean requiresAcknowledgment = true;

    @Column(name = "applicable_conditions", columnDefinition = "TEXT")
    private String applicableConditions;

    @Column(name = "exclusion_criteria", columnDefinition = "TEXT")
    private String exclusionCriteria;

    @Column(name = "version", length = 20)
    private String version;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * CDS Categories
     */
    public enum CdsCategory {
        PREVENTIVE,       // Preventive care recommendations
        CHRONIC_DISEASE,  // Chronic disease management
        MEDICATION,       // Medication alerts and recommendations
        ALERT,            // Clinical alerts
        MENTAL_HEALTH,    // Mental health recommendations
        SDOH              // Social determinants of health
    }

    /**
     * CDS Urgency Levels
     */
    public enum CdsUrgency {
        EMERGENT,   // Requires immediate attention
        URGENT,     // Address within 24-48 hours
        SOON,       // Address within 1-2 weeks
        ROUTINE     // Address at next visit or within 30 days
    }
}
