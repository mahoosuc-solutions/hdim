package com.healthdata.qualityevent.event;

import java.time.Instant;

public class MeasureScoreCalculatedEvent {
    private final String tenantId;
    private final String patientId;
    private final String measureCode;
    private final float score;
    private final String reason;
    private final Instant timestamp;

    public MeasureScoreCalculatedEvent(String tenantId, String patientId, String measureCode, float score, String reason) {
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.measureCode = measureCode;
        this.score = score;
        this.reason = reason;
        this.timestamp = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public String getMeasureCode() { return measureCode; }
    public float getScore() { return score; }
    public String getReason() { return reason; }
    public Instant getTimestamp() { return timestamp; }
}
