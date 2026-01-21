package com.healthdata.eventreplay.engine;

import java.time.Instant;
import java.util.UUID;

/**
 * ReplaySnapshot - Immutable snapshot of aggregate state at a specific version
 *
 * Snapshots optimize replay performance by capturing state at regular intervals,
 * allowing incremental replay from the snapshot point rather than from beginning.
 */
public class ReplaySnapshot {
    private final String snapshotId;
    private final String aggregateId;
    private final String tenantId;
    private final long version;
    private final Instant createdAt;
    private final byte[] snapshotData; // Serialized state

    public ReplaySnapshot(String aggregateId, String tenantId, long version) {
        this.snapshotId = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.tenantId = tenantId;
        this.version = version;
        this.createdAt = Instant.now();
        this.snapshotData = new byte[0]; // Placeholder
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public byte[] getSnapshotData() {
        return snapshotData;
    }
}
