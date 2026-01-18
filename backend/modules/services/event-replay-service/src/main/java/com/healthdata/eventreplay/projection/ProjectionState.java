package com.healthdata.eventreplay.projection;

import com.healthdata.eventsourcing.event.DomainEvent;
import java.time.Instant;
import java.util.*;

/**
 * ProjectionState - Denormalized read model built from events
 *
 * Represents the aggregated state of a domain object as of a specific version.
 * Optimized for query performance while maintaining consistency with event store.
 */
public class ProjectionState {
    private final String aggregateId;
    private final String tenantId;
    private long version;
    private Instant lastUpdatedAt;
    private final Set<String> duplicateEventIds;
    private boolean openGap;
    private boolean conditionDiagnosed;
    private boolean medicationPrescribed;

    public ProjectionState(String aggregateId, String tenantId, long initialVersion) {
        this.aggregateId = aggregateId;
        this.tenantId = tenantId;
        this.version = initialVersion;
        this.lastUpdatedAt = Instant.now();
        this.duplicateEventIds = new HashSet<>();
        this.openGap = false;
        this.conditionDiagnosed = false;
        this.medicationPrescribed = false;
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

    public void setVersion(long version) {
        this.version = version;
        this.lastUpdatedAt = Instant.now();
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public Set<String> getDuplicateEventIds() {
        return duplicateEventIds;
    }

    public boolean hasOpenGap() {
        return openGap;
    }

    public void setOpenGap(boolean openGap) {
        this.openGap = openGap;
    }

    public boolean hasConditionDiagnosed() {
        return conditionDiagnosed;
    }

    public void setConditionDiagnosed(boolean conditionDiagnosed) {
        this.conditionDiagnosed = conditionDiagnosed;
    }

    public boolean hasMedicationPrescribed() {
        return medicationPrescribed;
    }

    public void setMedicationPrescribed(boolean medicationPrescribed) {
        this.medicationPrescribed = medicationPrescribed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectionState that = (ProjectionState) o;
        return version == that.version &&
            Objects.equals(aggregateId, that.aggregateId) &&
            Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aggregateId, tenantId, version);
    }
}
