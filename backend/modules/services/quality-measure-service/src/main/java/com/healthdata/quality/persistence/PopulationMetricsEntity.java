package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Population Metrics Entity (CQRS Read Model)
 *
 * Aggregated metrics for population-level dashboards and reports.
 * Updated daily or on-demand via scheduled job.
 *
 * Benefits:
 * - Pre-calculated aggregations
 * - Fast dashboard queries
 * - Historical trending
 */
@Entity
@Table(name = "population_metrics")
@Data
public class PopulationMetricsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    // Population Counts
    @Column(name = "total_patients", nullable = false)
    private Integer totalPatients = 0;

    @Column(name = "high_risk_count", nullable = false)
    private Integer highRiskCount = 0;

    @Column(name = "medium_risk_count", nullable = false)
    private Integer mediumRiskCount = 0;

    @Column(name = "low_risk_count", nullable = false)
    private Integer lowRiskCount = 0;

    // Health Score Metrics
    @Column(name = "average_health_score")
    private Double averageHealthScore;

    @Column(name = "median_health_score")
    private Double medianHealthScore;

    @Column(name = "health_score_stddev")
    private Double healthScoreStddev;

    // Care Gap Metrics
    @Column(name = "total_care_gaps", nullable = false)
    private Integer totalCareGaps = 0;

    @Column(name = "urgent_care_gaps", nullable = false)
    private Integer urgentCareGaps = 0;

    @Column(name = "care_gaps_closed_today", nullable = false)
    private Integer careGapsClosedToday = 0;

    @Column(name = "gap_closure_rate")
    private Double gapClosureRate;

    // Alert Metrics
    @Column(name = "total_active_alerts", nullable = false)
    private Integer totalActiveAlerts = 0;

    @Column(name = "critical_alerts", nullable = false)
    private Integer criticalAlerts = 0;

    // Mental Health Metrics
    @Column(name = "positive_mental_health_screens", nullable = false)
    private Integer positiveMentalHealthScreens = 0;

    @Column(name = "mental_health_assessments_completed", nullable = false)
    private Integer mentalHealthAssessmentsCompleted = 0;

    // Quality Measure Metrics
    @Column(name = "average_quality_score")
    private Double averageQualityScore;

    @Column(name = "measures_calculated", nullable = false)
    private Integer measuresCalculated = 0;

    // Audit
    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (calculatedAt == null) {
            calculatedAt = Instant.now();
        }
        if (metricDate == null) {
            metricDate = LocalDate.now();
        }
    }
}
