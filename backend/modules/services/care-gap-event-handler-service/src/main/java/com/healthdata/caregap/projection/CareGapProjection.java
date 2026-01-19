package com.healthdata.caregap.projection;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * CareGapProjection - Denormalized read model for care gap tracking
 *
 * Built from care gap events via event sourcing.
 * Optimized for gap queries (status, severity, patient qualification).
 */
@Entity
@Table(name = "care_gap_projections", indexes = {
    @Index(name = "idx_caregap_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_caregap_patient_tenant", columnList = "patient_id, tenant_id")
})
public class CareGapProjection {
    @Id
    @Column(name = "id")
    private String id; // Composite key

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "gap_code", nullable = false)
    private String gapCode;

    @Column(name = "gap_description")
    private String gapDescription;

    @Column(name = "severity")
    private String severity;  // CRITICAL, HIGH, MEDIUM, LOW

    @Column(name = "status")
    private String status;  // OPEN, CLOSED, WAIVED

    @Column(name = "qualified")
    private boolean qualified;

    @Column(name = "recommended_intervention")
    private String recommendedIntervention;

    @Column(name = "detection_date")
    private LocalDate detectionDate;

    @Column(name = "closure_date")
    private LocalDate closureDate;

    @Column(name = "version")
    private long version;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    public CareGapProjection(String patientId, String tenantId, String gapCode, String gapDescription, String severity) {
        this.patientId = patientId;
        this.tenantId = tenantId;
        this.gapCode = gapCode;
        this.gapDescription = gapDescription;
        this.severity = severity;
        this.status = "OPEN";
        this.qualified = false;
        this.version = 1L;
        this.lastUpdated = Instant.now();
        this.detectionDate = LocalDate.now();
    }

    // Getters
    public String getPatientId() { return patientId; }
    public String getTenantId() { return tenantId; }
    public String getGapCode() { return gapCode; }
    public String getGapDescription() { return gapDescription; }
    public String getSeverity() { return severity; }
    public String getStatus() { return status; }
    public boolean isQualified() { return qualified; }
    public String getRecommendedIntervention() { return recommendedIntervention; }
    public LocalDate getDetectionDate() { return detectionDate; }
    public LocalDate getClosureDate() { return closureDate; }
    public long getVersion() { return version; }
    public Instant getLastUpdated() { return lastUpdated; }

    // Calculated property
    public int getDaysOpen() {
        LocalDate endDate = closureDate != null ? closureDate : LocalDate.now();
        return (int) java.time.temporal.ChronoUnit.DAYS.between(detectionDate, endDate);
    }

    // Setters
    public void setGapDescription(String gapDescription) { this.gapDescription = gapDescription; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setStatus(String status) { this.status = status; }
    public void setQualified(boolean qualified) { this.qualified = qualified; }
    public void setRecommendedIntervention(String recommendedIntervention) { this.recommendedIntervention = recommendedIntervention; }
    public void setDetectionDate(LocalDate detectionDate) { this.detectionDate = detectionDate; }
    public void setClosureDate(LocalDate closureDate) { this.closureDate = closureDate; }
    public void setVersion(long version) { this.version = version; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }

    public void incrementVersion() {
        this.version++;
        this.lastUpdated = Instant.now();
    }
}
