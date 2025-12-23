package com.healthdata.analytics.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Analytics Report Entity
 *
 * Stores generated analytics reports.
 */
@Entity
@Table(name = "analytics_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsReportEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "report_name", nullable = false, length = 255)
    private String reportName;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "report_data", nullable = false, columnDefinition = "TEXT")
    private String reportData;

    @Column(name = "summary_statistics", columnDefinition = "TEXT")
    private String summaryStatistics;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "generated_by", length = 128)
    private String generatedBy;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "COMPLETED";

    @Column(name = "file_url", length = 512)
    private String fileUrl;

    @Column(name = "file_format", length = 20)
    private String fileFormat;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (generatedAt == null) {
            generatedAt = Instant.now();
        }
    }
}
