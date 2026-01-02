package com.healthdata.qrda.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity tracking CMS quality measure submissions.
 *
 * Records submission history for regulatory compliance and audit purposes.
 * Supports MIPS, APP, and ACO-level quality reporting programs.
 */
@Entity
@Table(name = "cms_submissions", schema = "quality")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsSubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "submission_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SubmissionType submissionType;

    @Column(name = "program_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ProgramType programType;

    @Column(name = "submission_year", nullable = false)
    private Integer submissionYear;

    @Column(name = "performance_period_start")
    private LocalDateTime performancePeriodStart;

    @Column(name = "performance_period_end")
    private LocalDateTime performancePeriodEnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qrda_job_id")
    private QrdaExportJobEntity qrdaJob;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;

    @Column(name = "submission_reference", length = 255)
    private String submissionReference;

    @Column(name = "cms_tracking_number", length = 100)
    private String cmsTrackingNumber;

    @Column(name = "submission_metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> submissionMetadata;

    @Column(name = "measure_results", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> measureResults;

    @Column(name = "submitted_by", length = 100)
    private String submittedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = SubmissionStatus.DRAFT;
        }
    }

    public enum SubmissionType {
        ECQM,           // Electronic Clinical Quality Measures
        CQM,            // Clinical Quality Measures
        MEDICARE_CQM,   // Medicare-specific CQMs
        QRDA_I,         // Patient-level QRDA
        QRDA_III        // Aggregate QRDA
    }

    public enum ProgramType {
        MIPS,           // Merit-based Incentive Payment System
        APP,            // ACOS Participating in APP Plus
        MSSP,           // Medicare Shared Savings Program
        ACO_REACH,      // ACO REACH Model
        CPC_PLUS,       // Comprehensive Primary Care Plus
        PCF             // Primary Care First
    }

    public enum SubmissionStatus {
        DRAFT,          // Not yet submitted
        PENDING,        // Awaiting submission
        SUBMITTED,      // Sent to CMS
        ACKNOWLEDGED,   // CMS acknowledged receipt
        ACCEPTED,       // CMS accepted submission
        REJECTED,       // CMS rejected submission
        RESUBMITTED,    // Corrected and resubmitted
        FINALIZED       // Final score received
    }
}
