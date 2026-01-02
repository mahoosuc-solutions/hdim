package com.healthdata.hcc.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity tracking HCC recapture opportunities.
 *
 * HCC recapture refers to chronic conditions that were documented in a prior year
 * but have not yet been documented in the current year. These need to be
 * re-documented annually to maintain RAF score.
 */
@Entity
@Table(name = "recapture_opportunities", schema = "hcc",
    indexes = {
        @Index(name = "idx_recapture_tenant_patient", columnList = "tenant_id, patient_id"),
        @Index(name = "idx_recapture_year", columnList = "tenant_id, current_year"),
        @Index(name = "idx_recapture_status", columnList = "tenant_id, is_recaptured")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecaptureOpportunityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    /**
     * Year the HCC was last documented.
     */
    @Column(name = "prior_year", nullable = false)
    private Integer priorYear;

    /**
     * Current year for recapture.
     */
    @Column(name = "current_year", nullable = false)
    private Integer currentYear;

    /**
     * HCC code that needs recapture.
     */
    @Column(name = "hcc_code", nullable = false, length = 10)
    private String hccCode;

    @Column(name = "hcc_name", length = 255)
    private String hccName;

    /**
     * Original diagnosis code from prior year.
     */
    @Column(name = "prior_year_icd10", length = 10)
    private String priorYearIcd10;

    @Column(name = "prior_year_icd10_description", length = 500)
    private String priorYearIcd10Description;

    /**
     * RAF value of this HCC.
     */
    @Column(name = "raf_value_v24", precision = 8, scale = 5)
    private BigDecimal rafValueV24;

    @Column(name = "raf_value_v28", precision = 8, scale = 5)
    private BigDecimal rafValueV28;

    /**
     * Whether this HCC has been recaptured in the current year.
     */
    @Column(name = "is_recaptured", nullable = false)
    private Boolean isRecaptured;

    /**
     * If recaptured, the diagnosis code used.
     */
    @Column(name = "recaptured_icd10", length = 10)
    private String recapturedIcd10;

    @Column(name = "recaptured_at")
    private LocalDateTime recapturedAt;

    /**
     * Clinical guidance for recapture.
     */
    @Column(name = "clinical_guidance", columnDefinition = "text")
    private String clinicalGuidance;

    /**
     * Priority based on RAF value.
     */
    @Column(name = "priority", length = 20)
    private String priority;

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
        if (isRecaptured == null) {
            isRecaptured = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
