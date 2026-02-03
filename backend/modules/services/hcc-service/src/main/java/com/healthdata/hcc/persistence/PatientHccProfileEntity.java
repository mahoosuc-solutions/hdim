package com.healthdata.hcc.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entity storing patient HCC profile and RAF scores.
 *
 * Tracks both V24 and V28 model scores during the transition period,
 * plus the blended score used for actual payment.
 */
@Entity
@Table(name = "patient_hcc_profiles", schema = "hcc",
    indexes = {
        @Index(name = "idx_patient_hcc_tenant_patient", columnList = "tenant_id, patient_id"),
        @Index(name = "idx_patient_hcc_year", columnList = "tenant_id, profile_year")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientHccProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "profile_year", nullable = false)
    private Integer profileYear;

    // ========================================================================
    // RAF SCORES
    // ========================================================================

    /**
     * Risk Adjustment Factor score under V24 model.
     */
    @Column(name = "raf_score_v24", precision = 8, scale = 5)
    private BigDecimal rafScoreV24;

    /**
     * Risk Adjustment Factor score under V28 model.
     */
    @Column(name = "raf_score_v28", precision = 8, scale = 5)
    private BigDecimal rafScoreV28;

    /**
     * Blended RAF score based on CMS transition weights.
     * 2024: 67% V24 + 33% V28
     * 2025: 33% V24 + 67% V28
     * 2026+: 100% V28
     */
    @Column(name = "raf_score_blended", precision = 8, scale = 5)
    private BigDecimal rafScoreBlended;

    // ========================================================================
    // HCC LISTS
    // ========================================================================

    /**
     * List of HCC codes captured under V24 model.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hccs_v24", columnDefinition = "jsonb")
    private List<String> hccsV24;

    /**
     * List of HCC codes captured under V28 model.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hccs_v28", columnDefinition = "jsonb")
    private List<String> hccsV28;

    /**
     * Diagnosis codes used for HCC calculation.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diagnosis_codes", columnDefinition = "jsonb")
    private List<String> diagnosisCodes;

    // ========================================================================
    // DOCUMENTATION GAPS
    // ========================================================================

    /**
     * List of identified documentation gaps (as JSON).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "documentation_gaps", columnDefinition = "jsonb")
    private List<DocumentationGapSummary> documentationGaps;

    /**
     * Count of documentation gaps.
     */
    @Column(name = "documentation_gap_count")
    private Integer documentationGapCount;

    /**
     * Potential RAF uplift if all documentation gaps are addressed.
     */
    @Column(name = "potential_raf_uplift", precision = 8, scale = 5)
    private BigDecimal potentialRafUplift;

    // ========================================================================
    // RECAPTURE
    // ========================================================================

    /**
     * Count of HCCs from prior year that need recapture.
     */
    @Column(name = "recapture_opportunities_count")
    private Integer recaptureOpportunitiesCount;

    /**
     * Potential RAF value from recapture.
     */
    @Column(name = "recapture_raf_value", precision = 8, scale = 5)
    private BigDecimal recaptureRafValue;

    // ========================================================================
    // METADATA
    // ========================================================================

    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        lastCalculatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Summary of a documentation gap for JSON storage.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DocumentationGapSummary {
        private String currentIcd10;
        private String recommendedIcd10;
        private String gapType;
        private BigDecimal rafImpact;
        private String guidance;
    }
}
