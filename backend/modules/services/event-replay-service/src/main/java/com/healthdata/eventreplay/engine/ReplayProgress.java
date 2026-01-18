package com.healthdata.eventreplay.engine;

import java.time.Instant;

/**
 * ReplayProgress - Tracks progress of event replay
 */
public class ReplayProgress {
    private final String aggregateId;
    private final String tenantId;
    private long totalEvents;
    private long eventsProcessed;
    private boolean complete;
    private Instant startTime;
    private Instant endTime;

    public ReplayProgress(String aggregateId, String tenantId) {
        this.aggregateId = aggregateId;
        this.tenantId = tenantId;
        this.startTime = Instant.now();
        this.complete = false;
    }

    public void setTotalEvents(long totalEvents) {
        this.totalEvents = totalEvents;
    }

    public void setEventsProcessed(long eventsProcessed) {
        this.eventsProcessed = eventsProcessed;
    }

    public void markComplete() {
        this.complete = true;
        this.endTime = Instant.now();
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public long getEventsProcessed() {
        return eventsProcessed;
    }

    public boolean isComplete() {
        return complete;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public long getDurationMs() {
        if (startTime != null && endTime != null) {
            return endTime.toEpochMilli() - startTime.toEpochMilli();
        }
        return 0;
    }
}
