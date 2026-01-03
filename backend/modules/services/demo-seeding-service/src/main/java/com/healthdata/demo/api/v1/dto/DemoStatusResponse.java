package com.healthdata.demo.api.v1.dto;

/**
 * Response DTO for demo status endpoint.
 */
public class DemoStatusResponse {
    private boolean ready;
    private int scenarioCount;
    private int templateCount;
    private String currentSessionId;
    private String currentScenario;
    private String sessionStatus;

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public int getScenarioCount() { return scenarioCount; }
    public void setScenarioCount(int scenarioCount) { this.scenarioCount = scenarioCount; }

    public int getTemplateCount() { return templateCount; }
    public void setTemplateCount(int templateCount) { this.templateCount = templateCount; }

    public String getCurrentSessionId() { return currentSessionId; }
    public void setCurrentSessionId(String currentSessionId) { this.currentSessionId = currentSessionId; }

    public String getCurrentScenario() { return currentScenario; }
    public void setCurrentScenario(String currentScenario) { this.currentScenario = currentScenario; }

    public String getSessionStatus() { return sessionStatus; }
    public void setSessionStatus(String sessionStatus) { this.sessionStatus = sessionStatus; }
}
