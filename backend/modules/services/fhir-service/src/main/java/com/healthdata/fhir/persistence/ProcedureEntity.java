package com.healthdata.fhir.persistence;

import java.time.Instant;
import java.time.LocalDate;
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
 * JPA Entity representing a FHIR Procedure resource.
 * Stores procedures, surgeries, and interventions performed on patients.
 */
@Entity
@Table(name = "procedures")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProcedureEntity {

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

    // Procedure code
    @Column(name = "procedure_code", length = 128)
    private String procedureCode;

    @Column(name = "procedure_system", length = 128)
    private String procedureSystem;

    @Column(name = "procedure_display", length = 512)
    private String procedureDisplay;

    // Status (preparation, in-progress, not-done, on-hold, stopped, completed, entered-in-error, unknown)
    @Column(name = "status", length = 32)
    private String status;

    // Category (e.g., surgical procedure, diagnostic procedure)
    @Column(name = "category_code", length = 128)
    private String categoryCode;

    @Column(name = "category_display", length = 256)
    private String categoryDisplay;

    // Performed date/period
    @Column(name = "performed_date")
    private LocalDate performedDate;

    @Column(name = "performed_start")
    private LocalDateTime performedStart;

    @Column(name = "performed_end")
    private LocalDateTime performedEnd;

    // Performer (primary practitioner)
    @Column(name = "performer_id", length = 255)
    private String performerId;

    @Column(name = "performer_display", length = 512)
    private String performerDisplay;

    @Column(name = "performer_function", length = 128)
    private String performerFunction;

    // Location
    @Column(name = "location_id", length = 255)
    private String locationId;

    @Column(name = "location_display", length = 512)
    private String locationDisplay;

    // Reason for procedure (first reason if multiple)
    @Column(name = "reason_code", length = 128)
    private String reasonCode;

    @Column(name = "reason_system", length = 128)
    private String reasonSystem;

    @Column(name = "reason_display", length = 512)
    private String reasonDisplay;

    // Reason reference (condition, observation)
    @Column(name = "reason_reference", length = 255)
    private String reasonReference;

    // Body site (first body site if multiple)
    @Column(name = "body_site_code", length = 128)
    private String bodySiteCode;

    @Column(name = "body_site_system", length = 128)
    private String bodySiteSystem;

    @Column(name = "body_site_display", length = 512)
    private String bodySiteDisplay;

    // Outcome
    @Column(name = "outcome_code", length = 128)
    private String outcomeCode;

    @Column(name = "outcome_display", length = 256)
    private String outcomeDisplay;

    // Complication (first complication if multiple)
    @Column(name = "complication_code", length = 128)
    private String complicationCode;

    @Column(name = "complication_display", length = 256)
    private String complicationDisplay;

    // Encounter reference
    @Column(name = "encounter_id")
    private UUID encounterId;

    // Based on (previous procedure, care plan)
    @Column(name = "based_on_reference", length = 255)
    private String basedOnReference;

    // Part of (larger procedure)
    @Column(name = "part_of_reference", length = 255)
    private String partOfReference;

    // Recorded date
    @Column(name = "recorded_date")
    private LocalDate recordedDate;

    // Notes indicator
    @Column(name = "has_notes")
    private Boolean hasNotes;

    // Audit fields
    @Column(name = "created_by", nullable = false, length = 255, updatable = false)
    private String createdBy;

    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String modifiedBy;

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
            resourceType = "Procedure";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastModifiedAt = Instant.now();
    }
}
