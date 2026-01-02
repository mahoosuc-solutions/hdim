package com.healthdata.quality.model;

import com.healthdata.quality.persistence.CareTeamMemberEntity;
import com.healthdata.quality.persistence.NotificationEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Notification Recipient
 *
 * Represents a resolved recipient for a notification, including their
 * contact information and preferred channels.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRecipient {

    private String userId;
    private String emailAddress;
    private String phoneNumber;
    private Set<NotificationEntity.NotificationChannel> enabledChannels;
    private CareTeamMemberEntity.CareTeamRole careTeamRole;
    private boolean isPrimary;
    private NotificationEntity.NotificationSeverity severityThreshold;

    /**
     * Check if recipient should receive notification via specified channel
     */
    public boolean supportsChannel(NotificationEntity.NotificationChannel channel) {
        if (enabledChannels == null || enabledChannels.isEmpty()) {
            return false;
        }
        return enabledChannels.contains(channel);
    }

    /**
     * Check if recipient's severity threshold allows this notification
     */
    public boolean meetsThreshold(NotificationEntity.NotificationSeverity severity) {
        if (severityThreshold == null || severity == null) {
            return true;
        }

        int severityLevel = getSeverityLevel(severity);
        int thresholdLevel = getSeverityLevel(severityThreshold);

        return severityLevel >= thresholdLevel;
    }

    /**
     * Get contact information for a specific channel
     */
    public String getContactForChannel(NotificationEntity.NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> emailAddress;
            case SMS -> phoneNumber;
            case PUSH, IN_APP, WEBSOCKET -> userId;
        };
    }

    private int getSeverityLevel(NotificationEntity.NotificationSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
            case INFO -> 0;
        };
    }
}
