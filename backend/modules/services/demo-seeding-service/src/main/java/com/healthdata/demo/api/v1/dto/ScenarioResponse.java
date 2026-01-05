package com.healthdata.demo.api.v1.dto;

/**
 * Response DTO for scenario information.
 */
public class ScenarioResponse {
    private String id;
    private String name;
    private String displayName;
    private String description;
    private String scenarioType;
    private int patientCount;
    private String tenantId;
    private Integer estimatedLoadTimeSeconds;
    private boolean active;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getScenarioType() { return scenarioType; }
    public void setScenarioType(String scenarioType) { this.scenarioType = scenarioType; }

    public int getPatientCount() { return patientCount; }
    public void setPatientCount(int patientCount) { this.patientCount = patientCount; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public Integer getEstimatedLoadTimeSeconds() { return estimatedLoadTimeSeconds; }
    public void setEstimatedLoadTimeSeconds(Integer estimatedLoadTimeSeconds) {
        this.estimatedLoadTimeSeconds = estimatedLoadTimeSeconds;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
