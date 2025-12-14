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
 * JPA Entity for FHIR MedicationAdministration resource.
 *
 * MedicationAdministration describes the actual administration of a medication
 * to a patient. This is distinct from MedicationRequest which represents
 * a prescription or order, and MedicationDispense which represents the
 * supply of medication.
 */
@Entity
@Table(name = "medication_administrations")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MedicationAdministrationEntity {

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

    @Column(name = "encounter_id")
    private UUID encounterId;

    @Column(name = "medication_request_id")
    private UUID medicationRequestId;

    @Column(name = "medication_code", length = 128)
    private String medicationCode;

    @Column(name = "medication_system", length = 128)
    private String medicationSystem;

    @Column(name = "medication_display", length = 512)
    private String medicationDisplay;

    /**
     * Status of the administration:
     * in-progress | not-done | on-hold | completed | entered-in-error | stopped | unknown
     */
    @Column(name = "status", length = 32)
    private String status;

    /**
     * Category of administration (e.g., inpatient, outpatient, community)
     */
    @Column(name = "category", length = 64)
    private String category;

    /**
     * Date/time when the administration occurred or started
     */
    @Column(name = "effective_date_time")
    private LocalDateTime effectiveDateTime;

    /**
     * Start of administration period (if period was specified)
     */
    @Column(name = "effective_period_start")
    private LocalDateTime effectivePeriodStart;

    /**
     * End of administration period (if period was specified)
     */
    @Column(name = "effective_period_end")
    private LocalDateTime effectivePeriodEnd;

    /**
     * Who performed the administration
     */
    @Column(name = "performer_id", length = 255)
    private String performerId;

    /**
     * Reason the administration was performed
     */
    @Column(name = "reason_code", length = 128)
    private String reasonCode;

    /**
     * Route of administration (e.g., oral, IV, IM)
     */
    @Column(name = "route_code", length = 64)
    private String routeCode;

    @Column(name = "route_display", length = 255)
    private String routeDisplay;

    /**
     * Body site of administration
     */
    @Column(name = "site_code", length = 64)
    private String siteCode;

    @Column(name = "site_display", length = 255)
    private String siteDisplay;

    /**
     * Amount administered
     */
    @Column(name = "dose_value")
    private Double doseValue;

    @Column(name = "dose_unit", length = 64)
    private String doseUnit;

    /**
     * Rate of administration (for IV)
     */
    @Column(name = "rate_value")
    private Double rateValue;

    @Column(name = "rate_unit", length = 64)
    private String rateUnit;

    /**
     * Lot number for the administered medication
     */
    @Column(name = "lot_number", length = 64)
    private String lotNumber;

    /**
     * Expiration date of the medication
     */
    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dosage", columnDefinition = "jsonb")
    private String dosage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private Instant lastModifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.lastModifiedAt = now;
        if (this.version == null) {
            this.version = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.lastModifiedAt = Instant.now();
    }
}
