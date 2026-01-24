package com.healthdata.admin.dto;

import com.healthdata.admin.domain.AlertConfig.AlertSeverity;
import com.healthdata.admin.domain.AlertConfig.AlertType;
import com.healthdata.admin.domain.AlertConfig.NotificationChannel;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Alert Configuration Request DTO
 *
 * Used for creating new alert configurations.
 * Includes validation constraints to ensure data integrity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertConfigRequest {

    @NotBlank(message = "Service name is required")
    @Size(max = 255, message = "Service name must not exceed 255 characters")
    private String serviceName;

    @NotBlank(message = "Display name is required")
    @Size(max = 255, message = "Display name must not exceed 255 characters")
    private String displayName;

    @NotNull(message = "Alert type is required")
    private AlertType alertType;

    @NotNull(message = "Threshold is required")
    @Positive(message = "Threshold must be greater than 0")
    private Double threshold;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 1440, message = "Duration must not exceed 1440 minutes (24 hours)")
    private Integer durationMinutes;

    @NotNull(message = "Severity is required")
    private AlertSeverity severity;

    @Builder.Default
    private Boolean enabled = true;

    @NotEmpty(message = "At least one notification channel is required")
    @Size(min = 1, max = 4, message = "Must have between 1 and 4 notification channels")
    private List<NotificationChannel> notificationChannels;
}
