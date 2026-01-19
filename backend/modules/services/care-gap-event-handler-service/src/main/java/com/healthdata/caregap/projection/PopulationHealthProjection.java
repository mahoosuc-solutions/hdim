package com.healthdata.caregap.projection;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * PopulationHealthProjection - Aggregated care gap metrics for population health
 *
 * Tracks gap statistics across patient cohort for reporting and analytics.
 */
@Entity
@Table(name = "population_health_projections")
public class PopulationHealthProjection {
    @Id
    @Column(name = "id")
    private String id; // tenantId

    @Column(name = "tenant_id", nullable = false, unique = true)
    private String tenantId;

    @Column(name = "total_gaps_open")
    private int totalGapsOpen;

    @Column(name = "critical_gaps")
    private int criticalGaps;

    @Column(name = "high_gaps")
    private int highGaps;

    @Column(name = "medium_gaps")
    private int mediumGaps;

    @Column(name = "low_gaps")
    private int lowGaps;

    @Column(name = "gaps_closed")
    private int gapsClosed;

    @Column(name = "closure_rate")
    private float closureRate;  // closed / (open + closed)

    @Column(name = "version")
    private long version;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    public PopulationHealthProjection(String tenantId) {
        this.tenantId = tenantId;
        this.totalGapsOpen = 0;
        this.criticalGaps = 0;
        this.highGaps = 0;
        this.mediumGaps = 0;
        this.lowGaps = 0;
        this.gapsClosed = 0;
        this.closureRate = 0.0f;
        this.version = 1L;
        this.lastUpdated = Instant.now();
    }

    // Getters
    public String getTenantId() { return tenantId; }
    public int getTotalGapsOpen() { return totalGapsOpen; }
    public int getCriticalGaps() { return criticalGaps; }
    public int getHighGaps() { return highGaps; }
    public int getMediumGaps() { return mediumGaps; }
    public int getLowGaps() { return lowGaps; }
    public int getGapsClosed() { return gapsClosed; }
    public float getClosureRate() { return closureRate; }
    public long getVersion() { return version; }
    public Instant getLastUpdated() { return lastUpdated; }

    // Setters
    public void setTotalGapsOpen(int count) { this.totalGapsOpen = count; }
    public void setCriticalGaps(int count) { this.criticalGaps = count; }
    public void setHighGaps(int count) { this.highGaps = count; }
    public void setMediumGaps(int count) { this.mediumGaps = count; }
    public void setLowGaps(int count) { this.lowGaps = count; }
    public void setGapsClosed(int count) { this.gapsClosed = count; }
    public void setVersion(long version) { this.version = version; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }

    public void calculateClosureRate() {
        int total = totalGapsOpen + gapsClosed;
        if (total > 0) {
            this.closureRate = (float) gapsClosed / total;
        } else {
            this.closureRate = 0.0f;
        }
        this.version++;
        this.lastUpdated = Instant.now();
    }

    public void incrementVersion() {
        this.version++;
        this.lastUpdated = Instant.now();
    }
}
