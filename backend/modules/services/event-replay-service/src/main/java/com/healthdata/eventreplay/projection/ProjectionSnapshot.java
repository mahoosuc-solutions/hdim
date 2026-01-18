package com.healthdata.eventreplay.projection;

import java.time.Instant;
import java.util.UUID;

/**
 * ProjectionSnapshot - Immutable snapshot of projection state at a specific version
 */
public class ProjectionSnapshot {
    private final String snapshotId;
    private final String aggregateId;
    private final String tenantId;
    private final long version;
    private final Instant createdAt;

    public ProjectionSnapshot(String aggregateId, String tenantId, long version) {
        this.snapshotId = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.tenantId = tenantId;
        this.version = version;
        this.createdAt = Instant.now();
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
}
