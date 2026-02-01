package com.healthdata.qualityevent.event;

import java.time.Instant;

public class MeasureNumeratorStatusEvent {
    private final String tenantId;
    private final String patientId;
    private final String measureCode;
    private final boolean inNumerator;
    private final String reason;
    private final Instant timestamp;

    public MeasureNumeratorStatusEvent(String tenantId, String patientId, String measureCode, boolean inNumerator, String reason) {
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.measureCode = measureCode;
        this.inNumerator = inNumerator;
        this.reason = reason;
        this.timestamp = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public String getMeasureCode() { return measureCode; }
    public boolean isInNumerator() { return inNumerator; }
    public String getReason() { return reason; }
    public Instant getTimestamp() { return timestamp; }
}
