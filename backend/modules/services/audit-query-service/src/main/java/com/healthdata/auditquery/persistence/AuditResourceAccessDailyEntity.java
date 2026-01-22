package com.healthdata.auditquery.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Daily rollup projection of resource access patterns.
 *
 * <p>Aggregates audit events by tenant, resource, and day for:
 * <ul>
 *   <li>Identifying most accessed resources</li>
 *   <li>Detecting unusual access patterns</li>
 *   <li>Compliance reporting (who accessed which patient records)</li>
 * </ul>
 *
 * <p>Updated via Kafka consumer processing audit-events topic.
 */
@Entity
@Table(name = "audit_resource_access_daily", indexes = {
    @Index(name = "idx_resource_access_tenant_date", columnList = "tenant_id,access_date"),
    @Index(name = "idx_resource_access_resource", columnList = "tenant_id,resource_type,resource_id,access_date")
})
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AuditResourceAccessDailyEntity implements Serializable {

    @EmbeddedId
    private ResourceAccessKey id;

    /**
     * Total number of access events for this resource on this day.
     */
    @Column(name = "access_count", nullable = false)
    private Long accessCount;

    /**
     * Number of unique users who accessed this resource on this day.
     * Helps identify shared record access patterns.
     */
    @Column(name = "unique_users", nullable = false)
    private Integer uniqueUsers;

    /**
     * Composite primary key for resource access.
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceAccessKey implements Serializable {

        @Column(name = "tenant_id", nullable = false, length = 64)
        private String tenantId;

        @Column(name = "resource_type", nullable = false, length = 64)
        private String resourceType;

        @Column(name = "resource_id", nullable = false, length = 255)
        private String resourceId;

        @Column(name = "access_date", nullable = false)
        private LocalDate accessDate;
    }

    /**
     * Increment access count.
     */
    public void incrementAccessCount() {
        this.accessCount = (this.accessCount != null ? this.accessCount : 0L) + 1;
    }
}
