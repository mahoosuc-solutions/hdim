package com.healthdata.payer.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Pilot customer readiness tracking for HDIM onboarding.
 *
 * Tracks integration status, data ingestion, and overall readiness score
 * for each pilot customer. Used during sales-to-deployment handoff.
 *
 * HIPAA Note: No PHI stored. Contains integration metadata only.
 */
@Entity
@Table(name = "pilot_readiness")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PilotReadiness {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "ehr_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private EhrType ehrType;

    @Column(name = "integration_status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private IntegrationStatus integrationStatus;

    @Column(name = "fhir_endpoint_url", length = 500)
    private String fhirEndpointUrl;

    @Column(name = "data_ingestion_status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private DataStatus dataIngestionStatus;

    @Column(name = "demo_data_seeded", nullable = false)
    private Boolean demoDataSeeded;

    @Column(name = "quality_measures_configured", nullable = false)
    private Boolean qualityMeasuresConfigured;

    @Column(name = "user_accounts_provisioned", nullable = false)
    private Boolean userAccountsProvisioned;

    @Column(name = "readiness_score", nullable = false)
    private Integer readinessScore;

    @Column(name = "blockers", columnDefinition = "TEXT")
    private String blockers;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (demoDataSeeded == null) demoDataSeeded = false;
        if (qualityMeasuresConfigured == null) qualityMeasuresConfigured = false;
        if (userAccountsProvisioned == null) userAccountsProvisioned = false;
        if (integrationStatus == null) integrationStatus = IntegrationStatus.NOT_STARTED;
        if (dataIngestionStatus == null) dataIngestionStatus = DataStatus.NOT_STARTED;
        if (readinessScore == null) readinessScore = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Recalculates the readiness score based on current status fields.
     * Each field contributes a weighted portion of the 0-100 score.
     */
    public int calculateReadinessScore() {
        int score = 0;
        // Integration (30 points)
        if (integrationStatus == IntegrationStatus.CONNECTED) score += 30;
        else if (integrationStatus == IntegrationStatus.TESTING) score += 20;
        else if (integrationStatus == IntegrationStatus.CONFIGURED) score += 10;

        // Data ingestion (25 points)
        if (dataIngestionStatus == DataStatus.COMPLETE) score += 25;
        else if (dataIngestionStatus == DataStatus.IN_PROGRESS) score += 15;
        else if (dataIngestionStatus == DataStatus.VALIDATED) score += 20;

        // Demo data (15 points)
        if (Boolean.TRUE.equals(demoDataSeeded)) score += 15;

        // Quality measures (15 points)
        if (Boolean.TRUE.equals(qualityMeasuresConfigured)) score += 15;

        // User accounts (15 points)
        if (Boolean.TRUE.equals(userAccountsProvisioned)) score += 15;

        this.readinessScore = score;
        return score;
    }

    public enum EhrType {
        EPIC, CERNER, ALLSCRIPTS, ATHENA, MEDITECH, OTHER
    }

    public enum IntegrationStatus {
        NOT_STARTED, CONFIGURED, TESTING, CONNECTED, FAILED
    }

    public enum DataStatus {
        NOT_STARTED, IN_PROGRESS, VALIDATED, COMPLETE, FAILED
    }
}
