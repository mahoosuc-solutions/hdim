package com.healthdata.nurseworkflow.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Medication Reconciliation Entity
 *
 * Tracks medication reconciliation workflow from initiation to completion.
 * Implements Joint Commission NPSG.03.06.01 requirement to maintain and communicate
 * accurate medication information.
 *
 * Workflow:
 * 1. Started: When med rec task created (post-discharge, admission, transfer)
 * 2. In Progress: Nurse actively reconciling medications
 * 3. Completed: Final list verified, patient educated, Task updated
 * 4. Cancelled: No longer needed (e.g., patient refused, admitted elsewhere)
 *
 * HIPAA Compliance: Medication lists contain PHI - all access requires audit logging.
 */
@Entity
@Table(name = "medication_reconciliations", indexes = {
    @Index(name = "idx_med_rec_tenant_patient", columnList = "tenant_id, patient_id"),
    @Index(name = "idx_med_rec_reconciler", columnList = "reconciler_id"),
    @Index(name = "idx_med_rec_status", columnList = "status"),
    @Index(name = "idx_med_rec_trigger", columnList = "trigger_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationReconciliationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "reconciler_id", nullable = false)
    private UUID reconcilerId; // RN or pharmacist who performed reconciliation

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReconciliationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 50)
    private TriggerType triggerType; // What event initiated med rec

    // Number of medications reconciled (for analytics/compliance)
    @Column(name = "medication_count")
    private Integer medicationCount;

    // Number of discrepancies identified (dose changes, new meds, discontinued meds, etc.)
    @Column(name = "discrepancy_count")
    private Integer discrepancyCount;

    // Patient education provided (teach-back method)
    @Column(name = "patient_education_provided")
    private Boolean patientEducationProvided;

    // Patient understanding level (Good, Fair, Poor)
    @Enumerated(EnumType.STRING)
    @Column(name = "patient_understanding")
    private PatientUnderstanding patientUnderstanding;

    // Notes about reconciliation process/findings
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // FHIR Resource References
    @Column(name = "task_id", length = 128)
    private String taskId; // FHIR Task resource UUID (medication-reconciliation task)

    @Column(name = "encounter_id", length = 128)
    private String encounterId; // FHIR Encounter resource UUID (if applicable)

    // Timestamps
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
        this.createdAt = Instant.now();
    }

    /**
     * Medication reconciliation status
     */
    public enum ReconciliationStatus {
        REQUESTED,      // Task created, awaiting nurse
        IN_PROGRESS,    // Nurse actively reconciling
        COMPLETED,      // All steps done, list finalized
        CANCELLED       // No longer needed
    }

    /**
     * What triggered the medication reconciliation requirement
     */
    public enum TriggerType {
        HOSPITAL_ADMISSION,      // Patient admitted to hospital
        HOSPITAL_DISCHARGE,      // Patient discharged from hospital
        ED_VISIT,               // Emergency Department visit
        SPECIALTY_REFERRAL,     // New specialty encounter
        MEDICATION_CHANGE,      // Provider made significant med change
        ROUTINE,                // Routine med rec (annual, quarterly)
        PATIENT_REQUEST         // Patient requested med rec
    }

    /**
     * Patient's understanding of medications after education
     */
    public enum PatientUnderstanding {
        EXCELLENT,  // Patient can teach back all concepts
        GOOD,       // Patient understands most concepts
        FAIR,       // Patient understands some concepts
        POOR        // Patient needs additional education/follow-up
    }
}
