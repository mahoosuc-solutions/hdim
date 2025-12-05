package com.healthdata.fhir.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * FHIR MedicationRequest resource representation
 * Represents an order or request for both supply of the medication and instructions for administration
 */
@Entity
@Table(name = "medication_requests", schema = "fhir")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "medication_code", nullable = false)
    private String medicationCode;

    @Column(name = "medication_display")
    private String medicationDisplay;

    @Column(name = "status", nullable = false)
    private String status; // active, on-hold, cancelled, completed, entered-in-error, stopped, draft

    @Column(name = "intent", nullable = false)
    private String intent; // proposal, plan, order, original-order, reflex-order, filler-order, instance-order, option

    @Column(name = "priority")
    private String priority; // routine, urgent, asap, stat

    @Column(name = "dosage_instruction", columnDefinition = "TEXT")
    private String dosageInstruction;

    @Column(name = "dosage_timing")
    private String dosageTiming; // BID, TID, QID, daily, etc.

    @Column(name = "dosage_quantity")
    private Double dosageQuantity;

    @Column(name = "dosage_unit")
    private String dosageUnit;

    @Column(name = "dispense_quantity")
    private Integer dispenseQuantity;

    @Column(name = "dispense_unit")
    private String dispenseUnit;

    @Column(name = "days_supply")
    private Integer daysSupply;

    @Column(name = "refills_remaining")
    private Integer refillsRemaining;

    @Column(name = "authored_on")
    private LocalDateTime authoredOn;

    @Column(name = "valid_period_start")
    private LocalDateTime validPeriodStart;

    @Column(name = "valid_period_end")
    private LocalDateTime validPeriodEnd;

    @Column(name = "prescriber_id")
    private String prescriberId;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "reason_display")
    private String reasonDisplay;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}