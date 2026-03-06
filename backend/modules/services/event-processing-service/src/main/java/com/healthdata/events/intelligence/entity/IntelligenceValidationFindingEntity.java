package com.healthdata.events.intelligence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted validation finding generated during intelligence ingestion.
 */
@Entity
@Table(name = "intelligence_validation_findings", indexes = {
        @Index(name = "idx_finding_tenant_patient_created", columnList = "tenant_id, patient_ref, created_at DESC"),
        @Index(name = "idx_finding_tenant_status_created", columnList = "tenant_id, status, created_at DESC"),
        @Index(name = "idx_finding_id_tenant", columnList = "id, tenant_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class IntelligenceValidationFindingEntity {

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH
    }

    public enum FindingType {
        STRUCTURE,
        DATA_COMPLETENESS,
        TEMPORAL,
        CONSISTENCY
    }

    public enum FindingStatus {
        OPEN,
        RESOLVED,
        DISMISSED
    }

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "patient_ref", nullable = false, length = 128)
    private String patientRef;

    @Column(name = "source_event_id", nullable = false, length = 128)
    private String sourceEventId;

    @Column(name = "rule_code", nullable = false, length = 80)
    private String ruleCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "finding_type", nullable = false, length = 32)
    private FindingType findingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private FindingStatus status;

    @Column(name = "details", nullable = false, columnDefinition = "jsonb")
    private String details;

    @Column(name = "acted_by", length = 128)
    private String actedBy;

    @Column(name = "action_notes", columnDefinition = "text")
    private String actionNotes;

    @Column(name = "acted_at")
    private Instant actedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
