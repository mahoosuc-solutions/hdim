package com.healthdata.auditquery.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Daily rollup projection of user audit activity.
 *
 * <p>Aggregates audit events by tenant, user, and day for fast dashboard queries.
 * Prevents scanning millions of audit events for compliance reports.
 *
 * <p>Updated via Kafka consumer processing audit-events topic.
 *
 * <p>Metrics tracked:
 * <ul>
 *   <li>Total events per user per day</li>
 *   <li>PHI access counts (Patient, Observation, etc.)</li>
 *   <li>Failed event counts (security monitoring)</li>
 *   <li>Unique resources accessed</li>
 * </ul>
 */
@Entity
@Table(name = "audit_user_activity_daily", indexes = {
    @Index(name = "idx_user_activity_tenant_date", columnList = "tenant_id,activity_date"),
    @Index(name = "idx_user_activity_user_date", columnList = "tenant_id,user_id,activity_date")
})
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AuditUserActivityDailyEntity implements Serializable {

    @EmbeddedId
    private UserActivityKey id;

    /**
     * Total number of audit events for this user on this day.
     */
    @Column(name = "total_events", nullable = false)
    private Long totalEvents;

    /**
     * Number of PHI access events (Patient, Observation, etc.).
     * Used for HIPAA compliance reporting.
     */
    @Column(name = "phi_access_count", nullable = false)
    private Long phiAccessCount;

    /**
     * Number of failed events (outcome = FAILURE).
     * Used for security monitoring and anomaly detection.
     */
    @Column(name = "failed_events", nullable = false)
    private Long failedEvents;

    /**
     * Number of unique resources accessed by this user on this day.
     * Helps identify unusual access patterns.
     */
    @Column(name = "unique_resources", nullable = false)
    private Integer uniqueResources;

    /**
     * Composite primary key for user activity.
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivityKey implements Serializable {

        @Column(name = "tenant_id", nullable = false, length = 64)
        private String tenantId;

        @Column(name = "user_id", nullable = false, length = 64)
        private String userId;

        @Column(name = "activity_date", nullable = false)
        private LocalDate activityDate;
    }

    /**
     * Increment counters for a new audit event.
     *
     * @param isPhiAccess whether this event accessed PHI
     * @param isFailed whether this event failed
     */
    public void incrementCounters(boolean isPhiAccess, boolean isFailed) {
        this.totalEvents = (this.totalEvents != null ? this.totalEvents : 0L) + 1;

        if (isPhiAccess) {
            this.phiAccessCount = (this.phiAccessCount != null ? this.phiAccessCount : 0L) + 1;
        }

        if (isFailed) {
            this.failedEvents = (this.failedEvents != null ? this.failedEvents : 0L) + 1;
        }
    }
}
