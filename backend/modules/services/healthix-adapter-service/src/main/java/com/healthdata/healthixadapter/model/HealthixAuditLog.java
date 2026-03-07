package com.healthdata.healthixadapter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * HIPAA audit log entity for all PHI access through Healthix adapter.
 * Every FHIR notification, HL7 message, MPI query, and C-CDA document
 * access MUST be recorded here.
 */
@Entity
@Table(name = "healthix_audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthixAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "source_system", nullable = false, length = 50)
    private String sourceSystem;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "phi_level", nullable = false, length = 20)
    private String phiLevel;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "SUCCESS";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
