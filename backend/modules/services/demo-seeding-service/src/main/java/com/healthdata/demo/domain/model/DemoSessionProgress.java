package com.healthdata.demo.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks progress for an active demo session.
 */
@Entity
@Table(name = "demo_session_progress")
public class DemoSessionProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "scenario_name", length = 100)
    private String scenarioName;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Column(name = "stage", nullable = false, length = 50)
    private String stage;

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    @Column(name = "patients_generated")
    private Integer patientsGenerated;

    @Column(name = "patients_persisted")
    private Integer patientsPersisted;

    @Column(name = "care_gaps_created")
    private Integer careGapsCreated;

    @Column(name = "measures_seeded")
    private Integer measuresSeeded;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "cancel_requested", nullable = false)
    private boolean cancelRequested = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

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

    public boolean isCancelRequested() { return cancelRequested; }
    public void setCancelRequested(boolean cancelRequested) { this.cancelRequested = cancelRequested; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
