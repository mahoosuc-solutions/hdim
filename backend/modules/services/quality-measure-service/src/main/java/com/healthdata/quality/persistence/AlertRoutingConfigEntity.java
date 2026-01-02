package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Alert Routing Configuration Entity
 *
 * Stores tenant-specific alert routing rules including:
 * - Alert type to role mappings
 * - Escalation time thresholds
 * - Business hours vs after-hours routing
 * - On-call schedule references
 *
 * This enables database-driven routing configuration per tenant
 * instead of hardcoded routing logic.
 */
@Entity
@Table(name = "alert_routing_configs", indexes = {
    @Index(name = "idx_routing_tenant_alert_severity", columnList = "tenant_id, alert_type, severity"),
    @Index(name = "idx_routing_tenant_active", columnList = "tenant_id, active"),
    @Index(name = "idx_routing_alert_type", columnList = "alert_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRoutingConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Tenant ID for multi-tenant isolation
     */
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    /**
     * Alert type this configuration applies to
     * (e.g., MENTAL_HEALTH_CRISIS, CRITICAL_LAB, RISK_ESCALATION)
     */
    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType;

    /**
     * Alert severity level this configuration applies to
     * (e.g., CRITICAL, HIGH, MEDIUM, LOW)
     * NULL means applies to all severities
     */
    @Column(name = "severity", length = 20)
    private String severity;

    /**
     * Recipient role for this routing rule
     * (e.g., on-call-psychiatrist, primary-care-provider, care-coordinator)
     */
    @Column(name = "recipient_role", nullable = false, length = 100)
    private String recipientRole;

    /**
     * Priority/order in which this recipient should be notified
     * Lower numbers are higher priority (1 is highest)
     */
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private int priority = 10;

    /**
     * Whether this routing rule is for business hours only
     */
    @Column(name = "business_hours_only", nullable = false)
    @Builder.Default
    private boolean businessHoursOnly = false;

    /**
     * Whether this routing rule is for after-hours only
     */
    @Column(name = "after_hours_only", nullable = false)
    @Builder.Default
    private boolean afterHoursOnly = false;

    /**
     * Time threshold in minutes before escalation
     * NULL means no time-based escalation for this rule
     */
    @Column(name = "escalation_threshold_minutes")
    private Integer escalationThresholdMinutes;

    /**
     * Reference to on-call schedule ID (if applicable)
     */
    @Column(name = "on_call_schedule_ref", length = 100)
    private String onCallScheduleRef;

    /**
     * Whether this routing rule is active
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Description of this routing rule
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Audit fields
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
