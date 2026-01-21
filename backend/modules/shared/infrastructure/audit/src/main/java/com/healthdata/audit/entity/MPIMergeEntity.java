package com.healthdata.audit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * JPA Entity for MPI merge operations
 */
@Entity
@Table(name = "mpi_merges", indexes = {
    @Index(name = "idx_mpi_tenant_status", columnList = "tenant_id,merge_status"),
    @Index(name = "idx_mpi_merge_timestamp", columnList = "merge_timestamp"),
    @Index(name = "idx_mpi_validation_status", columnList = "validation_status"),
    @Index(name = "idx_mpi_source_target", columnList = "source_patient_id,target_patient_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MPIMergeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;
    
    @Column(name = "source_patient_id", nullable = false, length = 100)
    private String sourcePatientId;
    
    @Column(name = "target_patient_id", nullable = false, length = 100)
    private String targetPatientId;
    
    @Column(name = "merge_type", length = 50)
    private String mergeType;  // AUTOMATIC, MANUAL, ASSISTED
    
    @Column(name = "confidence_score")
    private Double confidenceScore;
    
    @Column(name = "merge_status", nullable = false, length = 20)
    private String mergeStatus;  // PENDING, VALIDATED, ROLLED_BACK, FAILED
    
    @Column(name = "validation_status", length = 20)
    private String validationStatus;  // NOT_VALIDATED, VALIDATED, VALIDATION_FAILED
    
    @Column(name = "merge_timestamp", nullable = false)
    private LocalDateTime mergeTimestamp;
    
    @Column(name = "performed_by", length = 100)
    private String performedBy;
    
    @Column(name = "source_patient_snapshot", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> sourcePatientSnapshot;
    
    @Column(name = "target_patient_snapshot", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> targetPatientSnapshot;
    
    @Column(name = "merged_patient_snapshot", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> mergedPatientSnapshot;
    
    @Column(name = "matching_details", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> matchingDetails;
    
    @Column(name = "validated_by", length = 100)
    private String validatedBy;
    
    @Column(name = "validated_at")
    private LocalDateTime validatedAt;
    
    @Column(name = "validation_notes", length = 2000)
    private String validationNotes;
    
    @Column(name = "has_merge_errors")
    private Boolean hasMergeErrors;
    
    @Column(name = "has_data_quality_issues")
    private Boolean hasDataQualityIssues;
    
    @Column(name = "data_quality_assessment", length = 50)
    private String dataQualityAssessment;
    
    @Column(name = "rollback_reason", length = 2000)
    private String rollbackReason;
    
    @Column(name = "rolled_back_at")
    private LocalDateTime rolledBackAt;
    
    @Column(name = "rolled_back_by", length = 100)
    private String rolledBackBy;
    
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
