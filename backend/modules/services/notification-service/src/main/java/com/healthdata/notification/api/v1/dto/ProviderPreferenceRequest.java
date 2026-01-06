package com.healthdata.notification.api.v1.dto;

import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating provider notification preferences.
 * Issue #148: Smart Notification Preferences
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderPreferenceRequest {

    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;

    /**
     * Whether this notification type is enabled.
     * Note: CRITICAL_RESULT cannot be disabled.
     */
    private Boolean enabled;

    /**
     * Primary delivery method: IN_APP, EMAIL, SMS, PUSH
     */
    private NotificationChannel deliveryMethod;

    /**
     * Additional delivery methods (comma-separated).
     * Example: "EMAIL,SMS"
     */
    private String additionalDeliveryMethods;

    /**
     * Minimum urgency level to trigger notification.
     * Values: LOW, MEDIUM, HIGH, CRITICAL
     */
    private String minUrgency;

    /**
     * Whether to batch notifications into digests.
     */
    private Boolean digestEnabled;

    /**
     * Digest frequency: DAILY, WEEKLY, IMMEDIATE
     */
    private String digestFrequency;
}
