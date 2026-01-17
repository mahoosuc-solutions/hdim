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
 * Patient Education Log Entity
 *
 * Documents patient education delivery including material provided, teaching method,
 * patient understanding (using teach-back method), and identified barriers to learning.
 *
 * Implements:
 * - Patient-Centered Care: Individualized education
 * - Health Literacy: Uses plain language, assesses understanding
 * - Teach-Back Method: Validates patient comprehension
 * - Meaningful Use: Tracks patient education activities for quality measures
 *
 * HIPAA Compliance: Education logs are PHI - all access requires audit logging.
 */
@Entity
@Table(name = "patient_education_logs", indexes = {
    @Index(name = "idx_edu_tenant_patient", columnList = "tenant_id, patient_id"),
    @Index(name = "idx_edu_educator", columnList = "educator_id"),
    @Index(name = "idx_edu_delivered_at", columnList = "delivered_at"),
    @Index(name = "idx_edu_material_type", columnList = "material_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientEducationLogEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "educator_id", nullable = false)
    private UUID educatorId; // RN or other healthcare provider

    @Column(name = "material_id", length = 128)
    private String materialId; // Reference to DocumentReference resource in FHIR

    @Enumerated(EnumType.STRING)
    @Column(name = "material_type", nullable = false, length = 100)
    private MaterialType materialType; // Disease/condition type for categorization

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false, length = 50)
    private DeliveryMethod deliveryMethod;

    // Topics covered during education session (comma-separated or JSON)
    @Column(name = "topics_covered", columnDefinition = "TEXT")
    private String topicsCovered; // JSON array of topics

    // Teach-back method: Did educator assess understanding?
    @Column(name = "teach_back_performed")
    private Boolean teachBackPerformed;

    // Patient's demonstrated understanding level
    @Enumerated(EnumType.STRING)
    @Column(name = "patient_understanding")
    private PatientUnderstanding patientUnderstanding;

    // Identified barriers to learning (health literacy, language, etc.)
    @Column(name = "barriers_to_learning", columnDefinition = "TEXT")
    private String barriersToLearning; // JSON array of barriers

    // Whether language interpreter was used
    @Column(name = "interpreter_used")
    private Boolean interpreterUsed;

    @Column(name = "interpreter_language", length = 50)
    private String interpreterLanguage;

    // Whether family/caregiver was involved in education
    @Column(name = "caregiver_involved")
    private Boolean caregiverInvolved;

    // Time spent in education (minutes)
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    // Clinical context (why this education was provided)
    @Column(name = "clinical_reason", columnDefinition = "TEXT")
    private String clinicalReason;

    // Notes about session
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // FHIR Resource Reference
    @Column(name = "document_reference_id", length = 128)
    private String documentReferenceId; // Link to DocumentReference (educational material)

    // Timestamp
    @Column(name = "delivered_at", nullable = false)
    private Instant deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.deliveredAt == null) {
            this.deliveredAt = Instant.now();
        }
        this.createdAt = Instant.now();
    }

    /**
     * Type of patient education material
     */
    public enum MaterialType {
        DIABETES_MANAGEMENT,
        HYPERTENSION_CONTROL,
        HEART_FAILURE,
        COPD,
        ASTHMA,
        MENTAL_HEALTH,
        MEDICATION_ADHERENCE,
        NUTRITION,
        EXERCISE,
        SMOKING_CESSATION,
        PREVENTIVE_CARE,
        PAIN_MANAGEMENT,
        WOUND_CARE,
        INFECTION_PREVENTION,
        OTHER
    }

    /**
     * Method of education delivery
     */
    public enum DeliveryMethod {
        IN_PERSON,              // Face-to-face education
        PHONE,                  // Over the phone
        VIDEO_CALL,             // Virtual education (Zoom, Teams, etc.)
        EMAIL,                  // Electronic delivery
        PATIENT_PORTAL,         // Via patient portal
        PRINTED_MATERIALS,      // Handed printed materials
        MULTIMEDIA,             // Interactive digital media
        GROUP_SESSION,          // Group education
        ONE_ON_ONE              // Individual education
    }

    /**
     * Patient's understanding level after teach-back assessment
     */
    public enum PatientUnderstanding {
        EXCELLENT,  // Patient can teach back all concepts accurately
        GOOD,       // Patient understands and can explain most concepts
        FAIR,       // Patient understands some concepts, needs clarification
        POOR,       // Patient demonstrates poor understanding, needs further education
        NOT_ASSESSED // Teach-back not performed or not applicable
    }
}
