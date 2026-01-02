package com.healthdata.sdoh.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking CMS-mandated HRSN (Health-Related Social Needs) Screening Sessions.
 *
 * Tracks completion and results for all 5 HRSN domains required by CMS SDOH-1/SDOH-2 measures:
 * 1. Food Insecurity
 * 2. Housing Instability
 * 3. Transportation Needs
 * 4. Utility Difficulties
 * 5. Interpersonal Safety
 *
 * Regulatory Requirements:
 * - Mandatory for inpatient as of 2024
 * - Expanding to outpatient settings 2025
 * - Required for MSSP ACOs via APP Plus starting 2028
 * - Billing code G0136 for SDOH risk assessment
 *
 * @see <a href="https://www.cms.gov/priorities/health-equity/sdoh">CMS SDOH Requirements</a>
 */
@Entity
@Table(name = "hrsn_screening_sessions", schema = "sdoh",
    indexes = {
        @Index(name = "idx_hrsn_session_tenant_patient", columnList = "tenant_id, patient_id"),
        @Index(name = "idx_hrsn_session_date", columnList = "tenant_id, screening_date"),
        @Index(name = "idx_hrsn_session_status", columnList = "tenant_id, status")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrsnScreeningSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "encounter_id")
    private UUID encounterId;

    @Column(name = "screening_date", nullable = false)
    private LocalDateTime screeningDate;

    @Column(name = "screening_tool", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ScreeningTool screeningTool;

    // ========================================================================
    // DOMAIN 1: FOOD INSECURITY (LOINC: 88122-7, 88123-5)
    // ========================================================================
    @Column(name = "food_insecurity_completed")
    private Boolean foodInsecurityCompleted;

    @Column(name = "food_insecurity_positive")
    private Boolean foodInsecurityPositive;

    @Column(name = "food_insecurity_response", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String foodInsecurityResponse;

    // ========================================================================
    // DOMAIN 2: HOUSING INSTABILITY (LOINC: 71802-3)
    // ========================================================================
    @Column(name = "housing_instability_completed")
    private Boolean housingInstabilityCompleted;

    @Column(name = "housing_instability_positive")
    private Boolean housingInstabilityPositive;

    @Column(name = "housing_instability_response", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String housingInstabilityResponse;

    // ========================================================================
    // DOMAIN 3: TRANSPORTATION (LOINC: 93030-5)
    // ========================================================================
    @Column(name = "transportation_completed")
    private Boolean transportationCompleted;

    @Column(name = "transportation_positive")
    private Boolean transportationPositive;

    @Column(name = "transportation_response", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String transportationResponse;

    // ========================================================================
    // DOMAIN 4: UTILITIES (LOINC: 93031-3)
    // ========================================================================
    @Column(name = "utilities_completed")
    private Boolean utilitiesCompleted;

    @Column(name = "utilities_positive")
    private Boolean utilitiesPositive;

    @Column(name = "utilities_response", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String utilitiesResponse;

    // ========================================================================
    // DOMAIN 5: INTERPERSONAL SAFETY (LOINC: 93038-8, 93039-6, 93040-4, 93041-2)
    // ========================================================================
    @Column(name = "interpersonal_safety_completed")
    private Boolean interpersonalSafetyCompleted;

    @Column(name = "interpersonal_safety_positive")
    private Boolean interpersonalSafetyPositive;

    @Column(name = "interpersonal_safety_response", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String interpersonalSafetyResponse;

    // ========================================================================
    // COMPUTED/AGGREGATE FIELDS
    // ========================================================================

    /**
     * True when all 5 HRSN domains have been screened.
     * Used for SDOH-1 measure numerator.
     */
    @Column(name = "all_domains_screened")
    private Boolean allDomainsScreened;

    /**
     * True when at least one domain has a positive result.
     * Used for SDOH-2 measure numerator.
     */
    @Column(name = "any_domain_positive")
    private Boolean anyDomainPositive;

    /**
     * Count of domains that screened positive (0-5).
     */
    @Column(name = "positive_domain_count")
    private Integer positiveDomainCount;

    // ========================================================================
    // BILLING
    // ========================================================================

    /**
     * HCPCS billing code for SDOH risk assessment (G0136).
     */
    @Column(name = "billing_code", length = 20)
    private String billingCode;

    /**
     * Whether billing code was applied to a claim.
     */
    @Column(name = "billing_applied")
    private Boolean billingApplied;

    // ========================================================================
    // STATUS & METADATA
    // ========================================================================

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ScreeningStatus status;

    @Column(name = "screened_by", length = 100)
    private String screenedBy;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

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
        updateComputedFields();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updateComputedFields();
    }

    /**
     * Updates computed fields based on domain completion and results.
     */
    public void updateComputedFields() {
        // All domains screened check
        this.allDomainsScreened = Boolean.TRUE.equals(foodInsecurityCompleted)
            && Boolean.TRUE.equals(housingInstabilityCompleted)
            && Boolean.TRUE.equals(transportationCompleted)
            && Boolean.TRUE.equals(utilitiesCompleted)
            && Boolean.TRUE.equals(interpersonalSafetyCompleted);

        // Any domain positive check
        this.anyDomainPositive = Boolean.TRUE.equals(foodInsecurityPositive)
            || Boolean.TRUE.equals(housingInstabilityPositive)
            || Boolean.TRUE.equals(transportationPositive)
            || Boolean.TRUE.equals(utilitiesPositive)
            || Boolean.TRUE.equals(interpersonalSafetyPositive);

        // Count positive domains
        int count = 0;
        if (Boolean.TRUE.equals(foodInsecurityPositive)) count++;
        if (Boolean.TRUE.equals(housingInstabilityPositive)) count++;
        if (Boolean.TRUE.equals(transportationPositive)) count++;
        if (Boolean.TRUE.equals(utilitiesPositive)) count++;
        if (Boolean.TRUE.equals(interpersonalSafetyPositive)) count++;
        this.positiveDomainCount = count;
    }

    /**
     * Supported SDOH screening tools.
     */
    public enum ScreeningTool {
        AHC_HRSN,       // Accountable Health Communities HRSN
        PRAPARE,        // Protocol for Responding to and Assessing Patients' Assets, Risks, and Experiences
        WE_CARE,        // WE CARE Survey
        CUSTOM          // Organization-specific tool
    }

    /**
     * Screening session status.
     */
    public enum ScreeningStatus {
        IN_PROGRESS,    // Screening started but not complete
        COMPLETED,      // All questions answered
        DECLINED,       // Patient declined screening
        DEFERRED,       // Screening deferred to future visit
        CANCELLED       // Screening cancelled
    }
}
