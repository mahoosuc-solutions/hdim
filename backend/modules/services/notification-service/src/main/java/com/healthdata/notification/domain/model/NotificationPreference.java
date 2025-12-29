package com.healthdata.notification.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/**
 * User notification preferences including channel enablement and quiet hours.
 */
@Entity
@Table(name = "notification_preferences", indexes = {
    @Index(name = "idx_preferences_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_preferences_user_id", columnList = "user_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_preference_tenant_user_channel", columnNames = {"tenant_id", "user_id", "channel"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Check if notifications should be suppressed based on quiet hours.
     */
    public boolean isInQuietHours(LocalTime currentTime) {
        if (quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }

        // Handle overnight quiet hours (e.g., 22:00 to 07:00)
        if (quietHoursStart.isAfter(quietHoursEnd)) {
            return currentTime.isAfter(quietHoursStart) || currentTime.isBefore(quietHoursEnd);
        }

        return currentTime.isAfter(quietHoursStart) && currentTime.isBefore(quietHoursEnd);
    }
}
