package com.healthdata.fhir.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * FHIR Condition resource representation
 * Represents a clinical condition, problem, diagnosis, or other event
 */
@Entity
@Table(name = "conditions", schema = "fhir")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Condition {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "display")
    private String display;

    @Column(name = "clinical_status")
    private String clinicalStatus; // active, recurrence, relapse, inactive, remission, resolved

    @Column(name = "verification_status")
    private String verificationStatus; // unconfirmed, provisional, differential, confirmed, refuted

    @Column(name = "category")
    private String category; // problem-list-item, encounter-diagnosis

    @Column(name = "severity")
    private String severity; // mild, moderate, severe

    @Column(name = "onset_date")
    private LocalDateTime onsetDate;

    @Column(name = "recorded_date")
    private LocalDateTime recordedDate;

    @Column(name = "abatement_date")
    private LocalDateTime abatementDate;

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