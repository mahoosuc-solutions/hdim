package com.healthdata.fhir.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "allergy_intolerances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllergyIntoleranceEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    // Clinical status
    @Column(name = "clinical_status")
    private String clinicalStatus;  // active, inactive, resolved

    @Column(name = "verification_status")
    private String verificationStatus;  // confirmed, unconfirmed, refuted, entered-in-error

    // Type and category
    @Column(name = "type")
    private String type;  // allergy, intolerance

    @Column(name = "category")
    private String category;  // food, medication, environment, biologic

    // Criticality
    @Column(name = "criticality")
    private String criticality;  // low, high, unable-to-assess

    // Allergen (code, system, display)
    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "code_system")
    private String codeSystem;  // e.g., RxNorm, SNOMED-CT

    @Column(name = "code_display")
    private String codeDisplay;

    // Onset
    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Column(name = "onset_age")
    private Integer onsetAge;

    // Recorded date
    @Column(name = "recorded_date")
    private LocalDateTime recordedDate;

    // Recorder
    @Column(name = "recorder_id")
    private String recorderId;

    @Column(name = "recorder_display")
    private String recorderDisplay;

    // Asserter
    @Column(name = "asserter_id")
    private String asserterId;

    @Column(name = "asserter_display")
    private String asserterDisplay;

    // Last occurrence
    @Column(name = "last_occurrence")
    private LocalDateTime lastOccurrence;

    // Note
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // Reactions
    @Column(name = "has_reactions")
    private Boolean hasReactions;

    @Column(name = "reaction_substance")
    private String reactionSubstance;

    @Column(name = "reaction_manifestation")
    private String reactionManifestation;  // e.g., "hives", "anaphylaxis"

    @Column(name = "reaction_severity")
    private String reactionSeverity;  // mild, moderate, severe

    @Column(name = "reaction_exposure_route")
    private String reactionExposureRoute;  // oral, injection, inhalation, topical

    // Encounter reference
    @Column(name = "encounter_id")
    private UUID encounterId;

    // FHIR resource stored as JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fhir_resource", columnDefinition = "JSONB")
    private String fhirResource;

    // Audit fields
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    // Optimistic locking
    @Version
    @Column(name = "version")
    private Integer version;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        lastModifiedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedAt = LocalDateTime.now();
    }
}
