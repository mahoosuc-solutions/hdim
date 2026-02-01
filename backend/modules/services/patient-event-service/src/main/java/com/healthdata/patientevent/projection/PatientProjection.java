package com.healthdata.patientevent.projection;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Patient Projection Entity (CQRS Read Model)
 *
 * Denormalized patient view optimized for fast queries.
 * Updated through Kafka event stream.
 */
@Entity
@Table(name = "patient_projections", indexes = {
    @Index(name = "idx_pp_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_pp_tenant_patient_id", columnList = "tenant_id, patient_id", unique = true),
    @Index(name = "idx_pp_tenant_last_name", columnList = "tenant_id, last_name"),
    @Index(name = "idx_pp_updated_at", columnList = "last_updated_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tenant Isolation (CRITICAL)
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    // Patient Identifiers
    @Column(name = "patient_id", nullable = false, length = 100)
    private UUID patientId;

    @Column(name = "fhir_id", length = 100)
    private String fhirId;

    // Demographics
    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "email", length = 255)
    private String email;

    // Address Information
    @Column(name = "address_street", length = 255)
    private String addressStreet;

    @Column(name = "address_city", length = 100)
    private String addressCity;

    @Column(name = "address_state", length = 50)
    private String addressState;

    @Column(name = "address_zip", length = 10)
    private String addressZip;

    // Patient Status
    @Column(name = "status", length = 50)
    private String status;  // ACTIVE, INACTIVE, DECEASED

    // Risk Score (aggregated from risk-assessment.updated event)
    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "risk_level", length = 50)
    private String riskLevel;  // LOW, MEDIUM, HIGH

    // Care Gap Count (aggregated from care-gap events)
    @Column(name = "open_care_gaps_count", nullable = false)
    @lombok.Builder.Default
    private Integer openCareGapsCount = 0;

    @Column(name = "urgent_care_gaps_count", nullable = false)
    @lombok.Builder.Default
    private Integer urgentCareGapsCount = 0;

    // Clinical Flags
    @Column(name = "active_alerts_count", nullable = false)
    @lombok.Builder.Default
    private Integer activeAlertsCount = 0;

    @Column(name = "has_critical_alert")
    @lombok.Builder.Default
    private Boolean hasCriticalAlert = false;

    // Mental Health Flag (aggregated from mental-health.updated event)
    @Column(name = "mental_health_flag")
    @lombok.Builder.Default
    private Boolean mentalHealthFlag = false;

    @Column(name = "mental_health_score")
    private Integer mentalHealthScore;

    // Audit Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @Column(name = "event_version", nullable = false)
    @lombok.Builder.Default
    private Long eventVersion = 0L;

    /**
     * Lifecycle hook: Set creation timestamp
     */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        lastUpdatedAt = Instant.now();
    }

    /**
     * Lifecycle hook: Update timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = Instant.now();
    }
}
