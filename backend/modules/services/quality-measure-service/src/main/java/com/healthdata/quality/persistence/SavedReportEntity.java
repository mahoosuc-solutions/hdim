package com.healthdata.quality.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Saved Report Entity
 * Stores generated quality reports for future reference and export
 */
@Hidden
@Schema(hidden = true)
@Entity
@Table(name = "saved_reports", indexes = {
        @Index(name = "idx_saved_reports_tenant", columnList = "tenant_id"),
        @Index(name = "idx_saved_reports_type", columnList = "report_type"),
        @Index(name = "idx_saved_reports_created_at", columnList = "created_at"),
        @Index(name = "idx_saved_reports_patient", columnList = "patient_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedReportEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType; // PATIENT, POPULATION, CARE_GAP

    @Column(name = "report_name", nullable = false, length = 255)
    private String reportName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Filter parameters
    @Column(name = "patient_id")
    private UUID patientId; // For patient reports

    @Column(name = "`year`") // Escaped because YEAR is a reserved SQL keyword
    private Integer year; // For population reports

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // Report data stored as JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_data", columnDefinition = "JSONB", nullable = false)
    private String reportData;

    // Metadata
    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    // Status tracking
    @Column(name = "status", nullable = false, length = 50)
    private String status; // GENERATING, COMPLETED, FAILED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

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
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "COMPLETED";
        }
    }
}
