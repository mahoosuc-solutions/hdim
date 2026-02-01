package com.healthdata.clinicalworkflow.domain.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Pre-visit Checklist Entity
 *
 * Configurable pre-visit task management for MAs:
 * - Standard checklist items (review history, verify insurance, etc.)
 * - Custom items stored as JSON array
 * - Completion percentage and status tracking
 * - Appointment type-specific checklists
 *
 * Supports preparation for different visit types
 * Multi-tenant isolation required
 */
@Entity
@Table(
    name = "pre_visit_checklists",
    indexes = {
        @Index(name = "idx_pre_visit_checklists_tenant_patient", columnList = "tenant_id, patient_id"),
        @Index(name = "idx_pre_visit_checklists_status", columnList = "tenant_id, status"),
        @Index(name = "idx_pre_visit_checklists_appointment_id", columnList = "tenant_id, appointment_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreVisitChecklistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "appointment_id", length = 255)
    private String appointmentId;

    @Column(name = "appointment_type", nullable = false, length = 100, columnDefinition = "VARCHAR(100) COMMENT 'new-patient, follow-up, procedure-pre, etc.'")
    private String appointmentType;

    // Standard Checklist Items
    @Column(name = "review_medical_history", nullable = false)
    @Builder.Default
    private Boolean reviewMedicalHistory = false;

    @Column(name = "verify_insurance", nullable = false)
    @Builder.Default
    private Boolean verifyInsurance = false;

    @Column(name = "update_demographics", nullable = false)
    @Builder.Default
    private Boolean updateDemographics = false;

    @Column(name = "review_medications", nullable = false)
    @Builder.Default
    private Boolean reviewMedications = false;

    @Column(name = "review_allergies", nullable = false)
    @Builder.Default
    private Boolean reviewAllergies = false;

    @Column(name = "prepare_vitals_equipment", nullable = false)
    @Builder.Default
    private Boolean prepareVitalsEquipment = false;

    @Column(name = "review_care_gaps", nullable = false)
    @Builder.Default
    private Boolean reviewCareGaps = false;

    @Column(name = "obtain_consent", nullable = false)
    @Builder.Default
    private Boolean obtainConsent = false;

    // Custom Items as JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_items", columnDefinition = "jsonb")
    private JsonNode customItems;

    // Status
    @Column(name = "completion_percentage", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal completionPercentage = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 50, columnDefinition = "VARCHAR(50) COMMENT 'pending, in-progress, completed'")
    @Builder.Default
    private String status = "pending";

    @Column(name = "completed_by", length = 255)
    private String completedBy;

    @Column(name = "completed_at")
    private Instant completedAt;

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
        updateCompletionPercentage();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        updateCompletionPercentage();
    }

    /**
     * Calculate completion percentage based on checked items
     */
    private void updateCompletionPercentage() {
        int totalItems = 8;  // Total standard items
        int completedItems = 0;

        if (reviewMedicalHistory) completedItems++;
        if (verifyInsurance) completedItems++;
        if (updateDemographics) completedItems++;
        if (reviewMedications) completedItems++;
        if (reviewAllergies) completedItems++;
        if (prepareVitalsEquipment) completedItems++;
        if (reviewCareGaps) completedItems++;
        if (obtainConsent) completedItems++;

        // Include custom items if present
        if (customItems != null && customItems.isArray()) {
            totalItems += customItems.size();
            for (JsonNode item : customItems) {
                if (item.has("completed") && item.get("completed").asBoolean()) {
                    completedItems++;
                }
            }
        }

        if (totalItems > 0) {
            completionPercentage = new BigDecimal(completedItems)
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(totalItems), 2, java.math.RoundingMode.HALF_UP);
        } else {
            completionPercentage = BigDecimal.ZERO;
        }

        // Update status based on completion
        if (completionPercentage.compareTo(BigDecimal.ZERO) > 0 &&
            completionPercentage.compareTo(new BigDecimal("100")) < 0) {
            status = "in-progress";
        } else if (completionPercentage.compareTo(new BigDecimal("100")) == 0) {
            status = "completed";
        }
    }

    /**
     * Get total number of checklist items
     */
    public Integer getTotalItems() {
        int count = 8;  // Standard items
        if (customItems != null && customItems.isArray()) {
            count += customItems.size();
        }
        return count;
    }

    /**
     * Get number of completed items
     */
    public Integer getCompletedItems() {
        int count = 0;
        if (reviewMedicalHistory) count++;
        if (verifyInsurance) count++;
        if (updateDemographics) count++;
        if (reviewMedications) count++;
        if (reviewAllergies) count++;
        if (prepareVitalsEquipment) count++;
        if (reviewCareGaps) count++;
        if (obtainConsent) count++;

        if (customItems != null && customItems.isArray()) {
            for (JsonNode item : customItems) {
                if (item.has("completed") && item.get("completed").asBoolean()) {
                    count++;
                }
            }
        }
        return count;
    }
}
