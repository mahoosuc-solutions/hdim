package com.healthdata.caregap.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class GapClosedEvent {
    private final String tenantId;
    private final String patientId;
    private final String gapCode;
    private final String closureReason;
    private final String closureStatus;  // CLOSED, RESOLVED, WAIVED
    private final Instant timestamp;

    public GapClosedEvent(String tenantId, String patientId, String gapCode, String closureReason, String closureStatus) {
        this(tenantId, patientId, gapCode, closureReason, closureStatus, Instant.now());
    }

    @JsonCreator
    public GapClosedEvent(
        @JsonProperty("tenantId") String tenantId,
        @JsonProperty("patientId") String patientId,
        @JsonProperty("gapCode") String gapCode,
        @JsonProperty("closureReason") String closureReason,
        @JsonProperty("closureStatus") String closureStatus,
        @JsonProperty("timestamp") Instant timestamp
    ) {
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.gapCode = gapCode;
        this.closureReason = closureReason;
        this.closureStatus = closureStatus;
        this.timestamp = timestamp == null ? Instant.now() : timestamp;
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public String getGapCode() { return gapCode; }
    public String getClosureReason() { return closureReason; }
    public String getClosureStatus() { return closureStatus; }
    public Instant getTimestamp() { return timestamp; }
}
