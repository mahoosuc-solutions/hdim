package com.healthdata.qualityevent.projection;

import java.time.Instant;

public class RiskScoreProjection {
    private final String patientId;
    private final String tenantId;
    private float riskScore;
    private String riskLevel;  // LOW, MEDIUM, HIGH, VERY_HIGH
    private String reason;
    private long version;
    private Instant lastUpdated;

    public RiskScoreProjection(String patientId, String tenantId, float riskScore, String reason) {
        this.patientId = patientId;
        this.tenantId = tenantId;
        this.riskScore = riskScore;
        this.reason = reason;
        this.riskLevel = categorizeRiskLevel(riskScore);
        this.version = 1L;
        this.lastUpdated = Instant.now();
    }

    private String categorizeRiskLevel(float score) {
        if (score >= 0.90f) return "VERY_HIGH";
        if (score >= 0.70f) return "HIGH";
        if (score >= 0.40f) return "MEDIUM";
        return "LOW";
    }

    // Getters
    public String getPatientId() { return patientId; }
    public String getTenantId() { return tenantId; }
    public float getRiskScore() { return riskScore; }
    public String getRiskLevel() { return riskLevel; }
    public String getReason() { return reason; }
    public long getVersion() { return version; }
    public Instant getLastUpdated() { return lastUpdated; }

    // Setters
    public void setRiskScore(float riskScore) {
        this.riskScore = riskScore;
        this.riskLevel = categorizeRiskLevel(riskScore);
    }
    public void setReason(String reason) { this.reason = reason; }
    public void setVersion(long version) { this.version = version; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }

    public void incrementVersion() {
        this.version++;
        this.lastUpdated = Instant.now();
    }
}
