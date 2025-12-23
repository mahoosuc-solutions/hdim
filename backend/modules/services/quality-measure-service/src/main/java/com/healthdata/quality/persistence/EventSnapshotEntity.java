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
 * Event Snapshot Entity
 *
 * Snapshot of aggregate state to speed event replay.
 */
@Entity
@Table(name = "event_snapshots", indexes = {
    @Index(name = "idx_es_latest", columnList = "tenant_id, aggregate_type, aggregate_id, event_number"),
    @Index(name = "idx_es_tenant", columnList = "tenant_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSnapshotEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_number", nullable = false)
    private Long eventNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> snapshotData;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
