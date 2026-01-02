package com.healthdata.qrda.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a QRDA export job.
 *
 * Tracks the status and output of QRDA Category I (patient-level)
 * and Category III (aggregate) document generation requests.
 */
@Entity
@Table(name = "qrda_export_jobs", schema = "quality")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrdaExportJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "job_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private QrdaJobType jobType;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private QrdaJobStatus status;

    @Column(name = "measure_ids", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> measureIds;

    @Column(name = "patient_ids", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<UUID> patientIds;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "document_location", length = 500)
    private String documentLocation;

    @Column(name = "document_count")
    private Integer documentCount;

    @Column(name = "patient_count")
    private Integer patientCount;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "validation_errors", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> validationErrors;

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = QrdaJobStatus.PENDING;
        }
    }

    public enum QrdaJobType {
        QRDA_I,       // Patient-level individual export
        QRDA_III      // Aggregate population export
    }

    public enum QrdaJobStatus {
        PENDING,      // Job created, not yet started
        RUNNING,      // Currently generating documents
        VALIDATING,   // Running Schematron validation
        COMPLETED,    // Successfully completed
        FAILED,       // Failed with errors
        CANCELLED     // Cancelled by user
    }
}
