package com.healthdata.caregap.projection;

import java.time.Instant;
import java.time.LocalDate;

/**
 * CareGapProjection - Denormalized read model for care gap tracking
 *
 * Built from care gap events via event sourcing.
 * Optimized for gap queries (status, severity, patient qualification).
 */
public class CareGapProjection {
    private final String patientId;
    private final String tenantId;
    private final String gapCode;
    private String gapDescription;
    private String severity;  // CRITICAL, HIGH, MEDIUM, LOW
    private String status;  // OPEN, CLOSED, WAIVED
    private boolean qualified;
    private String recommendedIntervention;
    private LocalDate detectionDate;
    private LocalDate closureDate;
    private long version;
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
