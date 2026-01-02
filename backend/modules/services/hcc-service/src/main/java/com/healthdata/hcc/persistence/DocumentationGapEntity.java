package com.healthdata.hcc.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity tracking HCC documentation gaps for a patient.
 *
 * Documentation gaps are identified when:
 * - A diagnosis code is non-specific but could be more specific (e.g., unspecified diabetes)
 * - A code is missing laterality
 * - A chronic condition needs annual revalidation
 * - V28 requires more specificity than V24
 */
@Entity
@Table(name = "documentation_gaps", schema = "hcc",
    indexes = {
        @Index(name = "idx_doc_gap_tenant_patient", columnList = "tenant_id, patient_id"),
        @Index(name = "idx_doc_gap_status", columnList = "tenant_id, status"),
        @Index(name = "idx_doc_gap_type", columnList = "tenant_id, gap_type")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentationGapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "profile_year", nullable = false)
    private Integer profileYear;

    /**
     * Current diagnosis code in the patient's record.
     */
    @Column(name = "current_icd10", nullable = false, length = 10)
    private String currentIcd10;

    @Column(name = "current_icd10_description", length = 500)
    private String currentIcd10Description;

    /**
     * Current HCC (if any) from the current code.
     */
    @Column(name = "current_hcc_v24", length = 10)
    private String currentHccV24;

    @Column(name = "current_hcc_v28", length = 10)
    private String currentHccV28;

    /**
     * Recommended diagnosis code for more specificity.
     */
    @Column(name = "recommended_icd10", length = 10)
    private String recommendedIcd10;

    @Column(name = "recommended_icd10_description", length = 500)
    private String recommendedIcd10Description;

    /**
     * HCC that would result from the recommended code.
     */
    @Column(name = "recommended_hcc_v24", length = 10)
    private String recommendedHccV24;

    @Column(name = "recommended_hcc_v28", length = 10)
    private String recommendedHccV28;

    /**
     * Type of documentation gap.
     */
    @Column(name = "gap_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private GapType gapType;

    /**
     * RAF impact if the gap is addressed (positive value = increase).
     */
    @Column(name = "raf_impact_v24", precision = 8, scale = 5)
    private BigDecimal rafImpactV24;

    @Column(name = "raf_impact_v28", precision = 8, scale = 5)
    private BigDecimal rafImpactV28;

    @Column(name = "raf_impact_blended", precision = 8, scale = 5)
    private BigDecimal rafImpactBlended;

    /**
     * Priority of addressing this gap (based on RAF impact).
     */
    @Column(name = "priority", length = 20)
    private String priority;

    /**
     * Clinical guidance for addressing the gap.
     */
    @Column(name = "clinical_guidance", columnDefinition = "text")
    private String clinicalGuidance;

    /**
     * Required documentation elements to address the gap.
     */
    @Column(name = "required_documentation", columnDefinition = "text")
    private String requiredDocumentation;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private GapStatus status;

    @Column(name = "addressed_by", length = 100)
    private String addressedBy;

    @Column(name = "addressed_at")
    private LocalDateTime addressedAt;

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
        if (status == null) {
            status = GapStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Types of documentation gaps.
     */
    public enum GapType {
        UNSPECIFIED,            // Diagnosis is unspecified (e.g., "unspecified diabetes")
        MISSING_LATERALITY,     // Laterality not documented
        MISSING_COMPLICATION,   // Complications not documented
        MISSING_SEVERITY,       // Severity not documented
        MISSING_TYPE,           // Type not specified (e.g., Type 1 vs Type 2)
        V28_SPECIFICITY,        // V28 requires more specificity than V24
        CHRONIC_REVALIDATION,   // Chronic condition needs annual revalidation
        SUSPECT_CONDITION       // Suspected condition based on clinical indicators
    }

    /**
     * Documentation gap status.
     */
    public enum GapStatus {
        OPEN,           // Gap identified, not yet addressed
        IN_PROGRESS,    // Being worked on by coder/provider
        ADDRESSED,      // Documentation updated
        REJECTED,       // Gap determined to be invalid
        DEFERRED        // Deferred to next encounter
    }
}
