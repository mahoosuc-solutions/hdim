package com.healthdata.notification.api.v1.dto;

import com.healthdata.notification.domain.model.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Request to send a notification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendNotificationRequest {

    @NotBlank(message = "Recipient ID is required")
    private String recipientId;

    /**
     * Recipient email address (required for EMAIL channel).
     */
    private String recipientEmail;

    /**
     * Recipient phone number (required for SMS channel).
     */
    private String recipientPhone;

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    /**
     * Template ID to use for generating content.
     * If provided, templateVariables will be used to populate the template.
     */
    private UUID templateId;

    /**
     * Template name (alternative to templateId).
     */
    private String templateName;

    /**
     * Variables to substitute in the template.
     */
    private Map<String, Object> templateVariables;

    /**
     * Direct subject (used if no template is specified).
     */
    private String subject;

    /**
     * Direct body content (used if no template is specified).
     */
    private String body;

    /**
     * Priority (1-10, lower is higher priority).
     */
    @Builder.Default
    private Integer priority = 5;

    /**
     * Additional metadata to store with the notification.
     */
    private Map<String, Object> metadata;

    /**
     * Whether to respect user quiet hours.
     */
    @Builder.Default
    private Boolean respectQuietHours = true;

    /**
     * Whether to check user preferences before sending.
     */
    @Builder.Default
    private Boolean checkPreferences = true;
}
