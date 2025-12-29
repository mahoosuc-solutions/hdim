package com.healthdata.notification.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/**
 * User preferences for receiving notifications.
 */
@Entity
@Table(name = "notification_preferences", indexes = {
    @Index(name = "idx_preferences_tenant_user", columnList = "tenant_id, user_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_preferences_tenant_user_channel", 
                      columnNames = {"tenant_id", "user_id", "channel"})
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

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "quiet_hours_enabled")
    @Builder.Default
    private Boolean quietHoursEnabled = false;

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

    public boolean isInQuietHours(LocalTime currentTime) {
        if (!quietHoursEnabled || quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }
        if (quietHoursStart.isBefore(quietHoursEnd)) {
            return !currentTime.isBefore(quietHoursStart) && currentTime.isBefore(quietHoursEnd);
        } else {
            // Quiet hours span midnight
            return !currentTime.isBefore(quietHoursStart) || currentTime.isBefore(quietHoursEnd);
        }
    }
}
