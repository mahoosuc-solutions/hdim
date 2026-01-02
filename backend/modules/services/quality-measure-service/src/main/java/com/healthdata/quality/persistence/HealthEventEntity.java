package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Health Event Entity
 *
 * Event-sourcing record for domain changes across quality workflows.
 */
@Entity
@Table(name = "health_events", indexes = {
    @Index(name = "idx_he_event_number", columnList = "event_number"),
    @Index(name = "idx_he_aggregate", columnList = "tenant_id, aggregate_type, aggregate_id, event_number"),
    @Index(name = "idx_he_event_type", columnList = "tenant_id, event_type, occurred_at"),
    @Index(name = "idx_he_time_travel", columnList = "tenant_id, aggregate_type, aggregate_id, occurred_at"),
    @Index(name = "idx_he_tenant", columnList = "tenant_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthEventEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_number", nullable = false)
    private Long eventNumber;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    @Builder.Default
    private Integer eventVersion = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> eventData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (occurredAt == null) {
            occurredAt = now;
        }
        if (recordedAt == null) {
            recordedAt = now;
        }
    }
}
