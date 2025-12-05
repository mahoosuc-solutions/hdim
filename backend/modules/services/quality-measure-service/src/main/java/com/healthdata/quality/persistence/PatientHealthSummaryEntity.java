package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Patient Health Summary Entity (CQRS Read Model)
 *
 * Denormalized view of patient health status for fast queries.
 * Updated via event-driven projections from write model.
 *
 * Benefits:
 * - Single table query (no joins)
 * - Optimized for dashboard/reporting
 * - Independent scaling from write model
 *
 * Updated by events:
 * - health-score.updated
 * - care-gap.auto-closed
 * - risk-assessment.updated
 * - clinical-alert.triggered
 */
@Entity
@Table(name = "patient_health_summary")
@Data
public class PatientHealthSummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false, length = 100)
    private String patientId;

    // Health Score Data
    @Column(name = "latest_health_score")
    private Double latestHealthScore;

    @Column(name = "health_trend", length = 50)
    private String healthTrend; // improving, declining, stable

    @Column(name = "health_score_updated_at")
    private Instant healthScoreUpdatedAt;

    // Care Gap Data
    @Column(name = "open_care_gaps_count", nullable = false)
    private Integer openCareGapsCount = 0;

    @Column(name = "urgent_gaps_count", nullable = false)
    private Integer urgentGapsCount = 0;

    @Column(name = "care_gaps_updated_at")
    private Instant careGapsUpdatedAt;

    // Risk Assessment Data
    @Column(name = "risk_level", length = 50)
    private String riskLevel; // low, medium, high

    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "risk_updated_at")
    private Instant riskUpdatedAt;

    // Clinical Alert Data
    @Column(name = "active_alerts_count", nullable = false)
    private Integer activeAlertsCount = 0;

    @Column(name = "critical_alerts_count", nullable = false)
    private Integer criticalAlertsCount = 0;

    @Column(name = "alerts_updated_at")
    private Instant alertsUpdatedAt;

    // Mental Health Data
    @Column(name = "latest_mental_health_score")
    private Integer latestMentalHealthScore;

    @Column(name = "mental_health_severity", length = 50)
    private String mentalHealthSeverity;

    @Column(name = "mental_health_updated_at")
    private Instant mentalHealthUpdatedAt;

    // Audit
    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt = Instant.now();

    @Column(name = "projection_version", nullable = false)
    private Long projectionVersion = 1L;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdatedAt = Instant.now();
        this.projectionVersion++;
    }
}
