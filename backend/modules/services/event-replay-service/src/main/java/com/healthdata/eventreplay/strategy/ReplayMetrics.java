package com.healthdata.eventreplay.strategy;

/**
 * ReplayMetrics - Tracks replay execution metrics
 */
public class ReplayMetrics {
    private int eventsReplayed;
    private long durationMs;
    private String strategy;

    public void setEventsReplayed(int eventsReplayed) {
        this.eventsReplayed = eventsReplayed;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public int getEventsReplayed() {
        return eventsReplayed;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getStrategy() {
        return strategy;
    }
}
