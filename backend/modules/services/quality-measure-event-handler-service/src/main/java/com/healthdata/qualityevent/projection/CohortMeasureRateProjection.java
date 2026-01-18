package com.healthdata.qualityevent.projection;

import java.time.Instant;

public class CohortMeasureRateProjection {
    private final String tenantId;
    private final String measureCode;
    private int denominatorCount;
    private int numeratorCount;
    private float complianceRate;
    private long version;
    private Instant lastUpdated;

    public CohortMeasureRateProjection(String tenantId, String measureCode) {
        this.tenantId = tenantId;
        this.measureCode = measureCode;
        this.denominatorCount = 0;
        this.numeratorCount = 0;
        this.complianceRate = 0.0f;
        this.version = 1L;
        this.lastUpdated = Instant.now();
    }

    // Getters
    public String getTenantId() { return tenantId; }
    public String getMeasureCode() { return measureCode; }
    public int getDenominatorCount() { return denominatorCount; }
    public int getNumeratorCount() { return numeratorCount; }
    public float getComplianceRate() { return complianceRate; }
    public long getVersion() { return version; }
    public Instant getLastUpdated() { return lastUpdated; }

    // Setters
    public void setDenominatorCount(int count) { this.denominatorCount = count; }
    public void setNumeratorCount(int count) { this.numeratorCount = count; }
    public void setComplianceRate(float rate) { this.complianceRate = rate; }

    public void calculateComplianceRate() {
        if (denominatorCount > 0) {
            this.complianceRate = (float) numeratorCount / denominatorCount;
        } else {
            this.complianceRate = 0.0f;
        }
        this.version++;
        this.lastUpdated = Instant.now();
    }
}
