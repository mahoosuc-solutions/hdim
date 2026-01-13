package com.healthdata.demo.api.v1.dto;

/**
 * Response DTO for demo session progress.
 */
public class ProgressResponse {
    private String sessionId;
    private String scenarioName;
    private String tenantId;
    private String stage;
    private int progressPercent;
    private Integer patientsGenerated;
    private Integer patientsPersisted;
    private Integer careGapsCreated;
    private Integer measuresSeeded;
    private String message;
    private String updatedAt;
    private boolean cancelRequested;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }

    public Integer getPatientsGenerated() { return patientsGenerated; }
    public void setPatientsGenerated(Integer patientsGenerated) { this.patientsGenerated = patientsGenerated; }

    public Integer getPatientsPersisted() { return patientsPersisted; }
    public void setPatientsPersisted(Integer patientsPersisted) { this.patientsPersisted = patientsPersisted; }

    public Integer getCareGapsCreated() { return careGapsCreated; }
    public void setCareGapsCreated(Integer careGapsCreated) { this.careGapsCreated = careGapsCreated; }

    public Integer getMeasuresSeeded() { return measuresSeeded; }
    public void setMeasuresSeeded(Integer measuresSeeded) { this.measuresSeeded = measuresSeeded; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public boolean isCancelRequested() { return cancelRequested; }
    public void setCancelRequested(boolean cancelRequested) { this.cancelRequested = cancelRequested; }
}
