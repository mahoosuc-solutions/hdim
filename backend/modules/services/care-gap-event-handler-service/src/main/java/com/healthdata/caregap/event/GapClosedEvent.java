package com.healthdata.caregap.event;

import java.time.Instant;

public class GapClosedEvent {
    private final String tenantId;
    private final String patientId;
    private final String gapCode;
    private final String closureReason;
    private final String closureStatus;  // CLOSED, RESOLVED, WAIVED
    private final Instant timestamp;

    public GapClosedEvent(String tenantId, String patientId, String gapCode, String closureReason, String closureStatus) {
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.gapCode = gapCode;
        this.closureReason = closureReason;
        this.closureStatus = closureStatus;
        this.timestamp = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public String getGapCode() { return gapCode; }
    public String getClosureReason() { return closureReason; }
    public String getClosureStatus() { return closureStatus; }
    public Instant getTimestamp() { return timestamp; }
}
