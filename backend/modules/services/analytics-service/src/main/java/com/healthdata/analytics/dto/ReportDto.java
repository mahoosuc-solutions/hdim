package com.healthdata.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDto {

    private UUID id;

    @NotBlank(message = "Report name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    @NotBlank(message = "Report type is required")
    private String reportType; // QUALITY, HCC, CARE_GAP, POPULATION, CUSTOM

    private Map<String, Object> parameters;

    private String scheduleCron;

    private Boolean scheduleEnabled;

    private String outputFormat; // PDF, CSV, EXCEL, JSON

    private Map<String, Object> recipients;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private ReportExecutionDto latestExecution;
}
