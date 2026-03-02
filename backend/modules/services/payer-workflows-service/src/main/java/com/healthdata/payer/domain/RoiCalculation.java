package com.healthdata.payer.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Saved ROI calculation from the HDIM ROI Calculator.
 *
 * Stores inputs, calculated results, and optional lead-capture contact info.
 * Calculations use BigDecimal for financial precision — formulas match
 * the frontend ROICalculator.tsx exactly.
 *
 * HIPAA Note: No PHI stored. Contains business-level financial projections only.
 */
@Entity
@Table(name = "roi_calculations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoiCalculation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id")
    private String tenantId;

    // ==================== Input Fields ====================

    @Column(name = "org_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private OrgType orgType;

    @Column(name = "patient_population", nullable = false)
    private Integer patientPopulation;

    @Column(name = "current_quality_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal currentQualityScore;

    @Column(name = "current_star_rating", nullable = false, precision = 3, scale = 1)
    private BigDecimal currentStarRating;

    @Column(name = "manual_reporting_hours", nullable = false)
    private Integer manualReportingHours;

    // ==================== Calculated Result Fields ====================

    @Column(name = "quality_improvement", precision = 5, scale = 1)
    private BigDecimal qualityImprovement;

    @Column(name = "projected_score", precision = 5, scale = 1)
    private BigDecimal projectedScore;

    @Column(name = "star_improvement", precision = 3, scale = 1)
    private BigDecimal starImprovement;

    @Column(name = "projected_star_rating", precision = 3, scale = 1)
    private BigDecimal projectedStarRating;

    @Column(name = "quality_bonuses", precision = 14, scale = 2)
    private BigDecimal qualityBonuses;

    @Column(name = "admin_savings", precision = 14, scale = 2)
    private BigDecimal adminSavings;

    @Column(name = "gap_closure_value", precision = 14, scale = 2)
    private BigDecimal gapClosureValue;

    @Column(name = "total_year1_value", precision = 14, scale = 2)
    private BigDecimal totalYear1Value;

    @Column(name = "year1_investment", precision = 14, scale = 2)
    private BigDecimal year1Investment;

    @Column(name = "year1_roi", precision = 10, scale = 2)
    private BigDecimal year1ROI;

    @Column(name = "payback_days", precision = 6, scale = 0)
    private BigDecimal paybackDays;

    @Column(name = "three_year_npv", precision = 14, scale = 2)
    private BigDecimal threeYearNPV;

    // ==================== Lead Capture Fields ====================

    @Column(name = "contact_name", length = 255)
    private String contactName;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_company", length = 255)
    private String contactCompany;

    // ==================== Audit Fields ====================

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

    public enum OrgType {
        ACO("ACO / MSSP", "0.25"),
        HEALTH_SYSTEM("Health System", "0.23"),
        HIE("Health Information Exchange", "0.20"),
        PAYER("Health Plan / Payer", "0.28"),
        FQHC("FQHC / Community Health", "0.22");

        public final String displayName;
        public final BigDecimal baseImprovement;

        OrgType(String displayName, String baseImprovement) {
            this.displayName = displayName;
            this.baseImprovement = new BigDecimal(baseImprovement);
        }
    }
}
