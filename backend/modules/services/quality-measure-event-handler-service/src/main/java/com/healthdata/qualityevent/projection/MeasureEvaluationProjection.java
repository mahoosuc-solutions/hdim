package com.healthdata.qualityevent.projection;

import java.time.Instant;
import java.time.LocalDate;

/**
 * MeasureEvaluationProjection - Denormalized read model for measure evaluation
 *
 * Built from quality measure events via event sourcing.
 * Optimized for fast queries (measure lookup, status checks).
 */
public class MeasureEvaluationProjection {
    private final String patientId;
    private final String tenantId;
    private final String measureCode;
    private String measureDescription;
    private float score;
    private String status;  // MET, NOT_MET, PENDING
    private String evaluationReason;
    private boolean inNumerator;
    private boolean inDenominator;
    private LocalDate evaluationDate;
    private long version;
    private Instant lastUpdated;

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

    // Getters
    public String getPatientId() { return patientId; }
    public String getTenantId() { return tenantId; }
    public String getMeasureCode() { return measureCode; }
    public String getMeasureDescription() { return measureDescription; }
    public float getScore() { return score; }
    public String getStatus() { return status; }
    public String getEvaluationReason() { return evaluationReason; }
    public boolean isInNumerator() { return inNumerator; }
    public boolean isInDenominator() { return inDenominator; }
    public LocalDate getEvaluationDate() { return evaluationDate; }
    public long getVersion() { return version; }
    public Instant getLastUpdated() { return lastUpdated; }

    // Setters
    public void setMeasureDescription(String measureDescription) { this.measureDescription = measureDescription; }
    public void setScore(float score) { this.score = score; }
    public void setStatus(String status) { this.status = status; }
    public void setEvaluationReason(String evaluationReason) { this.evaluationReason = evaluationReason; }
    public void setInNumerator(boolean inNumerator) { this.inNumerator = inNumerator; }
    public void setInDenominator(boolean inDenominator) { this.inDenominator = inDenominator; }
    public void setEvaluationDate(LocalDate evaluationDate) { this.evaluationDate = evaluationDate; }
    public void setVersion(long version) { this.version = version; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }

    public void incrementVersion() {
        this.version++;
        this.lastUpdated = Instant.now();
    }
}
