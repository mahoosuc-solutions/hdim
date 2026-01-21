package com.healthdata.qualityevent.event;

import java.time.Instant;

public class RiskScoreCalculatedEvent {
    private final String tenantId;
    private final String patientId;
    private final float riskScore;
    private final String reason;
    private final Instant timestamp;

    public RiskScoreCalculatedEvent(String tenantId, String patientId, float riskScore, String reason) {
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.riskScore = riskScore;
        this.reason = reason;
        this.timestamp = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public float getRiskScore() { return riskScore; }
    public String getReason() { return reason; }
    public Instant getTimestamp() { return timestamp; }
}
