package com.healthdata.quality.persistence;


import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Measure Modification Audit Entity
 * Tracks all changes to measure definitions for compliance and rollback capability.
 * Maps to the measure_modification_audit table created in migration 0039.
 *
 * Compliance: Records before/after values, approval status, and rollback data
 * for all modifications to quality measures, custom measures, and configurations.
 */
@Hidden
@Schema(hidden = true)
@Entity
@Table(name = "measure_modification_audit",
    indexes = {
        @Index(name = "idx_mma_entity", columnList = "entity_type, entity_id, modified_at DESC"),
        @Index(name = "idx_mma_modified_by", columnList = "modified_by, modified_at DESC"),
        @Index(name = "idx_mma_operation", columnList = "operation, modified_at DESC"),
        @Index(name = "idx_mma_tenant", columnList = "tenant_id"),
        @Index(name = "idx_mma_approval", columnList = "approval_status, requires_approval")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasureModificationAuditEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    // Entity Context
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // QUALITY_MEASURE, CUSTOM_MEASURE, MEASURE_OVERRIDE, CONFIG_PROFILE

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "entity_name")
    private String entityName;

    // Modification Details
    @Column(name = "operation", nullable = false, length = 50)
    private String operation; // CREATE, UPDATE, DELETE, ACTIVATE, DEACTIVATE, APPROVE, REJECT

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "change_description", columnDefinition = "TEXT")
    private String changeDescription;

    // Rollback Support
    @Column(name = "rollback_available", nullable = false)
    @Builder.Default
    private Boolean rollbackAvailable = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rollback_data", columnDefinition = "jsonb")
    private Map<String, Object> rollbackData;

    // Approval Workflow
    @Column(name = "requires_approval", nullable = false)
    @Builder.Default
    private Boolean requiresApproval = false;

    @Column(name = "approval_status", length = 50)
    private String approvalStatus; // PENDING, APPROVED, REJECTED, NOT_REQUIRED

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    // Audit
    @Column(name = "modified_by", nullable = false)
    private UUID modifiedBy;

    @Column(name = "modified_at", nullable = false, updatable = false)
    private OffsetDateTime modifiedAt;

    @PrePersist
    protected void onCreate() {
        if (modifiedAt == null) {
            modifiedAt = OffsetDateTime.now();
        }
        if (rollbackAvailable == null) {
            rollbackAvailable = false;
        }
        if (requiresApproval == null) {
            requiresApproval = false;
        }
    }
}
