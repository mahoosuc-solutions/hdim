package com.healthdata.investor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for dashboard statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {

    // Task statistics
    private long totalTasks;
    private long completedTasks;
    private long inProgressTasks;
    private long pendingTasks;
    private long blockedTasks;
    private double taskCompletionRate;
    private Map<String, Long> tasksByCategory;
    private Map<String, Long> tasksByWeek;

    // Contact statistics
    private long totalContacts;
    private Map<String, Long> contactsByCategory;
    private Map<String, Long> contactsByStatus;
    private Map<String, Long> contactsByTier;
    private long contactsNeedingFollowUp;

    // Activity statistics
    private long totalActivities;
    private Map<String, Long> activitiesByType;
    private Map<String, Long> activitiesByStatus;
    private long linkedInActivities;
    private long pendingActivities;

    // LinkedIn statistics
    private boolean linkedInConnected;
    private long linkedInConnectionsSent;
    private long linkedInMessageseSent;
    private long linkedInResponses;
}
