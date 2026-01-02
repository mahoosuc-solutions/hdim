package com.healthdata.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDto {

    private UUID id;

    @NotBlank(message = "Dashboard name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    private Map<String, Object> layout;

    private Boolean isDefault;

    private Boolean isShared;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<WidgetDto> widgets;
}
