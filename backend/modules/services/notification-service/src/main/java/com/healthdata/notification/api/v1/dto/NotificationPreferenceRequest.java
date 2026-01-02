package com.healthdata.notification.api.v1.dto;

import com.healthdata.notification.domain.model.NotificationChannel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Request to create or update notification preferences.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferenceRequest {

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    @NotNull(message = "Enabled flag is required")
    private Boolean enabled;

    /**
     * Start of quiet hours (e.g., "22:00").
     */
    private LocalTime quietHoursStart;

    /**
     * End of quiet hours (e.g., "07:00").
     */
    private LocalTime quietHoursEnd;

    /**
     * User's timezone (e.g., "America/New_York").
     */
    @Builder.Default
    private String timezone = "UTC";
}
