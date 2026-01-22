package com.healthdata.eventstore.domain;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

/**
 * EventSnapshot - JPA Entity for event_snapshots table
 *
 * Purpose: Performance optimization for event replay
 *
 * Problem: Replaying 10,000 events for a single aggregate is slow
 * Solution: Store periodic snapshots of aggregate state
 * Strategy: Snapshot every N events (e.g., every 100 events)
 * Benefit: Replay from latest snapshot + recent events (much faster)
 *
 * Example Flow:
 * 1. Patient aggregate has 5,000 events
 * 2. Latest snapshot at event version 4,900
 * 3. To reconstruct state: Load snapshot + replay events 4,901-5,000
 * 4. Result: Only 100 events replayed instead of 5,000
 *
 * Usage:
 * <pre>
 * EventSnapshot snapshot = EventSnapshot.builder()
 *     .aggregateId(patientId)
 *     .aggregateType("Patient")
 *     .snapshotVersion(100)
 *     .snapshotData(aggregateStateJson)
 *     .tenantId(tenantId)
 *     .build();
 * </pre>
 */
@Entity
@Table(name = "event_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_snapshots_aggregate_version",
                columnNames = {"aggregate_id", "aggregate_type", "snapshot_version"}
        ),
        indexes = {
                @Index(name = "idx_event_snapshots_aggregate", columnList = "aggregate_id, aggregate_type"),
                @Index(name = "idx_event_snapshots_tenant", columnList = "tenant_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class EventSnapshot {

    /**
     * Primary Key: Auto-incrementing ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * Aggregate ID: The entity this snapshot represents
     */
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    /**
     * Aggregate Type: The type of entity (e.g., "Patient", "CareGap")
     */
    @Column(name = "aggregate_type", nullable = false, length = 255)
    private String aggregateType;

    /**
     * Snapshot Version: Event version number when this snapshot was taken
     * Corresponds to event_store.event_version
     *
     * Example: If snapshot_version = 100, this snapshot reflects
     * the aggregate state after processing events 1-100
     */
    @Column(name = "snapshot_version", nullable = false)
    private Integer snapshotVersion;

    /**
     * Snapshot Data: Complete aggregate state in JSON format
     * Stored as JSONB for efficient querying
     *
     * Example for Patient aggregate:
     * {
     *   "patientId": "123e4567-e89b-12d3-a456-426614174000",
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "dateOfBirth": "1980-01-15",
     *   "status": "ACTIVE",
     *   "riskScore": 85.5,
     *   "lastUpdated": "2026-01-19T10:30:00Z"
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(name = "snapshot_data", nullable = false, columnDefinition = "jsonb")
    private String snapshotData;

    /**
     * Created At: When this snapshot was created
     */
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    /**
     * Tenant ID: Multi-tenant isolation (HIPAA requirement)
     */
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    /**
     * Snapshot Strategy: Configurable snapshot frequency
     */
    public static final int DEFAULT_SNAPSHOT_FREQUENCY = 100;

    /**
     * Check if a snapshot should be created based on event version
     *
     * @param eventVersion Current event version
     * @return true if snapshot should be created
     */
    public static boolean shouldCreateSnapshot(int eventVersion) {
        return eventVersion % DEFAULT_SNAPSHOT_FREQUENCY == 0;
    }

    /**
     * Check if a snapshot should be created with custom frequency
     *
     * @param eventVersion Current event version
     * @param frequency Snapshot frequency (e.g., 50 = every 50 events)
     * @return true if snapshot should be created
     */
    public static boolean shouldCreateSnapshot(int eventVersion, int frequency) {
        return eventVersion % frequency == 0;
    }
}
