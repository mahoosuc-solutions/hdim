package com.healthdata.quality.persistence;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Quality Measure Entity
 * Stores quality measure definitions (HEDIS, CMS, custom measures).
 * Maps to the quality_measures table created in migration 0026.
 */
@Hidden
@Schema(hidden = true)
@Entity
@Table(name = "quality_measures",
    indexes = {
        @Index(name = "idx_quality_measures_tenant", columnList = "tenant_id"),
        @Index(name = "idx_quality_measures_set", columnList = "measure_set"),
        @Index(name = "idx_quality_measures_domain", columnList = "domain"),
        @Index(name = "idx_quality_measures_active", columnList = "active")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityMeasureEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    // Measure Identity
    @Column(name = "measure_id", nullable = false, unique = true, length = 128)
    private String measureId;

    @Column(name = "measure_name", nullable = false, length = 255)
    private String measureName;

    @Column(name = "measure_set", nullable = false, length = 50)
    private String measureSet; // HEDIS, CMS, custom

    @Column(name = "version", nullable = false, length = 50)
    private String version;

    // Classification
    @Column(name = "domain", nullable = false, length = 100)
    private String domain;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "measure_type", nullable = false, length = 50)
    private String measureType; // proportion, ratio, continuous-variable

    // Details
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "rationale", columnDefinition = "TEXT")
    private String rationale;

    @Column(name = "guidance", columnDefinition = "TEXT")
    private String guidance;

    // CQL Integration
    @Column(name = "cql_library_id")
    private UUID cqlLibraryId;

    @Column(name = "calculation_logic", columnDefinition = "TEXT")
    private String calculationLogic;

    // Scoring
    @Column(name = "scoring_method", nullable = false, length = 50)
    private String scoringMethod;

    @Column(name = "improvement_notation", length = 50)
    private String improvementNotation; // higher-is-better, lower-is-better

    // Status
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "effective_period_start", nullable = false)
    private LocalDate effectivePeriodStart;

    @Column(name = "effective_period_end")
    private LocalDate effectivePeriodEnd;

    // Audit
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
