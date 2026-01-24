package com.healthdata.admin.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Alert Configuration Entity
 *
 * Stores alerting rules for service monitoring.
 * Alerts trigger when metrics exceed configured thresholds for specified duration.
 *
 * Multi-tenant: Each alert belongs to a specific tenant
 * Auditing: Tracks creation and updates
 */
@Entity
@Table(
    name = "alert_configs",
    indexes = {
        @Index(name = "idx_alert_configs_tenant", columnList = "tenant_id"),
        @Index(name = "idx_alert_configs_service", columnList = "service_name"),
        @Index(name = "idx_alert_configs_enabled", columnList = "enabled")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    @Column(name = "service_name", nullable = false, length = 255)
    private String serviceName;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private AlertType alertType;

    @Column(name = "threshold", nullable = false)
    private Double threshold;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @ElementCollection
    @CollectionTable(
        name = "alert_notification_channels",
        joinColumns = @JoinColumn(name = "alert_config_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "channel")
    private List<NotificationChannel> notificationChannels;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_triggered")
    private Instant lastTriggered;

    /**
     * Alert Type Enum
     * Defines the type of metric being monitored
     */
    public enum AlertType {
        CPU_USAGE,
        MEMORY_USAGE,
        ERROR_RATE,
        LATENCY,
        REQUEST_RATE
    }

    /**
     * Alert Severity Enum
     * Defines the severity level of the alert
     */
    public enum AlertSeverity {
        INFO,
        WARNING,
        CRITICAL
    }

    /**
     * Notification Channel Enum
     * Defines where notifications should be sent
     */
    public enum NotificationChannel {
        EMAIL,
        SLACK,
        WEBHOOK,
        SMS
    }

    /**
     * Update last triggered timestamp
     */
    public void markTriggered() {
        this.lastTriggered = Instant.now();
    }

    /**
     * Check if alert should evaluate
     * Only evaluate enabled alerts
     */
    public boolean shouldEvaluate() {
        return Boolean.TRUE.equals(this.enabled);
    }
}
