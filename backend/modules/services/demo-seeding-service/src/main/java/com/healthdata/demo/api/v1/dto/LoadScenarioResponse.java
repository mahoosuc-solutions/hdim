package com.healthdata.demo.api.v1.dto;

/**
 * Response DTO for scenario loading.
 */
public class LoadScenarioResponse {
    private String scenarioName;
    private String sessionId;
    private int patientCount;
    private int careGapCount;
    private long loadTimeMs;
    private boolean success;
    private String errorMessage;

    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public int getPatientCount() { return patientCount; }
    public void setPatientCount(int patientCount) { this.patientCount = patientCount; }

    public int getCareGapCount() { return careGapCount; }
    public void setCareGapCount(int careGapCount) { this.careGapCount = careGapCount; }

    public long getLoadTimeMs() { return loadTimeMs; }
    public void setLoadTimeMs(long loadTimeMs) { this.loadTimeMs = loadTimeMs; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
