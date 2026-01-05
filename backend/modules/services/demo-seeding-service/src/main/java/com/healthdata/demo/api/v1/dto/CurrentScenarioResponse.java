package com.healthdata.demo.api.v1.dto;

import java.time.Instant;

/**
 * Response DTO for current scenario info.
 */
public class CurrentScenarioResponse {
    private String name;
    private String displayName;
    private String description;
    private int patientCount;
    private String sessionId;
    private String status;
    private Instant startedAt;
    private Instant lastResetAt;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPatientCount() { return patientCount; }
    public void setPatientCount(int patientCount) { this.patientCount = patientCount; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getLastResetAt() { return lastResetAt; }
    public void setLastResetAt(Instant lastResetAt) { this.lastResetAt = lastResetAt; }
}
