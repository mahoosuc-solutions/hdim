package com.healthdata.eventreplay.temporal;

import java.time.Instant;

/**
 * TemporalSnapshot - Immutable state snapshot at a specific point in time
 */
public class TemporalSnapshot {
    private final String aggregateId;
    private final String tenantId;
    private final long version;
    private final Instant timestamp;
    private boolean conditionDiagnosed;
    private boolean medicationPrescribed;

    public TemporalSnapshot(String aggregateId, String tenantId, long version, Instant timestamp) {
        this.aggregateId = aggregateId;
        this.tenantId = tenantId;
        this.version = version;
        this.timestamp = timestamp;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean hasOpenGap() {
        return conditionDiagnosed && !medicationPrescribed;
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
}
