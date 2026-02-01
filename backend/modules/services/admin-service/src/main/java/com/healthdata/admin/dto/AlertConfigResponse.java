package com.healthdata.admin.dto;

import com.healthdata.admin.domain.AlertConfig;
import com.healthdata.admin.domain.AlertConfig.AlertSeverity;
import com.healthdata.admin.domain.AlertConfig.AlertType;
import com.healthdata.admin.domain.AlertConfig.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Alert Configuration Response DTO
 *
 * Returned to clients when querying alert configurations.
 * Excludes tenant ID for security (implicitly filtered by tenant context).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertConfigResponse {

    private UUID id;
    private String serviceName;
    private String displayName;
    private AlertType alertType;
    private Double threshold;
    private Integer durationMinutes;
    private AlertSeverity severity;
    private Boolean enabled;
    private List<NotificationChannel> notificationChannels;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastTriggered;

    /**
     * Convert entity to response DTO
     *
     * @param entity AlertConfig entity
     * @return AlertConfigResponse DTO
     */
    public static AlertConfigResponse fromEntity(AlertConfig entity) {
        return AlertConfigResponse.builder()
                .id(entity.getId())
                .serviceName(entity.getServiceName())
                .displayName(entity.getDisplayName())
                .alertType(entity.getAlertType())
                .threshold(entity.getThreshold())
                .durationMinutes(entity.getDurationMinutes())
                .severity(entity.getSeverity())
                .enabled(entity.getEnabled())
                .notificationChannels(entity.getNotificationChannels())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .lastTriggered(entity.getLastTriggered())
                .build();
    }
}
