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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Measure Config Profile Entity
 * Reusable configuration templates for measure parameters targeting specific patient populations.
 * Maps to the measure_config_profiles table created in migration 0036.
 *
 * Examples: "Elderly Diabetic Patients 75+", "CHF High-Risk", "CKD Stage 4+"
 * Supports priority-based override resolution when multiple profiles apply.
 */
@Hidden
@Schema(hidden = true)
@Entity
@Table(name = "measure_config_profiles",
    indexes = {
        @Index(name = "idx_mcp_tenant", columnList = "tenant_id"),
        @Index(name = "idx_mcp_measure", columnList = "measure_id, active"),
        @Index(name = "idx_mcp_profile_code", columnList = "profile_code"),
        @Index(name = "idx_mcp_priority", columnList = "priority DESC, active")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_mcp_measure_profile_code", columnNames = {"measure_id", "profile_code"})
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasureConfigProfileEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    // Core References
    @Column(name = "measure_id", nullable = false)
    private UUID measureId;

    // Profile Identity
    @Column(name = "profile_name", nullable = false)
    private String profileName;

    @Column(name = "profile_code", nullable = false, length = 100)
    private String profileCode; // e.g., ELDERLY_DM_75PLUS, CHF_HIGH_RISK

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Population Matching (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "population_criteria", columnDefinition = "jsonb")
    private Map<String, Object> populationCriteria;

    // Configuration Overrides (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_overrides", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> configOverrides;

    // Priority (higher wins when multiple profiles match)
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 0;

    // Status
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_until")
    private LocalDate effectiveUntil;

    // Approval
    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    // Audit Fields
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (priority == null) {
            priority = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
