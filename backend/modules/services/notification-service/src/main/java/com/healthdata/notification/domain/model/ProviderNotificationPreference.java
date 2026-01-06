package com.healthdata.notification.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Provider-specific notification preferences by notification type.
 * Issue #148: Smart Notification Preferences
 *
 * This entity allows providers to configure which types of notifications
 * they receive and through which delivery method.
 */
@Entity
@Table(name = "provider_notification_preferences", indexes = {
    @Index(name = "idx_prov_pref_tenant_provider", columnList = "tenant_id, provider_id"),
    @Index(name = "idx_prov_pref_type", columnList = "notification_type")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_prov_pref_provider_type",
                      columnNames = {"provider_id", "notification_type"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false, length = 50)
    @Builder.Default
    private NotificationChannel deliveryMethod = NotificationChannel.IN_APP;

    /**
     * Additional delivery methods (comma-separated).
     * Allows sending to multiple channels (e.g., "EMAIL,SMS")
     */
    @Column(name = "additional_delivery_methods", length = 200)
    private String additionalDeliveryMethods;

    /**
     * Minimum urgency level to trigger notification.
     * Values: LOW, MEDIUM, HIGH, CRITICAL
     */
    @Column(name = "min_urgency", length = 20)
    @Builder.Default
    private String minUrgency = "LOW";

    /**
     * Whether to send digest summaries instead of individual notifications.
     */
    @Column(name = "digest_enabled")
    @Builder.Default
    private Boolean digestEnabled = false;

    /**
     * Digest frequency: DAILY, WEEKLY, IMMEDIATE
     */
    @Column(name = "digest_frequency", length = 20)
    @Builder.Default
    private String digestFrequency = "DAILY";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();

        // Enforce always-enabled types
        if (notificationType != null && notificationType.isAlwaysEnabled()) {
            enabled = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();

        // Enforce always-enabled types
        if (notificationType != null && notificationType.isAlwaysEnabled()) {
            enabled = true;
        }
    }

    /**
     * Check if a notification should be sent based on urgency level.
     */
    public boolean shouldNotify(String urgencyLevel) {
        if (!enabled) {
            return false;
        }

        int minLevel = urgencyToLevel(minUrgency);
        int actualLevel = urgencyToLevel(urgencyLevel);
        return actualLevel >= minLevel;
    }

    private int urgencyToLevel(String urgency) {
        if (urgency == null) return 0;
        return switch (urgency.toUpperCase()) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }
}
