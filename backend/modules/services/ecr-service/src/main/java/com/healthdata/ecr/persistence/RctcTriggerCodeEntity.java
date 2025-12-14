package com.healthdata.ecr.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing RCTC (Reportable Condition Trigger Codes) from CDC.
 *
 * RCTC is a CDC-managed set of value sets containing codes that may indicate
 * a reportable condition. These codes trigger the eCR process when detected
 * in clinical data.
 *
 * Code systems include:
 * - ICD-10-CM (diagnoses)
 * - SNOMED CT (diagnoses, findings)
 * - LOINC (lab tests)
 * - RXNORM (medications)
 * - CPT (procedures)
 *
 * @see <a href="https://rctc.cdc.gov/">RCTC Value Sets</a>
 */
@Entity
@Table(name = "rctc_trigger_codes", schema = "ecr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(RctcTriggerCodeEntity.RctcTriggerCodeId.class)
public class RctcTriggerCodeEntity {

    @Id
    @Column(name = "code", nullable = false, length = 128)
    private String code;

    @Id
    @Column(name = "code_system", nullable = false, length = 255)
    private String codeSystem;

    /**
     * Human-readable display name for the code
     */
    @Column(name = "display", length = 500)
    private String display;

    /**
     * Type of trigger: DIAGNOSIS, LAB_ORDER, LAB_RESULT, MEDICATION, PROCEDURE
     */
    @Column(name = "trigger_type", nullable = false, length = 64)
    @Enumerated(EnumType.STRING)
    private TriggerType triggerType;

    /**
     * The reportable condition this code triggers
     */
    @Column(name = "condition_name", nullable = false, length = 255)
    private String conditionName;

    /**
     * OID of the value set this code belongs to
     */
    @Column(name = "value_set_oid", length = 128)
    private String valueSetOid;

    /**
     * Name of the value set
     */
    @Column(name = "value_set_name", length = 255)
    private String valueSetName;

    /**
     * Urgency of reporting when this trigger is detected
     */
    @Column(name = "urgency", length = 32)
    @Enumerated(EnumType.STRING)
    private Urgency urgency;

    /**
     * Whether this code is currently active in RCTC
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    /**
     * Version of RCTC value set this code came from
     */
    @Column(name = "rctc_version", length = 32)
    private String rctcVersion;

    /**
     * Date this code was added to RCTC
     */
    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    /**
     * Date this code was retired from RCTC (if applicable)
     */
    @Column(name = "retirement_date")
    private LocalDate retirementDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum TriggerType {
        DIAGNOSIS,       // ICD-10-CM, SNOMED CT diagnosis codes
        LAB_ORDER,       // LOINC codes for lab orders
        LAB_RESULT,      // LOINC codes with specific result values
        MEDICATION,      // RXNORM medication codes
        PROCEDURE        // CPT, SNOMED procedure codes
    }

    public enum Urgency {
        IMMEDIATE,        // Must report immediately (bioterrorism agents)
        WITHIN_24_HOURS,  // Report within 24 hours
        WITHIN_72_HOURS,  // Report within 72 hours
        ROUTINE           // Standard reporting timeframe
    }

    /**
     * Composite primary key class
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RctcTriggerCodeId implements java.io.Serializable {
        private String code;
        private String codeSystem;
    }
}
