package com.healthdata.caregap.projection;

import java.time.Instant;

/**
 * PopulationHealthProjection - Aggregated care gap metrics for population health
 *
 * Tracks gap statistics across patient cohort for reporting and analytics.
 */
public class PopulationHealthProjection {
    private final String tenantId;
    private int totalGapsOpen;
    private int criticalGaps;
    private int highGaps;
    private int mediumGaps;
    private int lowGaps;
    private int gapsClosed;
    private float closureRate;  // closed / (open + closed)
    private long version;
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
}
