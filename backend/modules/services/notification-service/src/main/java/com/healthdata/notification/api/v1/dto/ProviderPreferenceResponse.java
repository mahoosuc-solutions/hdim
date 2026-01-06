package com.healthdata.notification.api.v1.dto;

import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationType;
import com.healthdata.notification.domain.model.ProviderNotificationPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for provider notification preferences.
 * Issue #148: Smart Notification Preferences
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderPreferenceResponse {

    private UUID id;
    private UUID providerId;
    private String tenantId;

    /**
     * Notification type
     */
    private NotificationType notificationType;

    /**
     * Human-readable name
     */
    private String displayName;

    /**
     * Description of what triggers this notification
     */
    private String description;

    /**
     * Whether this notification type is enabled
     */
    private Boolean enabled;

    /**
     * Whether this type can be disabled
     */
    private Boolean canDisable;

    /**
     * Primary delivery method
     */
    private NotificationChannel deliveryMethod;

    /**
     * Additional delivery methods
     */
    private String additionalDeliveryMethods;

    /**
     * Minimum urgency level
     */
    private String minUrgency;

    /**
     * Whether digest mode is enabled
     */
    private Boolean digestEnabled;

    /**
     * Digest frequency
     */
    private String digestFrequency;

    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Convert entity to response DTO
     */
    public static ProviderPreferenceResponse fromEntity(ProviderNotificationPreference entity) {
        if (entity == null) {
            return null;
        }

        NotificationType type = entity.getNotificationType();

        return ProviderPreferenceResponse.builder()
            .id(entity.getId())
            .providerId(entity.getProviderId())
            .tenantId(entity.getTenantId())
            .notificationType(type)
            .displayName(type != null ? type.getDisplayName() : null)
            .description(type != null ? type.getDescription() : null)
            .enabled(entity.getEnabled())
            .canDisable(type != null && !type.isAlwaysEnabled())
            .deliveryMethod(entity.getDeliveryMethod())
            .additionalDeliveryMethods(entity.getAdditionalDeliveryMethods())
            .minUrgency(entity.getMinUrgency())
            .digestEnabled(entity.getDigestEnabled())
            .digestFrequency(entity.getDigestFrequency())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
