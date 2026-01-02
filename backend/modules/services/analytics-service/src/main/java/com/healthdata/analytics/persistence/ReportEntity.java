package com.healthdata.analytics.persistence;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
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
 * Report Entity
 *
 * Represents a report definition including parameters, schedule, and output format.
 * Reports can be executed on-demand or scheduled.
 */
@Entity
@Table(name = "reports",
       indexes = {
           @Index(name = "idx_report_tenant", columnList = "tenant_id"),
           @Index(name = "idx_report_type", columnList = "report_type"),
           @Index(name = "idx_report_created_by", columnList = "created_by")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType; // QUALITY, HCC, CARE_GAP, POPULATION, CUSTOM

    @Type(JsonBinaryType.class)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private Map<String, Object> parameters;

    @Column(name = "schedule_cron", length = 100)
    private String scheduleCron;

    @Column(name = "schedule_enabled", nullable = false)
    @Builder.Default
    private Boolean scheduleEnabled = false;

    @Column(name = "output_format", length = 20)
    @Builder.Default
    private String outputFormat = "PDF"; // PDF, CSV, EXCEL, JSON

    @Type(JsonBinaryType.class)
    @Column(name = "recipients", columnDefinition = "jsonb")
    private Map<String, Object> recipients;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Integer version;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
