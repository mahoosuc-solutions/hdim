package com.healthdata.qualityevent.event;

import java.time.Instant;

public class MeasureDenominatorStatusEvent {
    private final String tenantId;
    private final String patientId;
    private final String measureCode;
    private final boolean inDenominator;
    private final String reason;
    private final Instant timestamp;

    public MeasureDenominatorStatusEvent(String tenantId, String patientId, String measureCode, boolean inDenominator, String reason) {
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.measureCode = measureCode;
        this.inDenominator = inDenominator;
        this.reason = reason;
        this.timestamp = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public String getMeasureCode() { return measureCode; }
    public boolean isInDenominator() { return inDenominator; }
    public String getReason() { return reason; }
    public Instant getTimestamp() { return timestamp; }
}
