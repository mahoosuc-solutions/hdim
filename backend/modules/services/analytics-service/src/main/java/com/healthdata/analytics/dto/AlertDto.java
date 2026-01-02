package com.healthdata.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDto {

    private UUID id;

    @NotBlank(message = "Alert name is required")
    private String name;

    private String description;

    @NotBlank(message = "Metric type is required")
    private String metricType;

    private String metricName;

    @NotBlank(message = "Condition operator is required")
    private String conditionOperator; // GT, LT, EQ, GTE, LTE, CHANGE_PCT

    @NotNull(message = "Threshold value is required")
    private BigDecimal thresholdValue;

    private BigDecimal secondaryThreshold;

    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    private Map<String, Object> notificationChannels;

    private Map<String, Object> filters;

    private Integer cooldownMinutes;

    private Boolean isActive;

    private LocalDateTime lastTriggeredAt;

    private Integer triggerCount;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
