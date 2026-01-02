package com.healthdata.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WidgetDto {

    private UUID id;

    @NotNull(message = "Dashboard ID is required")
    private UUID dashboardId;

    @NotBlank(message = "Widget type is required")
    private String widgetType; // CHART, KPI, TABLE, MAP, GAUGE, TREND

    private String title;

    @NotNull(message = "Widget configuration is required")
    private Map<String, Object> config;

    private String dataSource; // QUALITY_MEASURE, HCC, CARE_GAP, CUSTOM

    private Integer refreshIntervalSeconds;

    private Integer positionX;

    private Integer positionY;

    private Integer width;

    private Integer height;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
