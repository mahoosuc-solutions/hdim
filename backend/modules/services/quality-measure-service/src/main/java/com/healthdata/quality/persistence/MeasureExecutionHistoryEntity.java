package com.healthdata.quality.persistence;


import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Measure Execution History Entity
 * Complete audit trail of every measure calculation for compliance and debugging.
 * Maps to the measure_execution_history table created in migration 0038.
 *
 * HIPAA Compliance: Tracks execution context, performance metrics, data sources,
 * overrides applied, and errors for regulatory audit requirements.
 */
@Hidden
@Schema(hidden = true)
@Entity
@Table(name = "measure_execution_history",
    indexes = {
        @Index(name = "idx_meh_patient_time", columnList = "patient_id, execution_timestamp DESC"),
        @Index(name = "idx_meh_measure_time", columnList = "measure_id, execution_timestamp DESC"),
        @Index(name = "idx_meh_status", columnList = "execution_status, execution_timestamp DESC"),
        @Index(name = "idx_meh_result", columnList = "result_id"),
        @Index(name = "idx_meh_tenant", columnList = "tenant_id"),
        @Index(name = "idx_meh_execution_mode", columnList = "execution_mode, execution_timestamp DESC")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasureExecutionHistoryEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    // Execution Context
    @Column(name = "measure_id", nullable = false)
    private UUID measureId;

    @Column(name = "measure_version", length = 50)
    private String measureVersion;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "execution_timestamp", nullable = false)
    private OffsetDateTime executionTimestamp;

    // Execution Details
    @Column(name = "execution_mode", nullable = false, length = 50)
    private String executionMode; // SCHEDULED, MANUAL, API, BULK, RECALCULATION, ADHOC

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_context", columnDefinition = "jsonb")
    private Map<String, Object> executionContext;

    // Performance
    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "duration_ms")
    private Long durationMs; // Auto-calculated by trigger

    // Result
    @Column(name = "result_id")
    private UUID resultId;

    @Column(name = "execution_status", nullable = false, length = 50)
    private String executionStatus; // SUCCESS, FAILURE, PARTIAL, SKIPPED, TIMEOUT

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_details", columnDefinition = "jsonb")
    private Map<String, Object> errorDetails;

    // Data Used
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_sources", columnDefinition = "jsonb")
    private Map<String, Object> dataSources;

    @Column(name = "data_period_start")
    private LocalDate dataPeriodStart;

    @Column(name = "data_period_end")
    private LocalDate dataPeriodEnd;

    // Overrides Applied
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "overrides_applied", columnDefinition = "jsonb")
    private Map<String, Object> overridesApplied;

    @Column(name = "profile_applied")
    private UUID profileApplied;

    // Audit
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (executionTimestamp == null) {
            executionTimestamp = OffsetDateTime.now();
        }
    }
}
