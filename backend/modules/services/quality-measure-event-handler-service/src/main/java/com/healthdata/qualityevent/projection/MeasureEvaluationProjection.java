package com.healthdata.qualityevent.projection;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDate;

/**
 * MeasureEvaluationProjection - Denormalized read model for measure evaluation
 *
 * Built from quality measure events via event sourcing.
 * Optimized for fast queries (measure lookup, status checks).
 */
@Entity(name = "MeasureEvaluationHandlerProjection")
@Table(name = "measure_evaluations")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MeasureEvaluationProjection {
    @Id
    @Column(name = "id")
    private String id; // Composite key: patientId + "_" + tenantId + "_" + measureCode

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "measure_code", nullable = false)
    private String measureCode;

    @Column(name = "measure_description")
    private String measureDescription;

    @Column(name = "score")
    private float score;

    @Column(name = "status")
    private String status;  // MET, NOT_MET, PENDING

    @Column(name = "evaluation_reason")
    private String evaluationReason;

    @Column(name = "in_numerator")
    private boolean inNumerator;

    @Column(name = "in_denominator")
    private boolean inDenominator;

    @Column(name = "risk_level")
    private String riskLevel;  // LOW, MEDIUM, HIGH, VERY_HIGH

    @Column(name = "evaluation_date")
    private LocalDate evaluationDate;

    @Column(name = "version")
    private long version;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    /**
     * Custom constructor for convenience
     * Used by event handlers to create projections from events
     */
    public MeasureEvaluationProjection(String patientId, String tenantId, String measureCode, String measureDescription) {
        this.patientId = patientId;
        this.tenantId = tenantId;
        this.measureCode = measureCode;
        this.measureDescription = measureDescription;
        this.status = "PENDING";
        this.inNumerator = false;
        this.inDenominator = false;
        this.version = 1L;
        this.lastUpdated = Instant.now();
        this.evaluationDate = LocalDate.now();
    }

    // Getters, setters, and equals/hashCode provided by @Data

    /**
     * Increment version and update timestamp
     */
    public void incrementVersion() {
        this.version++;
        this.lastUpdated = Instant.now();
    }
}
