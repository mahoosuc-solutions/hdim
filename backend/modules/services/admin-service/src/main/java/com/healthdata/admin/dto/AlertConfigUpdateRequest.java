package com.healthdata.admin.dto;

import com.healthdata.admin.domain.AlertConfig.AlertSeverity;
import com.healthdata.admin.domain.AlertConfig.NotificationChannel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Alert Configuration Update Request DTO
 *
 * Used for updating existing alert configurations.
 * All fields are optional to allow partial updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertConfigUpdateRequest {

    @Positive(message = "Threshold must be greater than 0")
    private Double threshold;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 1440, message = "Duration must not exceed 1440 minutes (24 hours)")
    private Integer durationMinutes;

    private AlertSeverity severity;

    private Boolean enabled;

    @Size(min = 1, max = 4, message = "Must have between 1 and 4 notification channels")
    private List<NotificationChannel> notificationChannels;
}
