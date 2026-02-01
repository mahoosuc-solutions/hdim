package com.healthdata.clinicalworkflow.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Patient Check-in Entity
 *
 * Tracks patient check-in events for MA workflow:
 * - Check-in timestamp and staff
 * - Insurance verification
 * - Demographics update
 * - Consent collection
 *
 * Links to FHIR Appointment and Encounter resources
 * Multi-tenant with strict tenant_id filtering
 */
@Entity
@Table(
    name = "patient_check_ins",
    indexes = {
        @Index(name = "idx_patient_check_ins_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_patient_check_ins_patient_id", columnList = "tenant_id, patient_id"),
        @Index(name = "idx_patient_check_ins_check_in_time", columnList = "tenant_id, check_in_time")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientCheckInEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "appointment_id", length = 255)
    private String appointmentId;

    @Column(name = "encounter_id", length = 255)
    private String encounterId;

    @Column(name = "check_in_time", nullable = false)
    private Instant checkInTime;

    @Column(name = "checked_in_by", nullable = false, length = 255)
    private String checkedInBy;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "checked-in";

    @Column(name = "insurance_verified", nullable = false)
    @Builder.Default
    private Boolean insuranceVerified = false;

    @Column(name = "demographics_updated", nullable = false)
    @Builder.Default
    private Boolean demographicsUpdated = false;

    @Column(name = "consent_obtained", nullable = false)
    @Builder.Default
    private Boolean consentObtained = false;

    @Column(name = "verified_by", length = 255)
    private String verifiedBy;

    @Column(name = "consent_obtained_by", length = 255)
    private String consentObtainedBy;

    @Column(name = "demographics_updated_by", length = 255)
    private String demographicsUpdatedBy;

    @Column(name = "waiting_time_minutes")
    private Integer waitingTimeMinutes;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        checkInTime = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
