package com.healthdata.audit.entity.clinical;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Clinical decision review entity
 */
@Entity
@Table(name = "clinical_decisions", indexes = {
    @Index(name = "idx_clinical_tenant_status", columnList = "tenant_id, review_status"),
    @Index(name = "idx_clinical_decision_timestamp", columnList = "decision_timestamp"),
    @Index(name = "idx_clinical_patient_id", columnList = "patient_id"),
    @Index(name = "idx_clinical_decision_type", columnList = "decision_type"),
    @Index(name = "idx_clinical_severity", columnList = "alert_severity")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalDecisionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;
    
    @Column(name = "patient_id", nullable = false, length = 100)
    private String patientId;
    
    @Column(name = "decision_type", nullable = false, length = 50)
    private String decisionType; // MEDICATION_ALERT, CARE_GAP, RISK_STRATIFICATION, CLINICAL_PATHWAY
    
    @Column(name = "alert_severity", length = 20)
    private String alertSeverity; // CRITICAL, HIGH, MODERATE, LOW
    
    @Column(name = "decision_timestamp", nullable = false)
    private LocalDateTime decisionTimestamp;
    
    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus; // PENDING, APPROVED, REJECTED, NEEDS_REVISION
    
    @Column(name = "evidence_grade", length = 10)
    private String evidenceGrade; // A, B, C, D
    
    @Column(name = "confidence_score")
    private Double confidenceScore;
    
    @Column(name = "specialty_area", length = 100)
    private String specialtyArea;
    
    @Type(JsonBinaryType.class)
    @Column(name = "patient_context", columnDefinition = "jsonb")
    private Map<String, Object> patientContext;
    
    @Type(JsonBinaryType.class)
    @Column(name = "recommendation", columnDefinition = "jsonb")
    private Map<String, Object> recommendation;
    
    @Type(JsonBinaryType.class)
    @Column(name = "evidence", columnDefinition = "jsonb")
    private Map<String, Object> evidence;
    
    @Type(JsonBinaryType.class)
    @Column(name = "clinical_details", columnDefinition = "jsonb")
    private Map<String, Object> clinicalDetails;
    
    // Review tracking
    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "review_notes", length = 2000)
    private String reviewNotes;
    
    // Override tracking
    @Column(name = "has_override")
    private Boolean hasOverride;
    
    @Column(name = "override_reason", length = 1000)
    private String overrideReason;
    
    @Column(name = "override_applied_by", length = 100)
    private String overrideAppliedBy;
    
    @Column(name = "override_applied_at")
    private LocalDateTime overrideAppliedAt;
    
    // Audit timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
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
