package com.healthdata.caregap.event;

import java.time.Instant;

public class PatientQualifiedEvent {
    private final String tenantId;
    private final String patientId;
    private final String gapCode;
    private final boolean qualified;
    private final String reason;
    private final Instant timestamp;

    public PatientQualifiedEvent(String tenantId, String patientId, String gapCode, boolean qualified, String reason) {
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.gapCode = gapCode;
        this.qualified = qualified;
        this.reason = reason;
        this.timestamp = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public String getGapCode() { return gapCode; }
    public boolean isQualified() { return qualified; }
    public String getReason() { return reason; }
    public Instant getTimestamp() { return timestamp; }
}
