package com.healthdata.eventrouter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricsSnapshot {
    private long totalRoutedEvents;
    private long totalFilteredEvents;
    private long totalUnroutedEvents;
    private long totalDlqEvents;
    private double eventsPerSecond;
    private double errorRate;
    private Map<String, Long> eventsByTopic;
    private Instant snapshotTime;
}
