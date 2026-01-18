package com.healthdata.caregap.event;

import java.time.Instant;

public class InterventionRecommendedEvent {
    private final String tenantId;
    private final String patientId;
    private final String gapCode;
    private final String intervention;
    private final String priority;
    private final Instant timestamp;

    public InterventionRecommendedEvent(String tenantId, String patientId, String gapCode, String intervention, String priority) {
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.gapCode = gapCode;
        this.intervention = intervention;
        this.priority = priority;
        this.timestamp = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public String getGapCode() { return gapCode; }
    public String getIntervention() { return intervention; }
    public String getPriority() { return priority; }
    public Instant getTimestamp() { return timestamp; }
}
