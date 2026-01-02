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
 * Dashboard Widget Entity
 *
 * Represents an individual widget on a dashboard.
 * Widgets can be charts, KPI displays, tables, or maps.
 */
@Entity
@Table(name = "dashboard_widgets",
       indexes = {
           @Index(name = "idx_widget_dashboard", columnList = "dashboard_id"),
           @Index(name = "idx_widget_tenant", columnList = "tenant_id"),
           @Index(name = "idx_widget_type", columnList = "widget_type")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardWidgetEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "dashboard_id", nullable = false)
    private UUID dashboardId;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "widget_type", nullable = false, length = 50)
    private String widgetType; // CHART, KPI, TABLE, MAP, GAUGE, TREND

    @Column(name = "title", length = 255)
    private String title;

    @Type(JsonBinaryType.class)
    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> config;

    @Column(name = "data_source", length = 100)
    private String dataSource; // QUALITY_MEASURE, HCC, CARE_GAP, CUSTOM

    @Column(name = "refresh_interval_seconds")
    private Integer refreshIntervalSeconds;

    @Column(name = "position_x")
    private Integer positionX;

    @Column(name = "position_y")
    private Integer positionY;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
