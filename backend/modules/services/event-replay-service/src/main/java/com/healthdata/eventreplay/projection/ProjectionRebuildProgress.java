package com.healthdata.eventreplay.projection;

import java.time.Instant;

/**
 * ProjectionRebuildProgress - Tracks progress of bulk projection rebuild
 */
public class ProjectionRebuildProgress {
    private final String tenantId;
    private final String projectionType;
    private long totalProjections;
    private long projectionsRebuilt;
    private boolean complete;
    private Instant startTime;
    private Instant endTime;

    public ProjectionRebuildProgress(String tenantId, String projectionType) {
        this.tenantId = tenantId;
        this.projectionType = projectionType;
        this.startTime = Instant.now();
        this.complete = false;
    }

    public void setTotalProjections(long totalProjections) {
        this.totalProjections = totalProjections;
    }

    public void setProjectionsRebuilt(long projectionsRebuilt) {
        this.projectionsRebuilt = projectionsRebuilt;
    }

    public void markComplete() {
        this.complete = true;
        this.endTime = Instant.now();
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getProjectionType() {
        return projectionType;
    }

    public long getTotalProjections() {
        return totalProjections;
    }

    public long getProjectionsRebuilt() {
        return projectionsRebuilt;
    }

    public boolean isComplete() {
        return complete;
    }

    public long getDurationMs() {
        if (startTime != null && endTime != null) {
            return endTime.toEpochMilli() - startTime.toEpochMilli();
        }
        return 0;
    }
}
