package com.healthdata.demo.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a pre-configured demo scenario.
 *
 * Demo scenarios define the complete state of the demo environment
 * for a specific use case (e.g., HEDIS evaluation, patient journey).
 *
 * Each scenario includes:
 * - Patient count and distribution
 * - Pre-configured quality measures
 * - Care gaps and interventions
 * - Demo user accounts
 */
@Entity
@Table(name = "demo_scenarios")
public class DemoScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario_type", nullable = false, length = 50)
    private ScenarioType scenarioType;

    @Column(name = "patient_count", nullable = false)
    private Integer patientCount;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_snapshot", columnDefinition = "JSONB")
    private String dataSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "JSONB")
    private String configuration;

    @Column(name = "estimated_load_time_seconds")
    private Integer estimatedLoadTimeSeconds;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
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

    // Constructors
    public DemoScenario() {}

    public DemoScenario(String name, String displayName, ScenarioType scenarioType,
                        Integer patientCount, String tenantId) {
        this.name = name;
        this.displayName = displayName;
        this.scenarioType = scenarioType;
        this.patientCount = patientCount;
        this.tenantId = tenantId;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ScenarioType getScenarioType() { return scenarioType; }
    public void setScenarioType(ScenarioType scenarioType) { this.scenarioType = scenarioType; }

    public Integer getPatientCount() { return patientCount; }
    public void setPatientCount(Integer patientCount) { this.patientCount = patientCount; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getDataSnapshot() { return dataSnapshot; }
    public void setDataSnapshot(String dataSnapshot) { this.dataSnapshot = dataSnapshot; }

    public String getConfiguration() { return configuration; }
    public void setConfiguration(String configuration) { this.configuration = configuration; }

    public Integer getEstimatedLoadTimeSeconds() { return estimatedLoadTimeSeconds; }
    public void setEstimatedLoadTimeSeconds(Integer estimatedLoadTimeSeconds) {
        this.estimatedLoadTimeSeconds = estimatedLoadTimeSeconds;
    }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * Types of demo scenarios supported.
     */
    public enum ScenarioType {
        HEDIS_EVALUATION,
        PATIENT_JOURNEY,
        RISK_STRATIFICATION,
        MULTI_TENANT,
        CUSTOM
    }
}
