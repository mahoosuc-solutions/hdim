package com.healthdata.fhir.persistence;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA Entity representing a FHIR Encounter resource.
 * Stores patient encounters/visits including office visits, ER visits, inpatient stays.
 */
@Entity
@Table(name = "encounters")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EncounterEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "resource_type", nullable = false, length = 32)
    private String resourceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_json", nullable = false, columnDefinition = "jsonb")
    private String resourceJson;

    // Patient reference
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    // Encounter classification (encounter.class)
    @Column(name = "encounter_class", length = 64)
    private String encounterClass;

    // Encounter type (first type if multiple)
    @Column(name = "encounter_type_code", length = 128)
    private String encounterTypeCode;

    @Column(name = "encounter_type_system", length = 128)
    private String encounterTypeSystem;

    @Column(name = "encounter_type_display", length = 512)
    private String encounterTypeDisplay;

    // Status (planned, arrived, in-progress, finished, cancelled)
    @Column(name = "status", length = 32)
    private String status;

    // Service type
    @Column(name = "service_type_code", length = 128)
    private String serviceTypeCode;

    @Column(name = "service_type_display", length = 256)
    private String serviceTypeDisplay;

    // Priority
    @Column(name = "priority", length = 32)
    private String priority;

    // Period
    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    // Duration in minutes
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    // Reason for visit (first reason if multiple)
    @Column(name = "reason_code", length = 128)
    private String reasonCode;

    @Column(name = "reason_system", length = 128)
    private String reasonSystem;

    @Column(name = "reason_display", length = 512)
    private String reasonDisplay;

    // Location reference
    @Column(name = "location_id", length = 255)
    private String locationId;

    @Column(name = "location_display", length = 512)
    private String locationDisplay;

    // Participant (primary provider)
    @Column(name = "participant_id", length = 255)
    private String participantId;

    @Column(name = "participant_display", length = 512)
    private String participantDisplay;

    // Service provider (organization)
    @Column(name = "service_provider_id", length = 255)
    private String serviceProviderId;

    @Column(name = "service_provider_display", length = 512)
    private String serviceProviderDisplay;

    // Hospitalization specific fields
    @Column(name = "admission_source", length = 128)
    private String admissionSource;

    @Column(name = "discharge_disposition", length = 128)
    private String dischargeDisposition;

    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private Instant lastModifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.lastModifiedAt = now;
        if (this.version == null) {
            this.version = 0;
        }
        if (resourceType == null) {
            resourceType = "Encounter";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastModifiedAt = Instant.now();
    }
}
