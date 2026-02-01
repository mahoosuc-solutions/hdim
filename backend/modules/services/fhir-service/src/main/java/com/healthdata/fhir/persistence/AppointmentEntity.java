package com.healthdata.fhir.persistence;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA Entity representing a FHIR Appointment resource.
 * Stores scheduled visits and appointments.
 */
@Entity
@Table(name = "appointments")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "resource_type", nullable = false, length = 32)
    private String resourceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_json", nullable = false, columnDefinition = "jsonb")
    private String resourceJson;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "appointment_type_code", length = 128)
    private String appointmentTypeCode;

    @Column(name = "appointment_type_display", length = 256)
    private String appointmentTypeDisplay;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "location_id", length = 255)
    private String locationId;

    @Column(name = "practitioner_id", length = 255)
    private String practitionerId;

    @Column(name = "created_by", nullable = false, length = 255, updatable = false)
    private String createdBy;

    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String lastModifiedBy;

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
            resourceType = "Appointment";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastModifiedAt = Instant.now();
    }
}
