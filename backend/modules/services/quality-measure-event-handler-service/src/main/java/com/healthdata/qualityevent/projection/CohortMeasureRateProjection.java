package com.healthdata.qualityevent.projection;

import jakarta.persistence.*;
import java.time.Instant;

@Entity(name = "CohortMeasureRateHandlerProjection")
@Table(name = "cohort_measure_rates")
public class CohortMeasureRateProjection {
    @Id
    @Column(name = "id")
    private String id; // Composite key: tenantId + "_" + measureCode

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "measure_code", nullable = false)
    private String measureCode;

    @Column(name = "denominator_count")
    private int denominatorCount;

    @Column(name = "numerator_count")
    private int numeratorCount;

    @Column(name = "compliance_rate")
    private float complianceRate;

    @Column(name = "version")
    private long version;

    @Column(name = "last_updated")
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
    public void setVersion(long version) { this.version = version; }

    public void calculateComplianceRate() {
        if (denominatorCount > 0) {
            this.complianceRate = (float) numeratorCount / denominatorCount;
        } else {
            this.complianceRate = 0.0f;
        }
        this.version++;
        this.lastUpdated = Instant.now();
    }

    public void incrementVersion() {
        this.version++;
        this.lastUpdated = Instant.now();
    }
}
