package com.healthdata.analytics.persistence;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Alert Rule Entity
 *
 * Defines threshold-based alert rules for monitoring KPIs and metrics.
 * When thresholds are crossed, notifications are sent via configured channels.
 */
@Entity
@Table(name = "alert_rules",
       indexes = {
           @Index(name = "idx_alert_tenant", columnList = "tenant_id"),
           @Index(name = "idx_alert_metric_type", columnList = "metric_type"),
           @Index(name = "idx_alert_active", columnList = "is_active")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType; // QUALITY_SCORE, RAF_SCORE, CARE_GAP_RATE, STAR_RATING

    @Column(name = "metric_name", length = 100)
    private String metricName;

    @Column(name = "condition_operator", nullable = false, length = 20)
    private String conditionOperator; // GT, LT, EQ, GTE, LTE, CHANGE_PCT

    @Column(name = "threshold_value", nullable = false, precision = 15, scale = 4)
    private BigDecimal thresholdValue;

    @Column(name = "secondary_threshold", precision = 15, scale = 4)
    private BigDecimal secondaryThreshold; // for range-based alerts

    @Column(name = "severity", nullable = false, length = 20)
    @Builder.Default
    private String severity = "MEDIUM"; // LOW, MEDIUM, HIGH, CRITICAL

    @Type(JsonBinaryType.class)
    @Column(name = "notification_channels", columnDefinition = "jsonb")
    private Map<String, Object> notificationChannels; // email, slack, webhook configs

    @Type(JsonBinaryType.class)
    @Column(name = "filters", columnDefinition = "jsonb")
    private Map<String, Object> filters; // additional filters like measureId, etc.

    @Column(name = "cooldown_minutes")
    @Builder.Default
    private Integer cooldownMinutes = 60; // prevent alert spam

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "trigger_count")
    @Builder.Default
    private Integer triggerCount = 0;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Integer version;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean canTrigger() {
        if (!isActive) {
            return false;
        }
        if (lastTriggeredAt == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(lastTriggeredAt.plusMinutes(cooldownMinutes));
    }
}
