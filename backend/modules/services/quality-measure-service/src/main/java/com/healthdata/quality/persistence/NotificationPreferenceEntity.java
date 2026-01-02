package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

/**
 * Notification Preference Entity
 *
 * Stores user preferences for how and when they want to receive notifications.
 * HIPAA compliant - users control their notification settings.
 */
@Entity
@Table(name = "notification_preferences", indexes = {
    @Index(name = "idx_notification_pref_user", columnList = "user_id"),
    @Index(name = "idx_notification_pref_tenant", columnList = "tenant_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_notification_pref_user_tenant",
        columnNames = {"user_id", "tenant_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    // Channel Preferences
    @Column(name = "email_enabled")
    @Builder.Default
    private Boolean emailEnabled = true;

    @Column(name = "sms_enabled")
    @Builder.Default
    private Boolean smsEnabled = false;

    @Column(name = "push_enabled")
    @Builder.Default
    private Boolean pushEnabled = true;

    @Column(name = "in_app_enabled")
    @Builder.Default
    private Boolean inAppEnabled = true;

    // Contact Information
    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "push_token")
    private String pushToken;

    // Notification Type Preferences (JSONB for flexibility)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enabled_types", columnDefinition = "jsonb")
    private Set<String> enabledTypes; // Which notification types to receive

    @Column(name = "severity_threshold")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NotificationEntity.NotificationSeverity severityThreshold =
        NotificationEntity.NotificationSeverity.MEDIUM;

    // Quiet Hours
    @Column(name = "quiet_hours_enabled")
    @Builder.Default
    private Boolean quietHoursEnabled = false;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart; // e.g., 22:00

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd; // e.g., 08:00

    @Column(name = "quiet_hours_override_critical")
    @Builder.Default
    private Boolean quietHoursOverrideCritical = true; // Always send critical alerts

    // Frequency Controls
    @Column(name = "digest_mode_enabled")
    @Builder.Default
    private Boolean digestModeEnabled = false; // Batch non-critical notifications

    @Column(name = "digest_frequency")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DigestFrequency digestFrequency = DigestFrequency.DAILY;

    // Custom Settings (for future extensibility)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_settings", columnDefinition = "jsonb")
    private Map<String, Object> customSettings;

    // HIPAA Compliance
    @Column(name = "consent_given")
    @Builder.Default
    private Boolean consentGiven = false;

    @Column(name = "consent_date")
    private java.time.LocalDateTime consentDate;

    public enum DigestFrequency {
        HOURLY,
        DAILY,
        WEEKLY
    }

    /**
     * Check if notifications should be sent during current time
     */
    public boolean isWithinQuietHours() {
        if (!quietHoursEnabled || quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }

        LocalTime now = LocalTime.now();

        // Handle quiet hours that span midnight
        if (quietHoursStart.isAfter(quietHoursEnd)) {
            return now.isAfter(quietHoursStart) || now.isBefore(quietHoursEnd);
        }

        return now.isAfter(quietHoursStart) && now.isBefore(quietHoursEnd);
    }

    /**
     * Check if user should receive notification based on preferences
     */
    public boolean shouldReceive(NotificationEntity.NotificationChannel channel,
                                 NotificationEntity.NotificationType type,
                                 NotificationEntity.NotificationSeverity severity) {
        // Check consent
        if (!consentGiven) {
            return false;
        }

        // Check channel enabled
        boolean channelEnabled = switch (channel) {
            case EMAIL -> emailEnabled != null && emailEnabled;
            case SMS -> smsEnabled != null && smsEnabled;
            case PUSH -> pushEnabled != null && pushEnabled;
            case IN_APP -> inAppEnabled != null && inAppEnabled;
            case WEBSOCKET -> true; // Always enabled for real-time updates
        };

        if (!channelEnabled) {
            return false;
        }

        // Check severity threshold
        if (severity != null && severityThreshold != null) {
            int severityLevel = getSeverityLevel(severity);
            int thresholdLevel = getSeverityLevel(severityThreshold);
            if (severityLevel < thresholdLevel) {
                return false;
            }
        }

        // Check quiet hours (with critical override)
        if (isWithinQuietHours()) {
            if (severity == NotificationEntity.NotificationSeverity.CRITICAL &&
                quietHoursOverrideCritical) {
                return true; // Critical alerts override quiet hours
            }
            return false; // Suppress during quiet hours
        }

        // Check notification type filter
        if (enabledTypes != null && !enabledTypes.isEmpty()) {
            return enabledTypes.contains(type.name());
        }

        return true;
    }

    /**
     * Convert severity enum to numeric level for comparison
     */
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
