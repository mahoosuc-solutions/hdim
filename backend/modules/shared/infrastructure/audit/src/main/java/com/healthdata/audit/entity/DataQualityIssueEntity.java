package com.healthdata.audit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for data quality issues
 */
@Entity
@Table(name = "data_quality_issues", indexes = {
    @Index(name = "idx_dqi_tenant_status", columnList = "tenant_id,status"),
    @Index(name = "idx_dqi_patient_id", columnList = "patient_id"),
    @Index(name = "idx_dqi_severity", columnList = "severity"),
    @Index(name = "idx_dqi_detected_at", columnList = "detected_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityIssueEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;
    
    @Column(name = "patient_id", nullable = false, length = 100)
    private String patientId;
    
    @Column(name = "issue_type", nullable = false, length = 50)
    private String issueType;  // DUPLICATE, INCOMPLETE, INCONSISTENT, INVALID
    
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;  // CRITICAL, HIGH, MEDIUM, LOW
    
    @Column(name = "status", nullable = false, length = 20)
    private String status;  // OPEN, IN_PROGRESS, RESOLVED, IGNORED
    
    @Column(name = "description", length = 2000)
    private String description;
    
    @Column(name = "affected_field", length = 100)
    private String affectedField;
    
    @Column(name = "current_value", length = 500)
    private String currentValue;
    
    @Column(name = "suggested_value", length = 500)
    private String suggestedValue;
    
    @Column(name = "recommendation", length = 2000)
    private String recommendation;
    
    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;
    
    @Column(name = "resolution_notes", length = 2000)
    private String resolutionNotes;
    
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
