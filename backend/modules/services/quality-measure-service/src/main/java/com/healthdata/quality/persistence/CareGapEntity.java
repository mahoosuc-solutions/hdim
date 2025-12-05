package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Care Gap Entity
 * Represents identified care gaps that require clinical attention
 */
@Entity
@Table(name = "care_gaps", indexes = {
    @Index(name = "idx_cg_patient_status", columnList = "patient_id, status"),
    @Index(name = "idx_cg_patient_priority", columnList = "patient_id, priority"),
    @Index(name = "idx_cg_due_date", columnList = "due_date"),
    @Index(name = "idx_cg_quality_measure", columnList = "quality_measure"),
    @Index(name = "idx_cg_patient_measure_status", columnList = "patient_id, measure_result_id, status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareGapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false, length = 100)
    private String patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private GapCategory category;

    @Column(name = "gap_type", nullable = false, length = 100)
    private String gapType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "quality_measure", length = 50)
    private String qualityMeasure;

    @Column(name = "measure_result_id")
    private UUID measureResultId;

    @Builder.Default
    @Column(name = "created_from_measure", nullable = false)
    private boolean createdFromMeasure = false;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "identified_date", nullable = false)
    private Instant identifiedDate;

    @Column(name = "addressed_date")
    private Instant addressedDate;

    @Column(name = "addressed_by", length = 255)
    private String addressedBy;

    @Column(name = "addressed_notes", columnDefinition = "TEXT")
    private String addressedNotes;

    @Builder.Default
    @Column(name = "auto_closed", nullable = false)
    private Boolean autoClosed = false;

    @Column(name = "evidence_resource_id", length = 100)
    private String evidenceResourceId;

    @Column(name = "evidence_resource_type", length = 50)
    private String evidenceResourceType;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by", length = 255)
    private String closedBy;

    @Column(name = "matching_codes", columnDefinition = "TEXT")
    private String matchingCodes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (identifiedDate == null) {
            identifiedDate = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Care Gap Categories
     */
    public enum GapCategory {
        PREVENTIVE_CARE,
        CHRONIC_DISEASE,
        MENTAL_HEALTH,
        MEDICATION,
        SCREENING,
        SOCIAL_DETERMINANTS
    }

    /**
     * Priority Levels
     */
    public enum Priority {
        URGENT,  // Requires immediate attention
        HIGH,    // Address within 1 week
        MEDIUM,  // Address within 1 month
        LOW      // Address within 3 months
    }

    /**
     * Gap Status
     */
    public enum Status {
        OPEN,         // Newly identified
        IN_PROGRESS,  // Being addressed
        ADDRESSED,    // Action taken
        CLOSED,       // Resolved
        DISMISSED     // Not applicable or patient declined
    }
}
